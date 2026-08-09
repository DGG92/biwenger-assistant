package com.artajerjes.biwengerassistant.market;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketData;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketLastBid;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketListing;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketPlayer;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketStatus;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketUser;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.market.dto.MarketListingResponse;
import com.artajerjes.biwengerassistant.market.dto.MarketSyncResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@ExtendWith(MockitoExtension.class)
class MarketServiceTest {

    private static final Long LEAGUE_ID = 1L;

    @Mock
    private MarketListingRepository marketListingRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private BiwengerClient biwengerClient;

    @InjectMocks
    private MarketService marketService;

    @Test
    void syncShouldCreateMachineSale() {
        League league = createLeague();

        Player player = createPlayer(
                10L,
                "91",
                "Jugador máquina");

        BiwengerMarketListing externalSale = new BiwengerMarketListing(
                1786165440L,
                1786338000L,
                true,
                2_000_000L,
                new BiwengerMarketPlayer(
                        91L,
                        null),
                null,
                null);

        BiwengerMarketResponse response = createMarketResponse(
                List.of(externalSale),
                List.of());

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of());

        when(biwengerClient.getMarket())
                .thenReturn(response);

        MarketSyncResponse result = marketService.sync(LEAGUE_ID);

        assertEquals(1, result.sales());
        assertEquals(0, result.auctions());
        assertEquals(0, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        ArgumentCaptor<MarketListing> captor = ArgumentCaptor.forClass(MarketListing.class);

        verify(marketListingRepository)
                .save(captor.capture());

        MarketListing saved = captor.getValue();

        assertEquals(
                MarketListingType.SALE,
                saved.getType());

        assertEquals(player, saved.getPlayer());

        /*
         * user == null en Biwenger:
         * jugador puesto a la venta por la máquina.
         */
        assertNull(saved.getSeller());

        assertEquals(
                2_000_000L,
                saved.getPrice());

        assertEquals(
                toLocalDateTime(1786165440L),
                saved.getPublishedAt());

        assertEquals(
                toLocalDateTime(1786338000L),
                saved.getExpiresAt());

        assertEquals(true, saved.isExtended());

        assertNull(saved.getLastBidAmount());
        assertNull(saved.getLastBidStatus());
        assertNull(saved.getLastBidManager());

        verify(marketListingRepository)
                .deleteAllByLeague_Id(LEAGUE_ID);
    }

    @Test
    void syncShouldCreateManagerSale() {
        League league = createLeague();

        Player player = createPlayer(
                11L,
                "24576",
                "Sørloth");

        Manager seller = createManager(
                5L,
                6_743_399L,
                "FOIX FC");

        BiwengerMarketListing externalSale = new BiwengerMarketListing(
                1786202955L,
                1786375755L,
                null,
                5_690_000L,
                new BiwengerMarketPlayer(
                        24576L,
                        null),
                new BiwengerMarketUser(
                        6_743_399L,
                        "FOIX FC",
                        "i/u/6743399.png"),
                null);

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(seller));

        when(biwengerClient.getMarket())
                .thenReturn(
                        createMarketResponse(
                                List.of(externalSale),
                                List.of()));

        MarketSyncResponse result = marketService.sync(LEAGUE_ID);

        assertEquals(1, result.sales());
        assertEquals(0, result.auctions());
        assertEquals(0, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        ArgumentCaptor<MarketListing> captor = ArgumentCaptor.forClass(MarketListing.class);

        verify(marketListingRepository)
                .save(captor.capture());

        MarketListing saved = captor.getValue();

        assertEquals(
                MarketListingType.SALE,
                saved.getType());

        assertEquals(player, saved.getPlayer());
        assertEquals(seller, saved.getSeller());

        assertEquals(
                5_690_000L,
                saved.getPrice());

        assertEquals(false, saved.isExtended());
    }

