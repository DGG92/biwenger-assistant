package com.artajerjes.biwengerassistant.recommendation;

import java.time.LocalDateTime;
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

import static org.mockito.ArgumentMatchers.anyLong;
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
import com.artajerjes.biwengerassistant.offer.OfferService;
import com.artajerjes.biwengerassistant.offer.dto.EconomicStatusResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationReason;
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

        @Mock
        private PlayerMatchReportRepository playerMatchReportRepository;

        @InjectMocks
        private RecommendationService recommendationService;

        @BeforeEach
        void setUp() {
                lenient()
                                .when(
                                                playerMatchReportRepository
                                                                .findTop2ByPlayer_IdOrderByMatchDateDesc(anyLong()))
                                .thenReturn(List.of());
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
                                                .findTop2ByPlayer_IdOrderByMatchDateDesc(70L))
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
                                                .findTop2ByPlayer_IdOrderByMatchDateDesc(71L))
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
                                                .findTop2ByPlayer_IdOrderByMatchDateDesc(72L))
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
                                                .findTop2ByPlayer_IdOrderByMatchDateDesc(73L))
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
                                                .findTop2ByPlayer_IdOrderByMatchDateDesc(63L))
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
                                                .findTop2ByPlayer_IdOrderByMatchDateDesc(64L))
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