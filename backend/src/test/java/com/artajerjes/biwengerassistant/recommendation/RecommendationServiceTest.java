package com.artajerjes.biwengerassistant.recommendation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
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
import com.artajerjes.biwengerassistant.matchday.MatchdayChangeEligibilityService;
import com.artajerjes.biwengerassistant.matchday.MatchdayDifficultyService;
import com.artajerjes.biwengerassistant.matchday.MatchdayVenue;
import com.artajerjes.biwengerassistant.matchday.OpponentDifficulty;
import com.artajerjes.biwengerassistant.offer.OfferService;
import com.artajerjes.biwengerassistant.offer.dto.EconomicStatusResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;
import com.artajerjes.biwengerassistant.recommendation.dto.FormationRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationReason;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignalService;

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

        @Mock
        private PlayerMatchReportRepository playerMatchReportRepository;

        @Mock
        private MatchdayDifficultyService matchdayDifficultyService;

        @Mock
        private MatchdayChangeEligibilityService matchdayChangeEligibilityService;

        private RecommendationService recommendationService;

        @BeforeEach
        void setUp() {

                lenient()
                                .when(
                                                playerMatchReportRepository
                                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(anyLong()))
                                .thenReturn(List.of());

                PlayerPerformanceSignalService playerPerformanceSignalService = new PlayerPerformanceSignalService(
                                playerMatchReportRepository);

                recommendationService = new RecommendationService(
                                leagueRepository,
                                marketListingRepository,
                                offerService,
                                playerRepository,
                                playerPerformanceSignalService,
                                matchdayDifficultyService,
                                matchdayChangeEligibilityService);

                lenient().when(
                                matchdayDifficultyService.resolveForTeams(
                                                org.mockito.ArgumentMatchers.anyLong(),
                                                org.mockito.ArgumentMatchers.anyList()))
                                .thenReturn(Map.of());

                lenient().when(
                                matchdayChangeEligibilityService
                                                .resolveModifiableByTeam(
                                                                org.mockito.ArgumentMatchers.anyLong()))
                                .thenReturn(Map.of());

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
                assertEquals(
                                PlayerStatus.OK,
                                result.status());

                assertEquals(94, result.score());
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

                assertEquals(
                                PlayerStatus.INJURED,
                                result.status());
                assertEquals(20, result.score());
                assertEquals(
                                50.0,
                                result.scoreBreakdown().base());

                assertEquals(
                                -30.0,
                                result.scoreBreakdown().status());

                assertEquals(
                                20.0,
                                result.scoreBreakdown().scoreBeforeCaps());

                assertFalse(
                                result.scoreBreakdown().affordabilityCapApplied());

                assertFalse(
                                result.scoreBreakdown().auctionBidCapApplied());
                assertEquals(
                                RecommendationType.AVOID,
                                result.recommendation());
        }

        @Test
        void saleShouldPenalizeDoubtPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                81L,
                                "801",
                                "Jugador duda",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                0L,
                                PlayerStatus.DOUBT);

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

                assertEquals(
                                PlayerStatus.DOUBT,
                                result.status());

                assertFalse(
                                result.reasons().contains(
                                                MarketRecommendationReason.INJURED));

                assertEquals(
                                40,
                                result.score());

                assertEquals(
                                RecommendationType.WATCH,
                                result.recommendation());
        }

        @Test
        void saleShouldPenalizeSanctionedPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                82L,
                                "802",
                                "Jugador sancionado",
                                List.of(PlayerPosition.MC),
                                5_000_000L,
                                0L,
                                PlayerStatus.SANCTIONED);

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

                assertEquals(
                                PlayerStatus.SANCTIONED,
                                result.status());

                assertEquals(
                                20,
                                result.score());

                assertEquals(
                                RecommendationType.AVOID,
                                result.recommendation());
        }

        @Test
        void saleShouldPenalizeDiscardedPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                83L,
                                "803",
                                "Jugador no convocado",
                                List.of(PlayerPosition.DF),
                                5_000_000L,
                                0L,
                                PlayerStatus.DISCARDED);

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

                assertEquals(
                                PlayerStatus.DISCARDED,
                                result.status());

                assertEquals(
                                25,
                                result.score());

                assertEquals(
                                RecommendationType.AVOID,
                                result.recommendation());
        }

        @Test
        void saleShouldSlightlyPenalizeWarnedPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                84L,
                                "804",
                                "Jugador apercibido",
                                List.of(PlayerPosition.DF),
                                5_000_000L,
                                0L,
                                PlayerStatus.WARNED);

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

                assertEquals(
                                PlayerStatus.WARNED,
                                result.status());

                assertEquals(
                                45,
                                result.score());

                assertEquals(
                                RecommendationType.WATCH,
                                result.recommendation());
        }

        @Test
        void auctionShouldReduceRecommendedBidForDoubtPlayer() {
                League league = createLeague();

                Player player = createPlayer(
                                85L,
                                "805",
                                "Subasta duda",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                PlayerStatus.DOUBT);

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
                                950_000L,
                                result.maximumRecommendedBid());
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
                assertTrue(
                                result.scoreBreakdown().affordabilityCapApplied());

                assertTrue(
                                result.scoreBreakdown().scoreBeforeCaps() > 25);
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
                                1_120_000L,
                                result.maximumRecommendedBid());

                assertEquals(75, result.score());

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
                                880_000L,
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
        void auctionShouldReportBidCapWhenCurrentBidCapsScore() {
                League league = createLeague();

                Player player = createPlayer(
                                27L,
                                "207",
                                "Puja limitada",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                10_000L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.AUCTION,
                                player,
                                800_000L,
                                1_100_000L,
                                league);

                mockCommon(
                                5_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                1_025_000L,
                                result.maximumRecommendedBid());

                assertEquals(
                                25,
                                result.score());

                assertTrue(
                                result.scoreBreakdown()
                                                .scoreBeforeCaps() > 25);

                assertTrue(
                                result.scoreBreakdown()
                                                .auctionBidCapApplied());

                assertFalse(
                                result.scoreBreakdown()
                                                .affordabilityCapApplied());

                assertEquals(
                                RecommendationType.AVOID,
                                result.recommendation());
        }

        @Test
        void marketRecommendationShouldIncreaseScoreForExcellentRecentForm() {
                League league = createLeague();

                Player player = createPlayer(
                                70L,
                                "700",
                                "Jugador en racha",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                PlayerMatchReport latestReport = new PlayerMatchReport(
                                player,
                                9001L,
                                1L,
                                "Jornada 2",
                                "J2",
                                LocalDateTime.of(2026, 8, 10, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                11);

                PlayerMatchReport previousReport = new PlayerMatchReport(
                                player,
                                9000L,
                                1L,
                                "Jornada 1",
                                "J1",
                                LocalDateTime.of(2026, 8, 3, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                11);

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(70L))
                                .thenReturn(
                                                List.of(
                                                                latestReport,
                                                                previousReport));

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(65, result.score());
                assertEquals(
                                RecommendationType.BUY,
                                result.recommendation());
        }

        @Test
        void marketRecommendationShouldDecreaseScoreForPoorRecentForm() {
                League league = createLeague();

                Player player = createPlayer(
                                71L,
                                "701",
                                "Jugador en mala racha",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                PlayerMatchReport latestReport = new PlayerMatchReport(
                                player,
                                9003L,
                                2L,
                                "Jornada 2",
                                "J2",
                                LocalDateTime.of(2026, 8, 10, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                2);

                PlayerMatchReport previousReport = new PlayerMatchReport(
                                player,
                                9002L,
                                2L,
                                "Jornada 1",
                                "J1",
                                LocalDateTime.of(2026, 8, 3, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                1);

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(71L))
                                .thenReturn(
                                                List.of(
                                                                latestReport,
                                                                previousReport));

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(45, result.score());
                assertEquals(
                                RecommendationType.WATCH,
                                result.recommendation());
        }

        @Test
        void marketRecommendationShouldIgnoreRecentFormAcrossDifferentSeasons() {
                League league = createLeague();

                Player player = createPlayer(
                                72L,
                                "702",
                                "Jugador cambio temporada",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                PlayerMatchReport latestReport = new PlayerMatchReport(
                                player,
                                9101L,
                                1L,
                                "Jornada 1",
                                "J1",
                                LocalDateTime.of(2026, 8, 15, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                11);

                PlayerMatchReport previousReport = new PlayerMatchReport(
                                player,
                                9100L,
                                38L,
                                "Jornada 38",
                                "J38",
                                LocalDateTime.of(2026, 5, 23, 21, 0),
                                "2025-2026",
                                true,
                                null,
                                11);

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(72L))
                                .thenReturn(List.of(
                                                latestReport,
                                                previousReport));

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(50, result.score());
                assertEquals(
                                RecommendationType.WATCH,
                                result.recommendation());
        }

        @Test
        void marketRecommendationShouldResetRecentFormWhenPlayerDidNotParticipate() {
                League league = createLeague();

                Player player = createPlayer(
                                73L,
                                "703",
                                "Jugador con ausencia",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                PlayerMatchReport latestReport = new PlayerMatchReport(
                                player,
                                9201L,
                                6L,
                                "Jornada 6",
                                "J6",
                                LocalDateTime.of(2026, 9, 20, 21, 0),
                                "2026-2027",
                                false,
                                "injured",
                                null);

                PlayerMatchReport previousReport = new PlayerMatchReport(
                                player,
                                9200L,
                                5L,
                                "Jornada 5",
                                "J5",
                                LocalDateTime.of(2026, 9, 13, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                15);

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(73L))
                                .thenReturn(List.of(
                                                latestReport,
                                                previousReport));

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(50, result.score());
                assertEquals(
                                RecommendationType.WATCH,
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
        void getMarketRecommendationsShouldExcludeOwnListings() {

                League league = createLeague();

                Player player = createPlayer(
                                32L,
                                "302",
                                "Jugador propio en venta",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                100_000L,
                                false);

                Manager ownManager = createManager();

                MarketListing ownListing = new MarketListing(
                                MarketListingType.SALE,
                                player,
                                ownManager,
                                4_000_000L,
                                null,
                                null,
                                false,
                                null,
                                null,
                                null,
                                league);

                mockCommon(
                                10_000_000L,
                                List.of(ownListing));

                List<MarketRecommendationResponse> result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID);

                assertTrue(result.isEmpty());
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
                                "status",
                                PlayerStatus.INJURED);

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
        void squadNeedsShouldTreatSanctionedPlayerAsUnavailable() {

                List<Player> squad = createCompleteSquad();

                Player sanctionedDefender = squad.stream()
                                .filter(player -> player.getPositions().contains(PlayerPosition.DF))
                                .findFirst()
                                .orElseThrow();

                ReflectionTestUtils.setField(
                                sanctionedDefender,
                                "status",
                                PlayerStatus.SANCTIONED);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                assertTrue(
                                result.needScoreByPosition().get("DF") > 0);
        }

        @Test
        void squadNeedsShouldTreatDiscardedPlayerAsUnavailable() {

                List<Player> squad = createCompleteSquad();

                Player discardedMidfielder = squad.stream()
                                .filter(player -> player.getPositions().contains(PlayerPosition.MC))
                                .findFirst()
                                .orElseThrow();

                ReflectionTestUtils.setField(
                                discardedMidfielder,
                                "status",
                                PlayerStatus.DISCARDED);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                assertTrue(
                                result.needScoreByPosition().get("MC") > 0);
        }

        @Test
        void squadNeedsShouldPartiallyPenalizeDoubtPlayer() {
                List<Player> squad = createCompleteSquad();

                Player doubtForward = squad.stream()
                                .filter(player -> player.getPositions().contains(PlayerPosition.DL))
                                .findFirst()
                                .orElseThrow();

                ReflectionTestUtils.setField(
                                doubtForward,
                                "status",
                                PlayerStatus.DOUBT);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                int forwardNeed = result.needScoreByPosition().get("DL");

                assertTrue(forwardNeed > 0);
                assertTrue(forwardNeed < 25);
        }

        @Test
        void squadNeedsShouldNotPenalizeWarnedPlayerAvailability() {
                List<Player> squad = createCompleteSquad();

                Player warnedDefender = squad.stream()
                                .filter(player -> player.getPositions().contains(PlayerPosition.DF))
                                .findFirst()
                                .orElseThrow();

                ReflectionTestUtils.setField(
                                warnedDefender,
                                "status",
                                PlayerStatus.WARNED);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                assertEquals(
                                0,
                                result.needScoreByPosition().get("DF"));
        }

        @Test
        void squadNeedsShouldNotDoubleCountMultiPositionPlayerForFormationCoverage() {
                Manager manager = createManager();

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                2001L,
                                                "2001",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                2002L,
                                                "2002",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2003L,
                                                "2003",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2004L,
                                                "2004",
                                                "Polivalente",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2005L,
                                                "2005",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2006L,
                                                "2006",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2007L,
                                                "2007",
                                                "MC 3",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2008L,
                                                "2008",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2009L,
                                                "2009",
                                                "DL 2",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2010L,
                                                "2010",
                                                "DL 3",
                                                List.of(PlayerPosition.DL),
                                                manager));

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                /*
                 * El jugador DF/MC no puede cubrir simultáneamente
                 * el tercer defensa y el cuarto centrocampista
                 * de un 3-4-3.
                 */
                assertTrue(
                                result.needScoreByPosition().get("DF") > 0
                                                || result.needScoreByPosition().get("MC") > 0);
        }

        @Test
        void squadNeedsShouldDetectWhenMultiPositionCountsHideLackOfElevenPlayers() {
                Manager manager = createManager();

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                2301L,
                                                "2301",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                2302L,
                                                "2302",
                                                "DF MC 1",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2303L,
                                                "2303",
                                                "DF MC 2",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2304L,
                                                "2304",
                                                "DF MC 3",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2305L,
                                                "2305",
                                                "DF MC 4",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2306L,
                                                "2306",
                                                "DF MC 5",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2307L,
                                                "2307",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2308L,
                                                "2308",
                                                "DL 2",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2309L,
                                                "2309",
                                                "DL 3",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2310L,
                                                "2310",
                                                "DL 4",
                                                List.of(PlayerPosition.DL),
                                                manager));

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                /*
                 * Aparentemente tenemos:
                 *
                 * DF = 5
                 * MC = 5
                 * DL = 4
                 *
                 * Pero esos cinco DF/MC son los mismos cinco jugadores.
                 *
                 * En total solo existen 9 jugadores de campo,
                 * por lo que es imposible construir ningún once válido.
                 *
                 * El sistema debe detectar que la plantilla
                 * todavía tiene alguna necesidad real.
                 */
                assertTrue(
                                result.needScoreByPosition().get("DF") > 0
                                                || result.needScoreByPosition().get("MC") > 0
                                                || result.needScoreByPosition().get("DL") > 0);
        }

        @Test
        void squadNeedsShouldRecognizeFlexibleMultiPositionSquad() {
                Manager manager = createManager();

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                2101L,
                                                "2101",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                2102L,
                                                "2102",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2103L,
                                                "2103",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2104L,
                                                "2104",
                                                "DF MC 1",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2105L,
                                                "2105",
                                                "DF MC 2",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2106L,
                                                "2106",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2107L,
                                                "2107",
                                                "MC DL",
                                                List.of(
                                                                PlayerPosition.MC,
                                                                PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2108L,
                                                "2108",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2109L,
                                                "2109",
                                                "DL 2",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2110L,
                                                "2110",
                                                "DL 3",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2111L,
                                                "2111",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager));

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                /*
                 * La plantilla tiene suficientes combinaciones
                 * para cubrir al menos una formación válida
                 * sin reutilizar jugadores.
                 */
                assertTrue(
                                result.needScoreByPosition().get("DF") < 75);

                assertTrue(
                                result.needScoreByPosition().get("MC") < 75);

                assertTrue(
                                result.needScoreByPosition().get("DL") < 75);
        }

        @Test
        void squadNeedsShouldLoseFormationCoverageWhenVersatilePlayerIsUnavailable() {
                Manager manager = createManager();

                Player versatile = createOwnedPlayer(
                                2204L,
                                "2204",
                                "Polivalente clave",
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                versatile,
                                "status",
                                PlayerStatus.SANCTIONED);

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                2201L,
                                                "2201",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                2202L,
                                                "2202",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2203L,
                                                "2203",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                versatile,

                                createOwnedPlayer(
                                                2205L,
                                                "2205",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2206L,
                                                "2206",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2207L,
                                                "2207",
                                                "MC 3",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2208L,
                                                "2208",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2209L,
                                                "2209",
                                                "DL 2",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2210L,
                                                "2210",
                                                "DL 3",
                                                List.of(PlayerPosition.DL),
                                                manager));

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                SquadNeedsResponse result = recommendationService.getSquadNeeds(LEAGUE_ID);

                assertTrue(
                                result.needScoreByPosition().get("DF") >= 25);

                assertTrue(
                                result.needScoreByPosition().get("MC") >= 25);
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

        @Test
        void marketRecommendationShouldIgnoreRecentFormWhenRoundsAreNotConsecutive() {
                League league = createLeague();

                Player player = createPlayer(
                                74L,
                                "704",
                                "Jugador sin continuidad",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                PlayerMatchReport latestReport = new PlayerMatchReport(
                                player,
                                9301L,
                                4908L,
                                "Jornada 9",
                                "J9",
                                LocalDateTime.of(2026, 10, 11, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                15);

                PlayerMatchReport previousReport = new PlayerMatchReport(
                                player,
                                9300L,
                                4900L,
                                "Jornada 1",
                                "J1",
                                LocalDateTime.of(2026, 8, 16, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                15);

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(74L))
                                .thenReturn(
                                                List.of(
                                                                latestReport,
                                                                previousReport));

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                /*
                 * J1 y J9 pertenecen a la misma temporada,
                 * pero no forman una racha consecutiva.
                 *
                 * Por tanto, esos 15 + 15 no deben aportar
                 * bonus de forma reciente.
                 */
                assertEquals(
                                50,
                                result.score());

                assertEquals(
                                RecommendationType.WATCH,
                                result.recommendation());

                assertEquals(
                                0,
                                result.scoreBreakdown().recentFormSampleSize());

                assertEquals(
                                0.0,
                                result.scoreBreakdown().recentForm());
        }

        @Test
        void marketRecommendationShouldExposeHistoricalPerformanceWithoutChangingScore() {
                League league = createLeague();

                Player player = createPlayer(
                                79L,
                                "709",
                                "Jugador con histórico",
                                List.of(PlayerPosition.MC),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                List<PlayerMatchReport> historicalReports = List.of(
                                new PlayerMatchReport(
                                                player,
                                                9805L,
                                                4955L,
                                                "Jornada 5",
                                                "J5",
                                                LocalDateTime.of(2026, 9, 20, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9804L,
                                                4954L,
                                                "Jornada 4",
                                                "J4",
                                                LocalDateTime.of(2026, 9, 13, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                6),
                                new PlayerMatchReport(
                                                player,
                                                9803L,
                                                4953L,
                                                "Jornada 3",
                                                "J3",
                                                LocalDateTime.of(2026, 9, 6, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                4),
                                new PlayerMatchReport(
                                                player,
                                                9802L,
                                                4952L,
                                                "Jornada 2",
                                                "J2",
                                                LocalDateTime.of(2026, 8, 30, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                2));

                when(
                                playerMatchReportRepository
                                                .findTop10ByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                                79L))
                                .thenReturn(historicalReports);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                5.0,
                                result.scoreBreakdown().historicalAveragePoints());

                assertEquals(
                                4,
                                result.scoreBreakdown().historicalSampleSize());

                /*
                 * El histórico todavía es informativo.
                 * No debe modificar el score.
                 */
                assertEquals(
                                50,
                                result.score());
        }

        @Test
        void marketRecommendationShouldExposeZeroHistoricalPerformanceWhenNoReportsExist() {
                League league = createLeague();

                Player player = createPlayer(
                                80L,
                                "710",
                                "Jugador sin histórico",
                                List.of(PlayerPosition.DF),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                when(
                                playerMatchReportRepository
                                                .findTop10ByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                                80L))
                                .thenReturn(List.of());

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                0.0,
                                result.scoreBreakdown().historicalAveragePoints());

                assertEquals(
                                0,
                                result.scoreBreakdown().historicalSampleSize());

                assertEquals(
                                50,
                                result.score());
        }

        @Test
        void marketRecommendationShouldIgnoreHistoricalPerformanceForCoach() {
                League league = createLeague();

                Player coach = createPlayer(
                                81L,
                                "711",
                                "Entrenador",
                                List.of(PlayerPosition.E),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                coach,
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
                                0.0,
                                result.scoreBreakdown().historicalAveragePoints());

                assertEquals(
                                0,
                                result.scoreBreakdown().historicalSampleSize());

                assertEquals(
                                50,
                                result.score());
        }

        @Test
        void marketRecommendationShouldRewardStrongHistoricalPerformanceWithEnoughSample() {
                League league = createLeague();

                Player player = createPlayer(
                                82L,
                                "712",
                                "Jugador histórico fuerte",
                                List.of(PlayerPosition.MC),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                List<PlayerMatchReport> historicalReports = List.of(
                                new PlayerMatchReport(
                                                player, 10001L, 5001L,
                                                "Jornada 10", "J10",
                                                LocalDateTime.of(2026, 5, 10, 21, 0),
                                                "2025-2026",
                                                true, null, 7),
                                new PlayerMatchReport(
                                                player, 10002L, 5002L,
                                                "Jornada 9", "J9",
                                                LocalDateTime.of(2026, 5, 3, 21, 0),
                                                "2025-2026",
                                                true, null, 6),
                                new PlayerMatchReport(
                                                player, 10003L, 5003L,
                                                "Jornada 8", "J8",
                                                LocalDateTime.of(2026, 4, 26, 21, 0),
                                                "2025-2026",
                                                true, null, 7),
                                new PlayerMatchReport(
                                                player, 10004L, 5004L,
                                                "Jornada 7", "J7",
                                                LocalDateTime.of(2026, 4, 19, 21, 0),
                                                "2025-2026",
                                                true, null, 6),
                                new PlayerMatchReport(
                                                player, 10005L, 5005L,
                                                "Jornada 6", "J6",
                                                LocalDateTime.of(2026, 4, 12, 21, 0),
                                                "2025-2026",
                                                true, null, 6));

                when(
                                playerMatchReportRepository
                                                .findTop10ByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                                82L))
                                .thenReturn(historicalReports);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                6.4,
                                result.scoreBreakdown().historicalAveragePoints());

                assertEquals(
                                5,
                                result.scoreBreakdown().historicalSampleSize());

                /*
                 * Un histórico >= 6 con muestra suficiente
                 * deberá aportar +10.
                 *
                 * Actualmente todavía no aporta nada,
                 * por lo que este test debe fallar primero.
                 */
                assertEquals(
                                60,
                                result.score());

                assertTrue(
                                result.reasons().contains(
                                                MarketRecommendationReason.STRONG_HISTORICAL_PERFORMANCE));

                assertEquals(
                                10,
                                result.scoreBreakdown().historicalPerformance());
        }

        @Test
        void shouldIncludePriceBelowMarketReason() {
                League league = createLeague();

                Player player = createPlayer(
                                60L,
                                "600",
                                "Barato",
                                List.of(PlayerPosition.DL),
                                5_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                4_500_000L,
                                null,
                                league);

                mockCommon(
                                10_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertTrue(
                                result.reasons().contains(
                                                MarketRecommendationReason.PRICE_BELOW_MARKET));
        }

        @Test
        void shouldIncludeFastValueRiseReason() {
                League league = createLeague();

                Player player = createPlayer(
                                61L,
                                "601",
                                "En subida",
                                List.of(PlayerPosition.MC),
                                1_000_000L,
                                50_000L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                mockCommon(
                                10_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertTrue(
                                result.reasons().contains(
                                                MarketRecommendationReason.VALUE_RISING_FAST));
        }

        @Test
        void shouldIncludeInjuredAndUnaffordableReasons() {
                League league = createLeague();

                Player player = createPlayer(
                                62L,
                                "602",
                                "Caro y lesionado",
                                List.of(PlayerPosition.DF),
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
                                1_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertTrue(
                                result.reasons().contains(
                                                MarketRecommendationReason.INJURED));

                assertTrue(
                                result.reasons().contains(
                                                MarketRecommendationReason.UNAFFORDABLE));
        }

        @Test
        void shouldIncludeExcellentRecentFormReason() {
                League league = createLeague();

                Player player = createPlayer(
                                63L,
                                "603",
                                "En gran forma",
                                List.of(PlayerPosition.DL),
                                1_000_000L,
                                0L,
                                false);

                PlayerMatchReport latestReport = new PlayerMatchReport(
                                player,
                                9101L,
                                1L,
                                "Jornada 2",
                                "J2",
                                LocalDateTime.of(2026, 8, 10, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                11);

                PlayerMatchReport previousReport = new PlayerMatchReport(
                                player,
                                9100L,
                                1L,
                                "Jornada 1",
                                "J1",
                                LocalDateTime.of(2026, 8, 3, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                9);

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(63L))
                                .thenReturn(
                                                List.of(
                                                                latestReport,
                                                                previousReport));

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                mockCommon(
                                10_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertTrue(
                                result.reasons().contains(
                                                MarketRecommendationReason.EXCELLENT_RECENT_FORM));
        }

        @Test
        void shouldIncludeGoodRecentFormReason() {
                League league = createLeague();

                Player player = createPlayer(
                                64L,
                                "604",
                                "En buena forma",
                                List.of(PlayerPosition.MC),
                                1_000_000L,
                                0L,
                                false);

                PlayerMatchReport latestReport = new PlayerMatchReport(
                                player,
                                9201L,
                                1L,
                                "Jornada 2",
                                "J2",
                                LocalDateTime.of(2026, 8, 10, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                7);

                PlayerMatchReport previousReport = new PlayerMatchReport(
                                player,
                                9200L,
                                1L,
                                "Jornada 1",
                                "J1",
                                LocalDateTime.of(2026, 8, 3, 21, 0),
                                "2026-2027",
                                true,
                                null,
                                7);

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(64L))
                                .thenReturn(
                                                List.of(
                                                                latestReport,
                                                                previousReport));

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                mockCommon(
                                10_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertTrue(
                                result.reasons().contains(
                                                MarketRecommendationReason.GOOD_RECENT_FORM));
        }

        @Test
        void marketRecommendationShouldStopRecentFormStreakAtMissingRound() {
                League league = createLeague();

                Player player = createPlayer(
                                76L,
                                "706",
                                "Jugador con hueco",
                                List.of(PlayerPosition.MC),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                List<PlayerMatchReport> reports = List.of(
                                new PlayerMatchReport(
                                                player,
                                                9505L,
                                                4922L,
                                                "Jornada 5",
                                                "J5",
                                                LocalDateTime.of(2026, 9, 20, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9504L,
                                                4921L,
                                                "Jornada 4",
                                                "J4",
                                                LocalDateTime.of(2026, 9, 13, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9502L,
                                                4919L,
                                                "Jornada 2",
                                                "J2",
                                                LocalDateTime.of(2026, 8, 30, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                1),
                                new PlayerMatchReport(
                                                player,
                                                9501L,
                                                4918L,
                                                "Jornada 1",
                                                "J1",
                                                LocalDateTime.of(2026, 8, 23, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                1));

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(76L))
                                .thenReturn(reports);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                65,
                                result.score());
        }

        @Test
        void marketRecommendationShouldStopRecentFormStreakAtNonParticipation() {
                League league = createLeague();

                Player player = createPlayer(
                                77L,
                                "707",
                                "Jugador con ausencia",
                                List.of(PlayerPosition.MC),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                List<PlayerMatchReport> reports = List.of(
                                new PlayerMatchReport(
                                                player,
                                                9605L,
                                                4932L,
                                                "Jornada 5",
                                                "J5",
                                                LocalDateTime.of(2026, 9, 20, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9604L,
                                                4931L,
                                                "Jornada 4",
                                                "J4",
                                                LocalDateTime.of(2026, 9, 13, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9603L,
                                                4930L,
                                                "Jornada 3",
                                                "J3",
                                                LocalDateTime.of(2026, 9, 6, 21, 0),
                                                "2026-2027",
                                                false,
                                                "injured",
                                                null),
                                new PlayerMatchReport(
                                                player,
                                                9602L,
                                                4929L,
                                                "Jornada 2",
                                                "J2",
                                                LocalDateTime.of(2026, 8, 30, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                1),
                                new PlayerMatchReport(
                                                player,
                                                9601L,
                                                4928L,
                                                "Jornada 1",
                                                "J1",
                                                LocalDateTime.of(2026, 8, 23, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                1));

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(77L))
                                .thenReturn(reports);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                65,
                                result.score());
        }

        @Test
        void marketRecommendationShouldUseUpToFiveConsecutiveReportsForRecentForm() {
                League league = createLeague();

                Player player = createPlayer(
                                75L,
                                "705",
                                "Jugador en racha",
                                List.of(PlayerPosition.MC),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                List<PlayerMatchReport> reports = List.of(
                                new PlayerMatchReport(
                                                player,
                                                9405L,
                                                4912L,
                                                "Jornada 5",
                                                "J5",
                                                LocalDateTime.of(2026, 9, 20, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9404L,
                                                4911L,
                                                "Jornada 4",
                                                "J4",
                                                LocalDateTime.of(2026, 9, 13, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9403L,
                                                4910L,
                                                "Jornada 3",
                                                "J3",
                                                LocalDateTime.of(2026, 9, 6, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9402L,
                                                4909L,
                                                "Jornada 2",
                                                "J2",
                                                LocalDateTime.of(2026, 8, 30, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9401L,
                                                4908L,
                                                "Jornada 1",
                                                "J1",
                                                LocalDateTime.of(2026, 8, 23, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8));

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(75L))
                                .thenReturn(reports);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                assertEquals(
                                65,
                                result.score());

                assertEquals(
                                RecommendationType.BUY,
                                result.recommendation());

                assertEquals(
                                5,
                                result.scoreBreakdown().recentFormSampleSize());

                assertEquals(
                                15.0,
                                result.scoreBreakdown().recentForm());
        }

        @Test
        void marketRecommendationShouldGiveMoreWeightToMostRecentMatches() {
                League league = createLeague();

                Player player = createPlayer(
                                78L,
                                "708",
                                "Jugador mejorando",
                                List.of(PlayerPosition.MC),
                                1_000_000L,
                                0L,
                                false);

                MarketListing listing = createListing(
                                MarketListingType.SALE,
                                player,
                                1_000_000L,
                                null,
                                league);

                List<PlayerMatchReport> reports = List.of(
                                new PlayerMatchReport(
                                                player,
                                                9705L,
                                                4945L,
                                                "Jornada 5",
                                                "J5",
                                                LocalDateTime.of(2026, 9, 20, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9704L,
                                                4944L,
                                                "Jornada 4",
                                                "J4",
                                                LocalDateTime.of(2026, 9, 13, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                8),
                                new PlayerMatchReport(
                                                player,
                                                9703L,
                                                4943L,
                                                "Jornada 3",
                                                "J3",
                                                LocalDateTime.of(2026, 9, 6, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                2),
                                new PlayerMatchReport(
                                                player,
                                                9702L,
                                                4942L,
                                                "Jornada 2",
                                                "J2",
                                                LocalDateTime.of(2026, 8, 30, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                2),
                                new PlayerMatchReport(
                                                player,
                                                9701L,
                                                4941L,
                                                "Jornada 1",
                                                "J1",
                                                LocalDateTime.of(2026, 8, 23, 21, 0),
                                                "2026-2027",
                                                true,
                                                null,
                                                2));

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(78L))
                                .thenReturn(reports);

                mockCommon(
                                2_000_000L,
                                List.of(listing));

                MarketRecommendationResponse result = recommendationService
                                .getMarketRecommendations(LEAGUE_ID)
                                .get(0);

                /*
                 * Media simple:
                 * (8 + 8 + 2 + 2 + 2) / 5 = 4.4
                 * -> no daría bonus.
                 *
                 * Media ponderada por recencia:
                 * (8*5 + 8*4 + 2*3 + 2*2 + 2*1) / 15
                 * = 5.6
                 * -> GOOD_RECENT_FORM, +5.
                 */
                assertEquals(
                                55,
                                result.score());

                assertTrue(
                                result.reasons().contains(
                                                MarketRecommendationReason.GOOD_RECENT_FORM));
        }

        @Test
        void formationRecommendationShouldHandleUnfeasibleCurrentFormationWithoutArtificialImprovement() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "5-4-1");

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                1901L,
                                                "1901",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                1902L,
                                                "1902",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                1903L,
                                                "1903",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                1904L,
                                                "1904",
                                                "DF 3",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                1905L,
                                                "1905",
                                                "DF 4",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                1906L,
                                                "1906",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                1907L,
                                                "1907",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                1908L,
                                                "1908",
                                                "MC 3",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                1909L,
                                                "1909",
                                                "MC 4",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                1910L,
                                                "1910",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                1911L,
                                                "1911",
                                                "DL 2",
                                                List.of(PlayerPosition.DL),
                                                manager));

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                for (Player player : squad) {
                        mockPerformanceReports(
                                        player,
                                        5);
                }

                FormationRecommendationResponse result = recommendationService
                                .getFormationRecommendation(LEAGUE_ID);

                assertEquals(
                                "5-4-1",
                                result.currentFormation());

                assertEquals(
                                "4-4-2",
                                result.recommendedFormation());

                assertEquals(
                                0.0,
                                result.currentScore());

                assertTrue(
                                result.recommendedScore() > 0);

                assertEquals(
                                result.recommendedScore(),
                                result.improvement());
        }

        @Test
        void formationRecommendationShouldPrefer442WhenSecondForwardClearlyImprovesBestEleven() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "5-4-1");

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                2001L,
                                                "2001",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                2002L,
                                                "2002",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                2003L,
                                                "2003",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                2004L,
                                                "2004",
                                                "DF 3",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                2005L,
                                                "2005",
                                                "DF 4",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2006L,
                                                "2006",
                                                "DF 5 flojo",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2007L,
                                                "2007",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                2008L,
                                                "2008",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                2009L,
                                                "2009",
                                                "MC 3",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                2010L,
                                                "2010",
                                                "MC 4",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2011L,
                                                "2011",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2012L,
                                                "2012",
                                                "DL 2 fuerte",
                                                List.of(PlayerPosition.DL),
                                                manager));

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                for (Player player : squad) {

                        int points;

                        if (player.getId().equals(2006L)) {
                                points = 2;
                        } else if (player.getId().equals(2012L)) {
                                points = 9;
                        } else {
                                points = 5;
                        }

                        mockPerformanceReports(
                                        player,
                                        points);
                }

                FormationRecommendationResponse result = recommendationService
                                .getFormationRecommendation(
                                                LEAGUE_ID);

                assertEquals(
                                "5-4-1",
                                result.currentFormation());

                assertEquals(
                                "4-4-2",
                                result.recommendedFormation());

                assertTrue(
                                result.recommendedScore() > result.currentScore());

                assertTrue(
                                result.improvement() > 0);
        }

        @Test
        void formationRecommendationShouldKeep541WhenFifthDefenderIsBetterThanSecondForward() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "5-4-1");

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                2101L,
                                                "2101",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                2102L,
                                                "2102",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                2103L,
                                                "2103",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                2104L,
                                                "2104",
                                                "DF 3",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                2105L,
                                                "2105",
                                                "DF 4",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2106L,
                                                "2106",
                                                "DF 5 fuerte",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                2107L,
                                                "2107",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                2108L,
                                                "2108",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                2109L,
                                                "2109",
                                                "MC 3",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                2110L,
                                                "2110",
                                                "MC 4",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                2111L,
                                                "2111",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                2112L,
                                                "2112",
                                                "DL 2 flojo",
                                                List.of(PlayerPosition.DL),
                                                manager));

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                for (Player player : squad) {

                        int points;

                        if (player.getId().equals(2106L)) {
                                points = 9;
                        } else if (player.getId().equals(2112L)) {
                                points = 2;
                        } else {
                                points = 5;
                        }

                        mockPerformanceReports(
                                        player,
                                        points);
                }

                FormationRecommendationResponse result = recommendationService
                                .getFormationRecommendation(
                                                LEAGUE_ID);

                assertEquals(
                                "5-4-1",
                                result.currentFormation());

                assertEquals(
                                "5-4-1",
                                result.recommendedFormation());

                assertEquals(
                                0.0,
                                result.improvement());
        }

        @Test
        void formationRecommendationShouldNotExposeSentinelWhenNoFormationIsFeasible() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "5-4-1");

                List<Player> squad = List.of(
                                createOwnedPlayer(1921L, "1921", "PT", List.of(PlayerPosition.PT), manager),
                                createOwnedPlayer(1922L, "1922", "DF 1", List.of(PlayerPosition.DF), manager),
                                createOwnedPlayer(1923L, "1923", "DF 2", List.of(PlayerPosition.DF), manager),
                                createOwnedPlayer(1924L, "1924", "DF 3", List.of(PlayerPosition.DF), manager),
                                createOwnedPlayer(1925L, "1925", "DF 4", List.of(PlayerPosition.DF), manager),
                                createOwnedPlayer(1926L, "1926", "MC 1", List.of(PlayerPosition.MC), manager),
                                createOwnedPlayer(1927L, "1927", "MC 2", List.of(PlayerPosition.MC), manager),
                                createOwnedPlayer(1928L, "1928", "MC 3", List.of(PlayerPosition.MC), manager),
                                createOwnedPlayer(1929L, "1929", "MC 4", List.of(PlayerPosition.MC), manager),
                                createOwnedPlayer(1930L, "1930", "DL", List.of(PlayerPosition.DL), manager));

                when(leagueRepository.existsById(LEAGUE_ID)).thenReturn(true);
                when(playerRepository.findAllByLeague_Id(LEAGUE_ID)).thenReturn(squad);

                FormationRecommendationResponse result = recommendationService.getFormationRecommendation(LEAGUE_ID);

                assertEquals("5-4-1", result.currentFormation());
                assertEquals("5-4-1", result.recommendedFormation());
                assertEquals(0.0, result.currentScore());
                assertEquals(0.0, result.recommendedScore());
                assertEquals(0.0, result.improvement());
                assertEquals(0, result.confidence());
        }

        @Test
        void recommendedLineupShouldKeepCurrentFormationWhenAnotherFormationTies() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "5-4-1");

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                3001L,
                                                "3001",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                3002L,
                                                "3002",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                3003L,
                                                "3003",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                3004L,
                                                "3004",
                                                "DF 3",
                                                List.of(PlayerPosition.DF),
                                                manager),
                                createOwnedPlayer(
                                                3005L,
                                                "3005",
                                                "DF MC",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                3006L,
                                                "3006",
                                                "DF DL",
                                                List.of(
                                                                PlayerPosition.DF,
                                                                PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                3007L,
                                                "3007",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                3008L,
                                                "3008",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager),
                                createOwnedPlayer(
                                                3009L,
                                                "3009",
                                                "MC DL",
                                                List.of(
                                                                PlayerPosition.MC,
                                                                PlayerPosition.DL),
                                                manager),
                                createOwnedPlayer(
                                                3010L,
                                                "3010",
                                                "MC DL 2",
                                                List.of(
                                                                PlayerPosition.MC,
                                                                PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                3011L,
                                                "3011",
                                                "DL",
                                                List.of(PlayerPosition.DL),
                                                manager));

                for (Player player : squad) {

                        ReflectionTestUtils.setField(
                                        player,
                                        "starter",
                                        true);

                        mockPerformanceReports(
                                        player,
                                        5);
                }

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                RecommendedLineupResponse result = recommendationService
                                .getRecommendedLineup(
                                                LEAGUE_ID);

                assertEquals(
                                "5-4-1",
                                result.currentFormation());

                assertEquals(
                                "5-4-1",
                                result.recommendedFormation());

                assertEquals(
                                11,
                                result.recommendedStarters().size());

                assertEquals(
                                0.0,
                                result.improvement());

                assertEquals(
                                0,
                                result.confidence());

                assertTrue(
                                result.changes().isEmpty());
        }

        @Test
        void recommendedLineupShouldReplaceStarterWhenReserveImprovesBestEleven() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "5-4-1");

                Player goalkeeper = createOwnedPlayer(
                                3101L,
                                "3101",
                                "PT",
                                List.of(PlayerPosition.PT),
                                manager);

                Player defenderOne = createOwnedPlayer(
                                3102L,
                                "3102",
                                "DF 1",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defenderTwo = createOwnedPlayer(
                                3103L,
                                "3103",
                                "DF 2",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defenderThree = createOwnedPlayer(
                                3104L,
                                "3104",
                                "DF 3",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defenderFour = createOwnedPlayer(
                                3105L,
                                "3105",
                                "DF 4",
                                List.of(PlayerPosition.DF),
                                manager);

                Player weakDefender = createOwnedPlayer(
                                3106L,
                                "3106",
                                "DF titular flojo",
                                List.of(PlayerPosition.DF),
                                manager);

                Player midfielderOne = createOwnedPlayer(
                                3107L,
                                "3107",
                                "MC 1",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielderTwo = createOwnedPlayer(
                                3108L,
                                "3108",
                                "MC 2",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielderThree = createOwnedPlayer(
                                3109L,
                                "3109",
                                "MC 3",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielderFour = createOwnedPlayer(
                                3110L,
                                "3110",
                                "MC 4",
                                List.of(PlayerPosition.MC),
                                manager);

                Player forward = createOwnedPlayer(
                                3111L,
                                "3111",
                                "DL",
                                List.of(PlayerPosition.DL),
                                manager);

                Player strongDefender = createOwnedPlayer(
                                3112L,
                                "3112",
                                "DF suplente fuerte",
                                List.of(PlayerPosition.DF),
                                manager);

                List<Player> squad = List.of(
                                goalkeeper,
                                defenderOne,
                                defenderTwo,
                                defenderThree,
                                defenderFour,
                                weakDefender,
                                midfielderOne,
                                midfielderTwo,
                                midfielderThree,
                                midfielderFour,
                                forward,
                                strongDefender);

                for (Player player : squad) {

                        boolean starter = !player.getId()
                                        .equals(3112L);

                        ReflectionTestUtils.setField(
                                        player,
                                        "starter",
                                        starter);
                }

                for (Player player : squad) {

                        int points;

                        if (player.getId().equals(3106L)) {
                                points = 2;
                        } else if (player.getId().equals(3112L)) {
                                points = 9;
                        } else {
                                points = 5;
                        }

                        mockPerformanceReports(
                                        player,
                                        points);
                }

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                RecommendedLineupResponse result = recommendationService
                                .getRecommendedLineup(
                                                LEAGUE_ID);

                assertEquals(
                                11,
                                result.recommendedStarters().size());

                assertTrue(
                                result.recommendedScore() > result.currentScore());

                assertTrue(
                                result.improvement() > 0);

                assertTrue(
                                result.recommendedStarters()
                                                .stream()
                                                .anyMatch(player -> player.playerId()
                                                                .equals(3112L)));

                assertFalse(
                                result.recommendedStarters()
                                                .stream()
                                                .anyMatch(player -> player.playerId()
                                                                .equals(3106L)));

                assertTrue(
                                result.changes()
                                                .stream()
                                                .anyMatch(change -> "OUT".equals(
                                                                change.type())
                                                                && change.playerId()
                                                                                .equals(3106L)));

                assertTrue(
                                result.changes()
                                                .stream()
                                                .anyMatch(change -> "IN".equals(
                                                                change.type())
                                                                && change.playerId()
                                                                                .equals(3112L)));
        }

        @Test
        void formationPlayerRatingShouldIncreaseAgainstEasyOpponent() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3201L,
                                "3201",
                                "Jugador rival fácil",
                                List.of(PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                10L);

                mockPerformanceReports(
                                player,
                                5);

                OpponentDifficulty difficulty = new OpponentDifficulty(
                                0.0,
                                50.0,
                                50.0,
                                MatchdayVenue.HOME);

                Double rating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of(
                                                10L,
                                                difficulty));

                assertEquals(
                                5.4,
                                rating,
                                0.000001);
        }

        @Test
        void formationPlayerRatingShouldDecreaseAgainstHardOpponent() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3202L,
                                "3202",
                                "Jugador rival difícil",
                                List.of(PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                20L);

                mockPerformanceReports(
                                player,
                                5);

                OpponentDifficulty difficulty = new OpponentDifficulty(
                                100.0,
                                50.0,
                                50.0,
                                MatchdayVenue.AWAY);

                Double rating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of(
                                                20L,
                                                difficulty));

                assertEquals(
                                4.6,
                                rating,
                                0.000001);
        }

        @Test
        void formationPlayerRatingShouldKeepBaselineWithoutMatchdayContext() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3203L,
                                "3203",
                                "Jugador sin contexto",
                                List.of(PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                30L);

                mockPerformanceReports(
                                player,
                                5);

                Double rating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of());

                assertEquals(
                                5.0,
                                rating,
                                0.000001);
        }

        @Test
        void formationPlayerRatingShouldRemainZeroForUnavailablePlayer() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3204L,
                                "3204",
                                "Jugador lesionado",
                                List.of(PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                40L);

                ReflectionTestUtils.setField(
                                player,
                                "status",
                                PlayerStatus.INJURED);

                mockPerformanceReports(
                                player,
                                5);

                OpponentDifficulty difficulty = new OpponentDifficulty(
                                0.0,
                                50.0,
                                50.0,
                                MatchdayVenue.HOME);

                Double rating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of(
                                                40L,
                                                difficulty));

                assertEquals(
                                0.0,
                                rating,
                                0.000001);
        }

        @Test
        void formationPlayerRatingShouldApplyDifficultyAfterDoubtAvailability() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3205L,
                                "3205",
                                "Jugador duda",
                                List.of(PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                50L);

                ReflectionTestUtils.setField(
                                player,
                                "status",
                                PlayerStatus.DOUBT);

                mockPerformanceReports(
                                player,
                                5);

                OpponentDifficulty difficulty = new OpponentDifficulty(
                                0.0,
                                50.0,
                                50.0,
                                MatchdayVenue.HOME);

                Double rating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of(
                                                50L,
                                                difficulty));

                assertEquals(
                                2.7,
                                rating,
                                0.000001);
        }

        @Test
        void formationPlayerRatingShouldPenalizeDefenderMoreThanMidfielderAgainstStrongAttack() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3301L,
                                "3301",
                                "Defensa centrocampista",
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                10L);

                mockPerformanceReports(
                                player,
                                5);

                OpponentDifficulty difficulty = new OpponentDifficulty(
                                50.0,
                                100.0,
                                50.0,
                                MatchdayVenue.AWAY);

                Double defenderRating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.DF,
                                Map.of(
                                                10L,
                                                difficulty));

                Double midfielderRating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of(
                                                10L,
                                                difficulty));

                assertEquals(
                                4.72,
                                defenderRating,
                                0.000001);

                assertEquals(
                                5.0,
                                midfielderRating,
                                0.000001);

                assertTrue(
                                defenderRating < midfielderRating);
        }

        @Test
        void formationPlayerRatingShouldPenalizeForwardMoreThanMidfielderAgainstStrongDefense() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3302L,
                                "3302",
                                "Centrocampista delantero",
                                List.of(
                                                PlayerPosition.MC,
                                                PlayerPosition.DL),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                20L);

                mockPerformanceReports(
                                player,
                                5);

                OpponentDifficulty difficulty = new OpponentDifficulty(
                                50.0,
                                50.0,
                                100.0,
                                MatchdayVenue.AWAY);

                Double midfielderRating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of(
                                                20L,
                                                difficulty));

                Double forwardRating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.DL,
                                Map.of(
                                                20L,
                                                difficulty));

                assertEquals(
                                5.0,
                                midfielderRating,
                                0.000001);

                assertEquals(
                                4.72,
                                forwardRating,
                                0.000001);

                assertTrue(
                                forwardRating < midfielderRating);
        }

        @Test
        void formationPlayerRatingShouldUseOverallDifficultyForMidfielder() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3303L,
                                "3303",
                                "Centrocampista",
                                List.of(PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                30L);

                mockPerformanceReports(
                                player,
                                5);

                OpponentDifficulty difficulty = new OpponentDifficulty(
                                75.0,
                                0.0,
                                100.0,
                                MatchdayVenue.AWAY);

                Double rating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of(
                                                30L,
                                                difficulty));

                assertEquals(
                                4.8,
                                rating,
                                0.000001);
        }

        @Test
        void formationPlayerRatingShouldDependOnAssignedPositionForMultiPositionPlayer() {

                Manager manager = createManager();

                Player player = createOwnedPlayer(
                                3304L,
                                "3304",
                                "Jugador multiposición",
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC,
                                                PlayerPosition.DL),
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "teamId",
                                40L);

                mockPerformanceReports(
                                player,
                                5);

                OpponentDifficulty difficulty = new OpponentDifficulty(
                                50.0,
                                100.0,
                                0.0,
                                MatchdayVenue.HOME);

                Double defenderRating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.DF,
                                Map.of(
                                                40L,
                                                difficulty));

                Double midfielderRating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.MC,
                                Map.of(
                                                40L,
                                                difficulty));

                Double forwardRating = ReflectionTestUtils.invokeMethod(
                                recommendationService,
                                "calculateFormationPlayerRating",
                                player,
                                PlayerPosition.DL,
                                Map.of(
                                                40L,
                                                difficulty));

                assertEquals(
                                4.72,
                                defenderRating,
                                0.000001);

                assertEquals(
                                5.0,
                                midfielderRating,
                                0.000001);

                assertEquals(
                                5.28,
                                forwardRating,
                                0.000001);

                assertTrue(
                                forwardRating > midfielderRating);

                assertTrue(
                                midfielderRating > defenderRating);
        }

        @Test
        void recommendedLineupShouldAssignMultiPositionPlayersToBestMatchupSlots() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "4-4-2");

                Player flexibleEasyDefense = createOwnedPlayer(
                                3401L,
                                "3401",
                                "Polivalente rival ataque débil",
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC),
                                manager);

                Player flexibleHardDefense = createOwnedPlayer(
                                3402L,
                                "3402",
                                "Polivalente rival ataque fuerte",
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC),
                                manager);

                ReflectionTestUtils.setField(
                                flexibleEasyDefense,
                                "teamId",
                                101L);

                ReflectionTestUtils.setField(
                                flexibleHardDefense,
                                "teamId",
                                102L);

                List<Player> squad = List.of(
                                createOwnedPlayer(
                                                3410L,
                                                "3410",
                                                "PT",
                                                List.of(PlayerPosition.PT),
                                                manager),

                                createOwnedPlayer(
                                                3411L,
                                                "3411",
                                                "DF 1",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                3412L,
                                                "3412",
                                                "DF 2",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                createOwnedPlayer(
                                                3413L,
                                                "3413",
                                                "DF 3",
                                                List.of(PlayerPosition.DF),
                                                manager),

                                flexibleEasyDefense,
                                flexibleHardDefense,

                                createOwnedPlayer(
                                                3414L,
                                                "3414",
                                                "MC 1",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                3415L,
                                                "3415",
                                                "MC 2",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                3416L,
                                                "3416",
                                                "MC 3",
                                                List.of(PlayerPosition.MC),
                                                manager),

                                createOwnedPlayer(
                                                3417L,
                                                "3417",
                                                "DL 1",
                                                List.of(PlayerPosition.DL),
                                                manager),

                                createOwnedPlayer(
                                                3418L,
                                                "3418",
                                                "DL 2",
                                                List.of(PlayerPosition.DL),
                                                manager));

                for (Player player : squad) {

                        ReflectionTestUtils.setField(
                                        player,
                                        "starter",
                                        true);

                        mockPerformanceReports(
                                        player,
                                        5);
                }

                OpponentDifficulty easyDefenseMatchup = new OpponentDifficulty(
                                50.0,
                                0.0,
                                50.0,
                                MatchdayVenue.HOME);

                OpponentDifficulty hardDefenseMatchup = new OpponentDifficulty(
                                50.0,
                                100.0,
                                50.0,
                                MatchdayVenue.AWAY);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                when(matchdayDifficultyService.resolveForTeams(
                                org.mockito.ArgumentMatchers.eq(LEAGUE_ID),
                                org.mockito.ArgumentMatchers.anyList()))
                                .thenReturn(
                                                Map.of(
                                                                101L,
                                                                easyDefenseMatchup,
                                                                102L,
                                                                hardDefenseMatchup));

                RecommendedLineupResponse result = recommendationService.getRecommendedLineup(
                                LEAGUE_ID);

                var easyDefenseAssignment = result.recommendedStarters()
                                .stream()
                                .filter(player -> player.playerId()
                                                .equals(3401L))
                                .findFirst()
                                .orElseThrow();

                var hardDefenseAssignment = result.recommendedStarters()
                                .stream()
                                .filter(player -> player.playerId()
                                                .equals(3402L))
                                .findFirst()
                                .orElseThrow();

                assertEquals(
                                "DF",
                                easyDefenseAssignment.position());

                assertEquals(
                                "MC",
                                hardDefenseAssignment.position());

                assertTrue(
                                easyDefenseAssignment.rating() > hardDefenseAssignment.rating());

                assertEquals(
                                55.28,
                                result.currentScore(),
                                0.000001);
        }

        private void mockPerformanceReports(
                        Player player,
                        int points) {

                List<PlayerMatchReport> reports = List.of(
                                new PlayerMatchReport(
                                                player,
                                                100_000L + player.getId(),
                                                5001L,
                                                "Jornada 3",
                                                "J3",
                                                LocalDateTime.of(
                                                                2026,
                                                                8,
                                                                24,
                                                                21,
                                                                0),
                                                "2026-2027",
                                                true,
                                                null,
                                                points),

                                new PlayerMatchReport(
                                                player,
                                                200_000L + player.getId(),
                                                5000L,
                                                "Jornada 2",
                                                "J2",
                                                LocalDateTime.of(
                                                                2026,
                                                                8,
                                                                17,
                                                                21,
                                                                0),
                                                "2026-2027",
                                                true,
                                                null,
                                                points),

                                new PlayerMatchReport(
                                                player,
                                                300_000L + player.getId(),
                                                4999L,
                                                "Jornada 1",
                                                "J1",
                                                LocalDateTime.of(
                                                                2026,
                                                                8,
                                                                10,
                                                                21,
                                                                0),
                                                "2026-2027",
                                                true,
                                                null,
                                                points));

                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(
                                                                player.getId()))
                                .thenReturn(reports);
        }

        @Test
        void recommendedLineupShouldKeepLockedStarterEvenWhenBetterReplacementExists() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "4-4-2");

                Player goalkeeper = createOwnedPlayer(
                                4001L,
                                "4001",
                                "Portero",
                                List.of(PlayerPosition.PT),
                                manager);

                Player lockedDefender = createOwnedPlayer(
                                4002L,
                                "4002",
                                "Defensa bloqueado",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defender2 = createOwnedPlayer(
                                4003L,
                                "4003",
                                "Defensa 2",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defender3 = createOwnedPlayer(
                                4004L,
                                "4004",
                                "Defensa 3",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defender4 = createOwnedPlayer(
                                4005L,
                                "4005",
                                "Defensa 4",
                                List.of(PlayerPosition.DF),
                                manager);

                Player midfielder1 = createOwnedPlayer(
                                4006L,
                                "4006",
                                "Medio 1",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielder2 = createOwnedPlayer(
                                4007L,
                                "4007",
                                "Medio 2",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielder3 = createOwnedPlayer(
                                4008L,
                                "4008",
                                "Medio 3",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielder4 = createOwnedPlayer(
                                4009L,
                                "4009",
                                "Medio 4",
                                List.of(PlayerPosition.MC),
                                manager);

                Player forward1 = createOwnedPlayer(
                                4010L,
                                "4010",
                                "Delantero 1",
                                List.of(PlayerPosition.DL),
                                manager);

                Player forward2 = createOwnedPlayer(
                                4011L,
                                "4011",
                                "Delantero 2",
                                List.of(PlayerPosition.DL),
                                manager);

                Player betterDefender = createOwnedPlayer(
                                4012L,
                                "4012",
                                "Defensa mejor",
                                List.of(PlayerPosition.DF),
                                manager);

                List<Player> currentStarters = List.of(
                                goalkeeper,
                                lockedDefender,
                                defender2,
                                defender3,
                                defender4,
                                midfielder1,
                                midfielder2,
                                midfielder3,
                                midfielder4,
                                forward1,
                                forward2);

                for (Player player : currentStarters) {

                        ReflectionTestUtils.setField(
                                        player,
                                        "starter",
                                        true);
                }

                for (Player player : currentStarters) {

                        ReflectionTestUtils.setField(
                                        player,
                                        "teamId",
                                        30L);
                }

                ReflectionTestUtils.setField(
                                lockedDefender,
                                "teamId",
                                10L);

                ReflectionTestUtils.setField(
                                betterDefender,
                                "teamId",
                                20L);

                List<Player> squad = List.of(
                                goalkeeper,
                                lockedDefender,
                                defender2,
                                defender3,
                                defender4,
                                midfielder1,
                                midfielder2,
                                midfielder3,
                                midfielder4,
                                forward1,
                                forward2,
                                betterDefender);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                when(matchdayChangeEligibilityService
                                .resolveModifiableByTeam(LEAGUE_ID))
                                .thenReturn(Map.of(
                                                10L, false,
                                                20L, true,
                                                30L, true));

                for (Player player : squad) {

                        int points;

                        if (player.getId().equals(4002L)) {

                                /*
                                 * Titular bloqueado:
                                 * sería claramente el defensa a sustituir
                                 * si Biwenger todavía permitiera modificarlo.
                                 */
                                points = 1;

                        } else if (player.getId().equals(4012L)) {

                                /*
                                 * Suplente mejor que el bloqueado,
                                 * pero peor que los otros tres defensas.
                                 */
                                points = 10;

                        } else if (player.getId().equals(4003L)
                                        || player.getId().equals(4004L)
                                        || player.getId().equals(4005L)) {

                                points = 20;

                        } else {

                                points = 5;
                        }

                        mockPerformanceReports(
                                        player,
                                        points);
                }

                RecommendedLineupResponse result = recommendationService
                                .getRecommendedLineup(
                                                LEAGUE_ID);

                boolean lockedStarterStillPresent = result.recommendedStarters()
                                .stream()
                                .anyMatch(player -> player.playerId()
                                                .equals(
                                                                lockedDefender.getId()));

                assertTrue(lockedStarterStillPresent);
        }

        @Test
        void recommendedLineupShouldNotAddLockedNonStarter() {

                Manager manager = createManager();

                ReflectionTestUtils.setField(
                                manager,
                                "currentFormation",
                                "4-4-2");

                Player goalkeeper = createOwnedPlayer(
                                4101L,
                                "4101",
                                "Portero",
                                List.of(PlayerPosition.PT),
                                manager);

                Player defender1 = createOwnedPlayer(
                                4102L,
                                "4102",
                                "Defensa 1",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defender2 = createOwnedPlayer(
                                4103L,
                                "4103",
                                "Defensa 2",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defender3 = createOwnedPlayer(
                                4104L,
                                "4104",
                                "Defensa 3",
                                List.of(PlayerPosition.DF),
                                manager);

                Player defender4 = createOwnedPlayer(
                                4105L,
                                "4105",
                                "Defensa 4",
                                List.of(PlayerPosition.DF),
                                manager);

                Player midfielder1 = createOwnedPlayer(
                                4106L,
                                "4106",
                                "Medio 1",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielder2 = createOwnedPlayer(
                                4107L,
                                "4107",
                                "Medio 2",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielder3 = createOwnedPlayer(
                                4108L,
                                "4108",
                                "Medio 3",
                                List.of(PlayerPosition.MC),
                                manager);

                Player midfielder4 = createOwnedPlayer(
                                4109L,
                                "4109",
                                "Medio 4",
                                List.of(PlayerPosition.MC),
                                manager);

                Player forward1 = createOwnedPlayer(
                                4110L,
                                "4110",
                                "Delantero 1",
                                List.of(PlayerPosition.DL),
                                manager);

                Player forward2 = createOwnedPlayer(
                                4111L,
                                "4111",
                                "Delantero 2",
                                List.of(PlayerPosition.DL),
                                manager);

                Player lockedBetterForward = createOwnedPlayer(
                                4112L,
                                "4112",
                                "Delantero bloqueado mejor",
                                List.of(PlayerPosition.DL),
                                manager);

                List<Player> currentStarters = List.of(
                                goalkeeper,
                                defender1,
                                defender2,
                                defender3,
                                defender4,
                                midfielder1,
                                midfielder2,
                                midfielder3,
                                midfielder4,
                                forward1,
                                forward2);

                for (Player player : currentStarters) {

                        ReflectionTestUtils.setField(
                                        player,
                                        "starter",
                                        true);
                }

                for (Player player : currentStarters) {

                        ReflectionTestUtils.setField(
                                        player,
                                        "teamId",
                                        30L);
                }

                ReflectionTestUtils.setField(
                                lockedBetterForward,
                                "teamId",
                                20L);

                List<Player> squad = List.of(
                                goalkeeper,
                                defender1,
                                defender2,
                                defender3,
                                defender4,
                                midfielder1,
                                midfielder2,
                                midfielder3,
                                midfielder4,
                                forward1,
                                forward2,
                                lockedBetterForward);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(squad);

                when(matchdayChangeEligibilityService
                                .resolveModifiableByTeam(LEAGUE_ID))
                                .thenReturn(Map.of(
                                                20L, false,
                                                30L, true));

                for (Player player : currentStarters) {

                        mockPerformanceReports(
                                        player,
                                        5);
                }

                RecommendedLineupResponse result = recommendationService
                                .getRecommendedLineup(
                                                LEAGUE_ID);

                boolean lockedForwardPresent = result.recommendedStarters()
                                .stream()
                                .anyMatch(player -> player.playerId()
                                                .equals(
                                                                lockedBetterForward.getId()));

                assertFalse(lockedForwardPresent);
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
                                .thenReturn(createNeutralMarketSquad());

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

                return createPlayer(
                                id,
                                biwengerPlayerId,
                                name,
                                positions,
                                marketValue,
                                valueFluctuation,
                                injured
                                                ? PlayerStatus.INJURED
                                                : PlayerStatus.OK);
        }

        private Player createPlayer(
                        Long id,
                        String biwengerPlayerId,
                        String name,
                        List<PlayerPosition> positions,
                        Long marketValue,
                        Long valueFluctuation,
                        PlayerStatus status) {

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
                                "status",
                                status);
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

        private List<Player> createNeutralMarketSquad() {
                List<Player> squad = new java.util.ArrayList<>(
                                createCompleteSquad());

                Manager manager = squad.get(0).getOwner();

                squad.add(
                                createOwnedPlayer(
                                                1017L,
                                                "1017",
                                                "MC DL neutral",
                                                List.of(
                                                                PlayerPosition.MC,
                                                                PlayerPosition.DL),
                                                manager));

                return squad;
        }
}