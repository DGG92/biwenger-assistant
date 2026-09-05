package com.artajerjes.biwengerassistant.offer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.auth.CurrentAssistantUserService;
import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketData;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketOffer;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketStatus;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketUser;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.offer.dto.EconomicStatusResponse;
import com.artajerjes.biwengerassistant.offer.dto.OfferPlayerResponse;
import com.artajerjes.biwengerassistant.offer.dto.OfferResponse;
import com.artajerjes.biwengerassistant.offer.dto.OfferSyncResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@ExtendWith(MockitoExtension.class)
class OfferServiceTest {

        private static final Long LEAGUE_ID = 1L;
        private static final Long BIWENGER_USER_ID = 11_467_137L;

        @Mock
        private OfferRepository offerRepository;

        @Mock
        private LeagueRepository leagueRepository;

        @Mock
        private PlayerRepository playerRepository;

        @Mock
        private ManagerRepository managerRepository;

        @Mock
        private BiwengerClient biwengerClient;

        @Mock
        private CurrentAssistantUserService currentAssistantUserService;

        @InjectMocks
        private OfferService offerService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(
                                offerService,
                                "biwengerUserId",
                                BIWENGER_USER_ID);
        }

        @Test
        void syncShouldCreateNewOffer() {
                League league = createLeague();
                Player player = createPlayer(
                                10L,
                                "32435",
                                "Jugador objetivo");
                Manager fromManager = createManager(
                                20L,
                                BIWENGER_USER_ID,
                                "Califato Omeya");

                BiwengerMarketOffer externalOffer = new BiwengerMarketOffer(
                                263_849_180L,
                                3_880_000L,
                                1_786_384_909L,
                                1_786_424_400L,
                                "waiting",
                                "purchase",
                                new BiwengerMarketUser(
                                                BIWENGER_USER_ID,
                                                "Califato Omeya",
                                                null),
                                null,
                                List.of(32_435L));

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(managerRepository
                                .findByBiwengerManagerIdAndLeague_Id(
                                                BIWENGER_USER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(fromManager));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(player));

                when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(fromManager));

                when(biwengerClient.getMarket())
                                .thenReturn(
                                                createMarketResponse(
                                                                List.of(externalOffer)));

                when(offerRepository
                                .findByBiwengerOfferId(263_849_180L))
                                .thenReturn(Optional.empty());

                when(offerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                OfferSyncResponse result = offerService.sync(LEAGUE_ID);

                assertEquals(1, result.total());
                assertEquals(1, result.created());
                assertEquals(0, result.updated());
                assertEquals(0, result.playersNotFound());
                assertEquals(0, result.managersNotFound());

                assertEquals(
                                -5_518_500L,
                                fromManager.getCash());

                assertEquals(
                                2_541_500L,
                                fromManager.getMaximumBid());

                ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);

                verify(offerRepository)
                                .save(captor.capture());

                Offer saved = captor.getValue();

                assertEquals(
                                263_849_180L,
                                saved.getBiwengerOfferId());
                assertEquals(
                                3_880_000L,
                                saved.getAmount());
                assertEquals(
                                "waiting",
                                saved.getStatus());
                assertEquals(
                                "purchase",
                                saved.getType());
                assertEquals(
                                fromManager,
                                saved.getFromManager());
                assertNull(saved.getToManager());
                assertEquals(
                                1,
                                saved.getRequestedPlayers().size());
                assertEquals(
                                player,
                                saved.getRequestedPlayers().get(0));
        }

        @Test
        void syncShouldUpdateExistingOffer() {
                League league = createLeague();

                Player player = createPlayer(
                                11L,
                                "32435",
                                "Jugador objetivo");

                Manager fromManager = createManager(
                                21L,
                                BIWENGER_USER_ID,
                                "Califato Omeya");

                Offer existingOffer = new Offer(
                                263_849_180L,
                                3_500_000L,
                                "waiting",
                                "purchase",
                                fromManager,
                                null,
                                LocalDateTime.of(
                                                2026,
                                                8,
                                                10,
                                                10,
                                                0),
                                LocalDateTime.of(
                                                2026,
                                                8,
                                                10,
                                                20,
                                                0),
                                List.of(player),
                                league);

                BiwengerMarketOffer externalOffer = new BiwengerMarketOffer(
                                263_849_180L,
                                3_880_000L,
                                1_786_384_909L,
                                1_786_424_400L,
                                "waiting",
                                "purchase",
                                new BiwengerMarketUser(
                                                BIWENGER_USER_ID,
                                                "Califato Omeya",
                                                null),
                                null,
                                List.of(32_435L));

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(managerRepository
                                .findByBiwengerManagerIdAndLeague_Id(
                                                BIWENGER_USER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(fromManager));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(player));

                when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(fromManager));

                when(biwengerClient.getMarket())
                                .thenReturn(
                                                createMarketResponse(
                                                                List.of(externalOffer)));

                when(offerRepository
                                .findByBiwengerOfferId(263_849_180L))
                                .thenReturn(Optional.of(existingOffer));

                when(offerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(existingOffer));

                OfferSyncResponse result = offerService.sync(LEAGUE_ID);

                assertEquals(1, result.total());
                assertEquals(0, result.created());
                assertEquals(1, result.updated());

                assertEquals(
                                3_880_000L,
                                existingOffer.getAmount());

                assertEquals(
                                "waiting",
                                existingOffer.getStatus());

                assertEquals(
                                fromManager,
                                existingOffer.getFromManager());

                assertNull(existingOffer.getToManager());

                assertEquals(
                                player,
                                existingOffer
                                                .getRequestedPlayers()
                                                .get(0));

                assertEquals(
                                -5_518_500L,
                                fromManager.getCash());

                assertEquals(
                                2_541_500L,
                                fromManager.getMaximumBid());

                verify(
                                offerRepository,
                                never()).save(existingOffer);
        }

        @Test
        void syncShouldDeleteOfferNoLongerReturnedByBiwenger() {
                League league = createLeague();

                Manager authenticatedManager = createManager(
                                20L,
                                BIWENGER_USER_ID,
                                "Califato Omeya");

                Offer obsoleteOffer = new Offer(
                                999L,
                                1_000_000L,
                                "waiting",
                                "purchase",
                                null,
                                null,
                                LocalDateTime.of(
                                                2026,
                                                8,
                                                9,
                                                10,
                                                0),
                                LocalDateTime.of(
                                                2026,
                                                8,
                                                10,
                                                10,
                                                0),
                                List.of(),
                                league);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(managerRepository
                                .findByBiwengerManagerIdAndLeague_Id(
                                                BIWENGER_USER_ID,
                                                LEAGUE_ID))
                                .thenReturn(
                                                Optional.of(authenticatedManager));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(
                                                List.of(authenticatedManager));

                when(biwengerClient.getMarket())
                                .thenReturn(
                                                createMarketResponse(List.of()));

                when(offerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(obsoleteOffer));

                OfferSyncResponse result = offerService.sync(LEAGUE_ID);

                assertEquals(0, result.total());
                assertEquals(0, result.created());
                assertEquals(0, result.updated());

                assertEquals(
                                -5_518_500L,
                                authenticatedManager.getCash());

                assertEquals(
                                2_541_500L,
                                authenticatedManager.getMaximumBid());

                verify(offerRepository)
                                .delete(obsoleteOffer);
        }

        @Test
        void syncShouldCountPlayerNotFound() {
                League league = createLeague();

                Manager authenticatedManager = createManager(
                                20L,
                                BIWENGER_USER_ID,
                                "Califato Omeya");

                BiwengerMarketOffer externalOffer = new BiwengerMarketOffer(
                                1L,
                                500_000L,
                                1_786_384_909L,
                                1_786_424_400L,
                                "waiting",
                                "purchase",
                                null,
                                null,
                                List.of(999_999L));

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(managerRepository
                                .findByBiwengerManagerIdAndLeague_Id(
                                                BIWENGER_USER_ID,
                                                LEAGUE_ID))
                                .thenReturn(
                                                Optional.of(authenticatedManager));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(
                                                List.of(authenticatedManager));

                when(biwengerClient.getMarket())
                                .thenReturn(
                                                createMarketResponse(
                                                                List.of(externalOffer)));

                when(offerRepository
                                .findByBiwengerOfferId(1L))
                                .thenReturn(Optional.empty());

                when(offerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                OfferSyncResponse result = offerService.sync(LEAGUE_ID);

                assertEquals(1, result.total());
                assertEquals(1, result.created());
                assertEquals(
                                1,
                                result.playersNotFound());

                ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);

                verify(offerRepository)
                                .save(captor.capture());

                assertEquals(
                                0,
                                captor.getValue()
                                                .getRequestedPlayers()
                                                .size());
        }

        @Test
        void syncShouldCountManagerNotFound() {
                League league = createLeague();

                Manager authenticatedManager = createManager(
                                20L,
                                BIWENGER_USER_ID,
                                "Califato Omeya");

                BiwengerMarketOffer externalOffer = new BiwengerMarketOffer(
                                2L,
                                1_000_000L,
                                1_786_384_909L,
                                1_786_424_400L,
                                "waiting",
                                "purchase",
                                new BiwengerMarketUser(
                                                99_999_999L,
                                                "Manager desconocido",
                                                null),
                                null,
                                List.of());

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(managerRepository
                                .findByBiwengerManagerIdAndLeague_Id(
                                                BIWENGER_USER_ID,
                                                LEAGUE_ID))
                                .thenReturn(
                                                Optional.of(authenticatedManager));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(
                                                List.of(authenticatedManager));

                when(biwengerClient.getMarket())
                                .thenReturn(
                                                createMarketResponse(
                                                                List.of(externalOffer)));

                when(offerRepository
                                .findByBiwengerOfferId(2L))
                                .thenReturn(Optional.empty());

                when(offerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                OfferSyncResponse result = offerService.sync(LEAGUE_ID);

                assertEquals(1, result.created());
                assertEquals(
                                1,
                                result.managersNotFound());

                ArgumentCaptor<Offer> captor = ArgumentCaptor.forClass(Offer.class);

                verify(offerRepository)
                                .save(captor.capture());

                assertNull(
                                captor.getValue()
                                                .getFromManager());
        }

        @Test
        void syncShouldThrowWhenLeagueDoesNotExist() {
                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                LeagueNotFoundException.class,
                                () -> offerService.sync(LEAGUE_ID));

                verify(
                                biwengerClient,
                                never()).getMarket();
        }

        @Test
        void syncShouldThrowWhenMarketResponseIsInvalid() {
                League league = createLeague();

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(biwengerClient.getMarket())
                                .thenReturn(null);

                assertThrows(
                                IllegalStateException.class,
                                () -> offerService.sync(LEAGUE_ID));

                verify(
                                offerRepository,
                                never()).findAllByLeague_Id(LEAGUE_ID);
        }

        @Test
        void findAllShouldReturnOfferResponses() {
                League league = createLeague();

                Manager fromManager = createManager(
                                22L,
                                BIWENGER_USER_ID,
                                "Califato Omeya");
                Player player = createPlayer(
                                12L,
                                "32435",
                                "Jugador objetivo");

                player.updateOwnership(
                                fromManager,
                                LocalDateTime.of(
                                                2026,
                                                8,
                                                1,
                                                12,
                                                0),
                                750_000L,
                                null,
                                null);

                Offer offer = new Offer(
                                263_849_180L,
                                3_880_000L,
                                "waiting",
                                "purchase",
                                fromManager,
                                null,
                                LocalDateTime.of(
                                                2026,
                                                8,
                                                10,
                                                12,
                                                0),
                                LocalDateTime.of(
                                                2026,
                                                8,
                                                10,
                                                23,
                                                0),
                                List.of(player),
                                league);

                ReflectionTestUtils.setField(
                                offer,
                                "id",
                                100L);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(offerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(offer));

                List<OfferResponse> result = offerService.findAll(LEAGUE_ID);

                assertEquals(1, result.size());

                OfferResponse response = result.get(0);

                assertEquals(100L, response.id());
                assertEquals(
                                263_849_180L,
                                response.biwengerOfferId());
                assertEquals(
                                3_880_000L,
                                response.amount());
                assertEquals(
                                "waiting",
                                response.status());
                assertEquals(
                                "purchase",
                                response.type());
                assertEquals(
                                22L,
                                response.fromManagerId());
                assertEquals(
                                "Califato Omeya",
                                response.fromManagerName());
                assertNull(response.toManagerId());
                assertNull(response.toManagerName());

                assertEquals(
                                1,
                                response.requestedPlayers().size());

                OfferPlayerResponse requestedPlayer = response.requestedPlayers().get(0);

                assertEquals(
                                12L,
                                requestedPlayer.id());

                assertEquals(
                                "Jugador objetivo",
                                requestedPlayer.name());

                assertEquals(
                                1_000_000L,
                                requestedPlayer.marketValue());

                assertEquals(
                                750_000L,
                                requestedPlayer.purchasePrice());
        }

        @Test
        void findAllShouldReturnEmptyListWhenThereAreNoOffers() {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(offerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                List<OfferResponse> result = offerService.findAll(LEAGUE_ID);

                assertEquals(0, result.size());
        }

        @Test
        void findAllShouldThrowWhenLeagueDoesNotExist() {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(false);

                assertThrows(
                                LeagueNotFoundException.class,
                                () -> offerService.findAll(LEAGUE_ID));

                verify(
                                offerRepository,
                                never()).findAllByLeague_Id(LEAGUE_ID);
        }

        @Test
        void getEconomicStatusShouldReturnBalanceAndMaximumBid() {
                Manager manager = createManager(
                                20L,
                                BIWENGER_USER_ID,
                                "Califato Omeya");

                manager.updateEconomicStatus(
                                -5_518_500L,
                                2_541_500L);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(currentAssistantUserService.getCurrentManager())
                                .thenReturn(manager);

                EconomicStatusResponse result = offerService.getEconomicStatus(LEAGUE_ID);

                assertEquals(
                                -5_518_500L,
                                result.balance());

                assertEquals(
                                2_541_500L,
                                result.maximumBid());

                verify(
                                biwengerClient,
                                never()).getMarket();
        }

        @Test
        void getEconomicStatusShouldThrowWhenAuthenticatedUserHasNoManager() {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(currentAssistantUserService.getCurrentManager())
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Authenticated Assistant user has no manager assigned"));

                assertThrows(
                                IllegalStateException.class,
                                () -> offerService.getEconomicStatus(
                                                LEAGUE_ID));

                verify(
                                biwengerClient,
                                never()).getMarket();
        }

        private BiwengerMarketResponse createMarketResponse(
                        List<BiwengerMarketOffer> offers) {
                BiwengerMarketData data = new BiwengerMarketData(
                                new BiwengerMarketStatus(
                                                -5_518_500L,
                                                2_541_500L),
                                List.of(),
                                offers,
                                List.of());

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
                                "Equipo",
                                1_000_000L,
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
}