    @Test
    void syncShouldCreateAuctionWithoutBid() {
        League league = createLeague();

        Player player = createPlayer(
                20L,
                "12734",
                "Jugador subasta");

        BiwengerMarketListing externalAuction = new BiwengerMarketListing(
                1786238345L,
                1786324745L,
                null,
                160_000L,
                new BiwengerMarketPlayer(
                        12734L,
                        null),
                null,
                null);

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of());

        when(biwengerClient.getMarket())
                .thenReturn(
                        createMarketResponse(
                                List.of(),
                                List.of(externalAuction)));

        MarketSyncResponse result = marketService.sync(LEAGUE_ID);

        assertEquals(0, result.sales());
        assertEquals(1, result.auctions());
        assertEquals(0, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        ArgumentCaptor<MarketListing> captor = ArgumentCaptor.forClass(MarketListing.class);

        verify(marketListingRepository)
                .save(captor.capture());

        MarketListing saved = captor.getValue();

        assertEquals(
                MarketListingType.AUCTION,
                saved.getType());

        assertEquals(player, saved.getPlayer());

        assertNull(saved.getSeller());
        assertNull(saved.getLastBidAmount());
        assertNull(saved.getLastBidStatus());
        assertNull(saved.getLastBidManager());
    }

    @Test
    void syncShouldCreateAuctionWithLastBid() {
        League league = createLeague();

        Player player = createPlayer(
                21L,
                "20102",
                "Jugador con puja");

        Manager bidder = createManager(
                8L,
                13_963_608L,
                "Rahulk");

        BiwengerMarketLastBid lastBid = new BiwengerMarketLastBid(
                "bid",
                4_860_000L,
                "waiting",
                new BiwengerMarketUser(
                        13_963_608L,
                        "Rahulk",
                        "i/t/19.png"),
                null);

        BiwengerMarketListing externalAuction = new BiwengerMarketListing(
                1786239009L,
                1786325409L,
                null,
                5_248_800L,
                new BiwengerMarketPlayer(
                        20102L,
                        null),
                null,
                lastBid);

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(bidder));

        when(biwengerClient.getMarket())
                .thenReturn(
                        createMarketResponse(
                                List.of(),
                                List.of(externalAuction)));

        MarketSyncResponse result = marketService.sync(LEAGUE_ID);

        assertEquals(0, result.sales());
        assertEquals(1, result.auctions());
        assertEquals(0, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        ArgumentCaptor<MarketListing> captor = ArgumentCaptor.forClass(MarketListing.class);

        verify(marketListingRepository)
                .save(captor.capture());

        MarketListing saved = captor.getValue();

        assertEquals(
                4_860_000L,
                saved.getLastBidAmount());

        assertEquals(
                "waiting",
                saved.getLastBidStatus());

        assertEquals(
                bidder,
                saved.getLastBidManager());
    }

    @Test
    void syncShouldCountPlayerNotFoundAndSkipListing() {
        League league = createLeague();

        BiwengerMarketListing externalSale = new BiwengerMarketListing(
                1786165440L,
                1786338000L,
                null,
                1_000_000L,
                new BiwengerMarketPlayer(
                        999999L,
                        null),
                null,
                null);

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of());

