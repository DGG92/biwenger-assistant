package com.artajerjes.biwengerassistant.recommendation;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.market.MarketListing;
import com.artajerjes.biwengerassistant.market.MarketListingRepository;
import com.artajerjes.biwengerassistant.market.MarketListingType;
import com.artajerjes.biwengerassistant.offer.OfferService;
import com.artajerjes.biwengerassistant.offer.dto.EconomicStatusResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

        private static final Long LEAGUE_ID = 1L;
        private static final Long BIWENGER_USER_ID = 11_467_137L;

        @Mock
        private LeagueRepository leagueRepository;

        @Mock
        private MarketListingRepository marketListingRepository;

        @Mock
        private OfferService offerService;

        @Mock
        private PlayerRepository playerRepository;

        @InjectMocks
        private RecommendationService recommendationService;

        @BeforeEach
        void setUp() {
                ReflectionTestUtils.setField(
                                recommendationService,
                                "biwengerUserId",
                                BIWENGER_USER_ID);
        }

        @Test
        void saleShouldReturnBuyForCheapRisingAffordablePlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                10L,
                                "100",
                                "Oportunidad",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                100_000L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                4_000_000L,
                                null,
                                league);

                mockCommon(
                                6_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals("Oportunidad", result.playerName());
                assertEquals(MarketListingType.SALE, result.marketType());
                assertEquals(5_000_000L, result.marketValue());
                assertEquals(4_000_000L, result.askingPrice());

                assertNull(result.currentBid());
                assertNull(result.maximumRecommendedBid());

                assertEquals(1_000_000L, result.priceDifference());
                assertEquals(20.0, result.priceDifferencePercentage());
                assertTrue(result.affordable());
                assertFalse(result.injured());

                assertEquals(90, result.score());
                assertEquals(
                                RecommendationType.STRONG_BUY,
                                result.recommendation());
        }

        @Test
        void saleShouldPenalizeInjuredPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                11L,
                                "101",
                                "Lesionado",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                0L,
                                true);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                5_000_000L,
                                null,
                                league);

                mockCommon(
                                10_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertTrue(result.injured());
                assertEquals(20, result.score());
                assertEquals(
                                RecommendationType.AVOID,
                                result.recommendation());
        }

        @Test
        void saleShouldCapUnaffordablePlayerAtTwentyFive() {
                League league = createLeague();

                Player player = createPlayer(
                                12L,
                                "102",
                                "Demasiado caro",
                                List.of(PlayerPosition.DL),
                                10_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                10_000_000L,
                                null,
                                league);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertFalse(result.affordable());
                assertEquals(25, result.score());
                assertEquals(
                                RecommendationType.AVOID,
                                result.recommendation());
        }

        @Test
        void saleShouldPenalizeOverpricedPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                13L,
                                "103",
                                "Sobreprecio",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                6_000_000L,
                                null,
                                league);

                mockCommon(
                                10_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(-1_000_000L, result.priceDifference());
                assertEquals(-20.0, result.priceDifferencePercentage());

                assertEquals(20, result.score());
                assertEquals(
                                RecommendationType.AVOID,
                                result.recommendation());
        }

        @Test
        void auctionWithoutBidShouldUseAskingPriceAsEffectivePrice() {
                League league = createLeague();

                Player player = createPlayer(
                                20L,
                                "200",
                                "Subasta sin puja",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.AUCTION,
                                player,
                                800_000L,
                                null,
                                league);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                MarketListingType.AUCTION,
                                result.marketType());

                assertEquals(800_000L, result.askingPrice());
                assertNull(result.currentBid());

                assertEquals(
                                1_000_000L,
                                result.maximumRecommendedBid());

                assertEquals(
                                200_000L,
                                result.priceDifference());

                assertEquals(
                                20.0,
                                result.priceDifferencePercentage());

                assertTrue(result.affordable());
                assertEquals(80, result.score());
                assertEquals(
                                RecommendationType.STRONG_BUY,
                                result.recommendation());
        }

        @Test
        void auctionWithBidShouldUseCurrentBidAsEffectivePrice() {
                League league = createLeague();

                Player player = createPlayer(
                                21L,
                                "201",
                                "Subasta con puja",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.AUCTION,
                                player,
                                800_000L,
                                900_000L,
                                league);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(900_000L, result.currentBid());
                assertEquals(
                                1_000_000L,
                                result.maximumRecommendedBid());

                assertEquals(
                                100_000L,
                                result.priceDifference());

                assertEquals(
                                10.0,
                                result.priceDifferencePercentage());

                assertTrue(result.affordable());
                assertEquals(80, result.score());

                assertEquals(
                                RecommendationType.STRONG_BUY,
                                result.recommendation());
        }

        @Test
        void auctionShouldIncreaseRecommendedBidForRisingPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                22L,
                                "202",
                                "Jugador subiendo",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                50_000L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.AUCTION,
                                player,
                                1_000_000L,
                                null,
                                league);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                1_100_000L,
                                result.maximumRecommendedBid());

                assertEquals(70, result.score());

                assertEquals(
                                RecommendationType.BUY,
                                result.recommendation());
        }

        @Test
        void auctionShouldReduceRecommendedBidForFallingPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                23L,
                                "203",
                                "Jugador bajando",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                -50_000L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.AUCTION,
                                player,
                                800_000L,
                                null,
                                league);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                900_000L,
                                result.maximumRecommendedBid());

                assertTrue(result.affordable());
        }

        @Test
        void auctionShouldReduceRecommendedBidForInjuredPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                24L,
                                "204",
                                "Subasta lesionado",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                true);

                MarketListing listing = createListing(
                                MarketListingType.AUCTION,
                                player,
                                800_000L,
                                null,
                                league);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                850_000L,
                                result.maximumRecommendedBid());
        }

        @Test
        void auctionShouldNeverRecommendMoreThanUserMaximumBid() {
                League league = createLeague();

                Player player = createPlayer(
                                25L,
                                "205",
                                "Límite económico",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                500_000L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.AUCTION,
                                player,
                                2_000_000L,
                                null,
                                league);

                mockCommon(
                                2_500_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                2_500_000L,
                                result.maximumRecommendedBid());
        }

        @Test
        void auctionShouldAvoidWhenCurrentBidExceedsRecommendedLimit() {
                League league = createLeague();

                Player player = createPlayer(
                                26L,
                                "206",
                                "Puja pasada",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.AUCTION,
                                player,
                                800_000L,
                                1_100_000L,
                                league);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                1_000_000L,
                                result.maximumRecommendedBid());

                assertFalse(result.affordable());
                assertEquals(20, result.score());

                assertEquals(
                                RecommendationType.AVOID,
                                result.recommendation());
        }

        @Test
        void getMarketRecommendationsShouldSortFromHighestToLowestScore() {
                League league = createLeague();

                Player good = createPlayer(
                                30L,
                                "300",
                                "Bueno",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                100_000L,
                                false);

                Player bad = createPlayer(
                                31L,
                                "301",
                                "Malo",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                0L,
                                true);

                MarketListing goodListing = createListing(
                                MarketListingType.SALE,
                                good,
                                4_500_000L,
                                null,
                                league);

                MarketListing badListing = createListing(
                                MarketListingType.SALE,
                                bad,
                                5_000_000L,
                                null,
                                league);

                mockCommon(
                                10_000_000L,
                                List.of(
                                                badListing,
                                                goodListing));

                List<MarketRecommendationResponse> result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID);

                assertEquals(2, result.size());
                assertEquals("Bueno", result.get(0).playerName());
                assertEquals("Malo", result.get(1).playerName());

                assertTrue(
                                result.get(0).score() > result.get(1).score());
        }

        @Test
        void getMarketRecommendationsShouldReturnEmptyListWhenMarketIsEmpty() {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(offerService.getEconomicStatus(LEAGUE_ID))
                                .thenReturn(
                                                new EconomicStatusResponse(
                                                                0L,
                                                                0L));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                when(
                                marketListingRepository
                                                .findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                List<MarketRecommendationResponse> result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID);

                assertEquals(0, result.size());
        }

        @Test
        void getMarketRecommendationsShouldThrowWhenLeagueDoesNotExist() {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(false);

                assertThrows(
                                LeagueNotFoundException.class,
                                () -> recommendationService
                                                .getMarketRecommendations(LEAGUE_ID));
        }

        @Test
        void squadNeedsShouldExcludeCoachFromTotalPlayers() {
                Manager manager = createManager();

                Player goalkeeper = createOwnedPlayer(
                                40L,
                                "400",
                                "Portero",
                                List.of(PlayerPosition.PT),
                                manager);

                Player defender = createOwnedPlayer(
                                41L,
                                "401",
                                "Defensa",
                                List.of(PlayerPosition.DF),
                                manager);

                Player coach = createOwnedPlayer(
                                42L,
                                "402",
                                "Entrenador",
                                List.of(PlayerPosition.E),
                                manager);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(
                                                goalkeeper,
                                                defender,
                                                coach));

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                assertEquals(2, result.totalPlayers());
                assertEquals(1, result.playersByPosition().get("PT"));
                assertEquals(1, result.playersByPosition().get("DF"));
                assertEquals(0, result.playersByPosition().get("MC"));
                assertEquals(0, result.playersByPosition().get("DL"));
        }

        @Test
        void squadNeedsShouldCountMultiPositionPlayerInEveryPosition() {
                Manager manager = createManager();

                Player versatile = createOwnedPlayer(
                                43L,
                                "403",
                                "Polivalente",
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC),
                                manager);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(versatile));

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                assertEquals(1, result.totalPlayers());
                assertEquals(1, result.playersByPosition().get("DF"));
                assertEquals(1, result.playersByPosition().get("MC"));
        }

        @Test
        void squadNeedsShouldIncreaseNeedForMissingAndInjuredPlayers() {
                Manager manager = createManager();

                Player injuredDefender = createOwnedPlayer(
                                44L,
                                "404",
                                "Defensa lesionado",
                                List.of(PlayerPosition.DF),
                                manager);

                ReflectionTestUtils.setField(
                                injuredDefender,
                                "injured",
                                true);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(injuredDefender));

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                assertEquals(
                                100,
                                result.needScoreByPosition().get("DF"));

                assertEquals(
                                1,
                                result.injuredByPosition().get("DF"));
        }

        @Test
        void marketRecommendationShouldReceiveBonusForNeededPosition() {
                League league = createLeague();
                Manager manager = createManager();

                Player goalkeeperOne = createOwnedPlayer(
                                50L,
                                "500",
                                "PT 1",
                                List.of(PlayerPosition.PT),
                                manager);

                Player goalkeeperTwo = createOwnedPlayer(
                                51L,
                                "501",
                                "PT 2",
                                List.of(PlayerPosition.PT),
                                manager);

                Player marketDefender = createPlayer(
                                52L,
                                "502",
                                "Defensa mercado",
                                List.of(PlayerPosition.DF),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                marketDefender,
                                1_000_000L,
                                null,
                                league);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(offerService.getEconomicStatus(LEAGUE_ID))
                                .thenReturn(
                                                new EconomicStatusResponse(
                                                                0L,
                                                                2_000_000L));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(
                                                goalkeeperOne,
                                                goalkeeperTwo));

                when(marketListingRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(70, result.score());

                assertEquals(
                                RecommendationType.BUY,
                                result.recommendation());
        }

        @Test
        void marketRecommendationShouldNotAddMultiPositionNeedsTogether() {
                League league = createLeague();
                Manager manager = createManager();

                Player goalkeeperOne = createOwnedPlayer(
                                60L,
                                "600",
                                "PT 1",
                                List.of(PlayerPosition.PT),
                                manager);

                Player goalkeeperTwo = createOwnedPlayer(
                                61L,
                                "601",
                                "PT 2",
                                List.of(PlayerPosition.PT),
                                manager);

                Player versatileMarketPlayer = createPlayer(
                                62L,
                                "602",
                                "MC DL mercado",
                                List.of(
                                                PlayerPosition.MC,
                                                PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                versatileMarketPlayer,
                                1_000_000L,
                                null,
                                league);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(offerService.getEconomicStatus(LEAGUE_ID))
                                .thenReturn(
                                                new EconomicStatusResponse(
                                                                0L,
                                                                2_000_000L));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(
                                                goalkeeperOne,
                                                goalkeeperTwo));

                when(marketListingRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(70, result.score());
        }

        private void mockCommon(
                        Long maximumBid,
                        List<MarketListing> listings) {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(offerService.getEconomicStatus(LEAGUE_ID))
                                .thenReturn(
                                                new EconomicStatusResponse(
                                                                0L,
                                                                maximumBid));

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(createCompleteSquad());

                when(
                                marketListingRepository
                                                .findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(listings);
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

        private Manager createManager() {
                Manager manager = new Manager(
                                BIWENGER_USER_ID,
                                "Califato Omeya",
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
                                13L);

                return manager;
        }

        private Player createOwnedPlayer(
                        Long id,
                        String biwengerPlayerId,
                        String name,
                        List<PlayerPosition> positions,
                        Manager owner) {
                Player player = createPlayer(
                                id,
                                biwengerPlayerId,
                                name,
                                positions,
                                1_000_000L,
                                0L,
                                false);

                ReflectionTestUtils.setField(
                                player,
                                "owner",
                                owner);

                return player;
        }

        private Player createPlayer(
                        Long id,
                        String biwengerPlayerId,
                        String name,
                        List<PlayerPosition> positions,
                        Long marketValue,
                        Long valueFluctuation,
                        boolean injured) {
                Player player = new Player(
                                biwengerPlayerId,
                                name,
                                positions,
                                "Equipo",
                                marketValue,
                                createLeague());

                ReflectionTestUtils.setField(
                                player,
                                "id",
                                id);

                ReflectionTestUtils.setField(
                                player,
                                "valueFluctuation",
                                valueFluctuation);

                ReflectionTestUtils.setField(
                                player,
                                "injured",
                                injured);

                return player;
        }

        private MarketListing createListing(
                        MarketListingType type,
                        Player player,
                        Long price,
                        Long lastBidAmount,
                        League league) {
                return new MarketListing(
                                type,
                                player,
                                null,
                                price,
                                null,
                                null,
                                false,
                                lastBidAmount,
                                lastBidAmount == null
                                                ? null
                                                : "waiting",
                                null,
                                league);
        }

        private List<Player> createCompleteSquad() {
                Manager manager = createManager();

                return List.of(
                                createOwnedPlayer(
                                                1001L,
                                                "1001",
                                                "PT 1",
                                                List.of(PlayerPosition.PT),
                                                manager),
                                createOwnedPlayer(
                                                1002L,
                                                "1002",
                                                "PT 2",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                1003L,
                                                "1003",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                1004L,
                                                "1004",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                1005L,
                                                "1005",
                                                "DF 3",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                1006L,
                                                "1006",
                                                "DF 4",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                1007L,
                                                "1007",
                                                "DF 5",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                1008L,
                                                "1008",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                1009L,
                                                "1009",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                1010L,
                                                "1010",
                                                "MC 3",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                1011L,
                                                "1011",
                                                "MC 4",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                1012L,
                                                "1012",
                                                "MC 5",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                1013L,
                                                "1013",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),
                                createOwnedPlayer(
                                                1014L,
                                                "1014",
                                                "DL 2",
                                                List.of(PlayerPosition.DL),
                                                manager),
                                createOwnedPlayer(
                                                1015L,
                                                "1015",
                                                "DL 3",
                                                List.of(PlayerPosition.DL),
                                                manager),
                                createOwnedPlayer(
                                                1016L,
                                                "1016",
                                                "DL 4",
                                                List.of(PlayerPosition.DL),
                                                manager));
        }
}