        when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of());

        when(biwengerClient.getMarket())
                .thenReturn(
                        createMarketResponse(
                                List.of(externalSale),
                                List.of()));

        MarketSyncResponse result = marketService.sync(LEAGUE_ID);

        assertEquals(0, result.sales());
        assertEquals(0, result.auctions());
        assertEquals(1, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        verify(
                marketListingRepository,
                never()).save(any(MarketListing.class));
    }

    @Test
    void syncShouldCountSellerNotFoundButStillSaveListing() {
        League league = createLeague();

        Player player = createPlayer(
                30L,
                "24576",
                "Sørloth");

        BiwengerMarketListing externalSale = new BiwengerMarketListing(
                1786202955L,
                1786375755L,
                null,
                5_690_000L,
                new BiwengerMarketPlayer(
                        24576L,
                        null),
                new BiwengerMarketUser(
                        99_999_999L,
                        "Manager desconocido",
                        null),
                null);

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of());

        when(biwengerClient.getMarket())
                .thenReturn(
                        createMarketResponse(
                                List.of(externalSale),
                                List.of()));

        MarketSyncResponse result = marketService.sync(LEAGUE_ID);

        assertEquals(1, result.sales());
        assertEquals(0, result.auctions());
        assertEquals(0, result.playersNotFound());
        assertEquals(1, result.managersNotFound());

        ArgumentCaptor<MarketListing> captor = ArgumentCaptor.forClass(MarketListing.class);

        verify(marketListingRepository)
                .save(captor.capture());

        assertNull(
                captor.getValue().getSeller());
    }

    @Test
    void syncShouldCountLastBidManagerNotFoundButStillSaveAuction() {
        League league = createLeague();

        Player player = createPlayer(
                31L,
                "20102",
                "Jugador subasta");

        BiwengerMarketLastBid lastBid = new BiwengerMarketLastBid(
                "bid",
                4_860_000L,
                "waiting",
                new BiwengerMarketUser(
                        99_999_999L,
                        "Manager desconocido",
                        null),
                null);

        BiwengerMarketListing externalAuction = new BiwengerMarketListing(
                1786239009L,
                1786325409L,
                null,
                5_248_800L,
                new BiwengerMarketPlayer(
                        20102L,
                        null),
                null,
                lastBid);

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of());

        when(biwengerClient.getMarket())
                .thenReturn(
                        createMarketResponse(
                                List.of(),
                                List.of(externalAuction)));

        MarketSyncResponse result = marketService.sync(LEAGUE_ID);

        assertEquals(0, result.sales());
        assertEquals(1, result.auctions());
        assertEquals(0, result.playersNotFound());
        assertEquals(1, result.managersNotFound());

        ArgumentCaptor<MarketListing> captor = ArgumentCaptor.forClass(MarketListing.class);

        verify(marketListingRepository)
                .save(captor.capture());

        MarketListing saved = captor.getValue();

        assertEquals(
                4_860_000L,
                saved.getLastBidAmount());

        assertEquals(
                "waiting",
                saved.getLastBidStatus());

        assertNull(
                saved.getLastBidManager());
    }

    @Test
    void syncShouldThrowWhenLeagueDoesNotExist() {
        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                LeagueNotFoundException.class,
                () -> marketService.sync(LEAGUE_ID));

        verify(biwengerClient, never())
                .getMarket();

        verify(
                marketListingRepository,
                never()).deleteAllByLeague_Id(any(Long.class));
    }

    @Test
    void syncShouldThrowWhenBiwengerResponseIsInvalid() {
        League league = createLeague();

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(biwengerClient.getMarket())
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> marketService.sync(LEAGUE_ID));

        verify(
                marketListingRepository,
                never()).deleteAllByLeague_Id(any(Long.class));
    }

    @Test
    void findAllShouldReturnMarketListingResponses() {
        League league = createLeague();

        Player player = createPlayer(
                40L,
                "24576",
                "Sørloth");

        Manager seller = createManager(
                5L,
                6_743_399L,
                "FOIX FC");

        Manager bidder = createManager(
                8L,
                13_963_608L,
                "Rahulk");

        MarketListing listing = new MarketListing(
                MarketListingType.AUCTION,
                player,
                seller,
                5_690_000L,
                LocalDateTime.of(
                        2026,
                        8,
                        8,
                        12,
                        0),
                LocalDateTime.of(
                        2026,
                        8,
                        10,
                        12,
                        0),
                false,
                5_500_000L,
                "waiting",
                bidder,
                league);

        ReflectionTestUtils.setField(
                listing,
                "id",
                100L);

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                marketListingRepository.findAllByLeague_Id(
                        LEAGUE_ID))
                .thenReturn(List.of(listing));

        List<MarketListingResponse> result = marketService.findAll(LEAGUE_ID);

        assertEquals(1, result.size());

        MarketListingResponse response = result.get(0);

        assertEquals(
                100L,
                response.id());

        assertEquals(
                MarketListingType.AUCTION,
                response.type());

        assertEquals(
                40L,
                response.playerId());

        assertEquals(
                "24576",
                response.biwengerPlayerId());

        assertEquals(
                "Sørloth",
                response.playerName());

        assertEquals(
                "Atlético",
                response.teamName());

        assertEquals(
                5_620_000L,
                response.marketValue());

        assertEquals(
                5_690_000L,
                response.askingPrice());

        assertEquals(
                5L,
                response.sellerId());

        assertEquals(
                "FOIX FC",
                response.sellerName());

        assertEquals(
                5_500_000L,
                response.lastBidAmount());

        assertEquals(
                "waiting",
                response.lastBidStatus());

        assertEquals(
                8L,
                response.lastBidManagerId());

        assertEquals(
                "Rahulk",
                response.lastBidManagerName());
    }

    @Test
    void findAllShouldReturnNullSellerForMachineListing() {
        League league = createLeague();

        Player player = createPlayer(
                41L,
                "91",
                "Jugador máquina");

        MarketListing listing = new MarketListing(
                MarketListingType.SALE,
                player,
                null,
                2_000_000L,
                LocalDateTime.of(
                        2026,
                        8,
                        8,
                        12,
                        0),
                LocalDateTime.of(
                        2026,
                        8,
                        10,
                        12,
                        0),
                true,
                null,
                null,
                null,
                league);

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                marketListingRepository.findAllByLeague_Id(
                        LEAGUE_ID))
                .thenReturn(List.of(listing));

        MarketListingResponse response = marketService
                .findAll(LEAGUE_ID)
                .get(0);

        assertNull(response.sellerId());
        assertNull(response.sellerName());
        assertNull(response.lastBidAmount());
        assertNull(response.lastBidManagerId());
        assertNull(response.lastBidManagerName());
    }

    @Test
    void findAllShouldThrowWhenLeagueDoesNotExist() {
        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(false);

        assertThrows(
                LeagueNotFoundException.class,
                () -> marketService.findAll(LEAGUE_ID));

        verify(
                marketListingRepository,
                never()).findAllByLeague_Id(LEAGUE_ID);
    }

    private BiwengerMarketResponse createMarketResponse(
            List<BiwengerMarketListing> sales,
            List<BiwengerMarketListing> auctions) {
        BiwengerMarketData data = new BiwengerMarketData(
                new BiwengerMarketStatus(
                        -5_518_500L,
                        6_331_500L),
                sales,
                List.of(),
                auctions);

        return new BiwengerMarketResponse(
                200,
                data);
    }

    private League createLeague() {
        League league = new League(
                "VII Güenguer",
                "1268640");

        ReflectionTestUtils.setField(
                league,
                "id",
                LEAGUE_ID);

        return league;
    }

    private Player createPlayer(
            Long id,
            String biwengerPlayerId,
            String name) {
        Player player = new Player(
                biwengerPlayerId,
                name,
                List.of(PlayerPosition.DL),
                "Atlético",
                5_620_000L,
                createLeague());

        ReflectionTestUtils.setField(
                player,
                "id",
                id);

        return player;
    }

    private Manager createManager(
            Long id,
            Long biwengerManagerId,
            String name) {
        Manager manager = new Manager(
                biwengerManagerId,
                name,
                null,
                0,
                15,
                50_000_000L,
                100_000L,
                1,
                "manager",
                createLeague());

        ReflectionTestUtils.setField(
                manager,
                "id",
                id);

        return manager;
    }

    private LocalDateTime toLocalDateTime(
            Long timestamp) {
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault());
    }
}