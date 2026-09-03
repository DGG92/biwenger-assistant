package com.artajerjes.biwengerassistant.recommendation.action;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.market.MarketListingType;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerProtectionService;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlert;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlertLevel;
import com.artajerjes.biwengerassistant.recommendation.RecommendationService;
import com.artajerjes.biwengerassistant.recommendation.RecommendationType;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationReason;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupChangeResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupPlayerResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignalService;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignals;

@ExtendWith(MockitoExtension.class)
class ActionRecommendationServiceTest {

        private static final Long LEAGUE_ID = 1L;
        private static final Long BIWENGER_USER_ID = 11_467_137L;

        @Mock
        private LeagueRepository leagueRepository;

        @Mock
        private PlayerRepository playerRepository;

        @Mock
        private RecommendationService recommendationService;

        @Mock
        private PlayerPerformanceSignalService playerPerformanceSignalService;

        @Mock
        private Manager manager;

        @Mock
        private PlayerProtectionService playerProtectionService;

        private ActionRecommendationService actionRecommendationService;

        @BeforeEach
        void setUp() {

                lenient()
                                .when(manager.getBiwengerManagerId())
                                .thenReturn(BIWENGER_USER_ID);

                actionRecommendationService = new ActionRecommendationService(
                                leagueRepository,
                                playerRepository,
                                recommendationService,
                                playerPerformanceSignalService,
                                playerProtectionService);

                ReflectionTestUtils.setField(
                                actionRecommendationService,
                                "biwengerUserId",
                                BIWENGER_USER_ID);

                lenient()
                                .when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);
        }

        @Test
        void shouldHoldProfitableRisingStarterWithStrongPerformance() {

                Player player = createOwnedPlayer(
                                480L,
                                "2184",
                                "Ryan",
                                PlayerPosition.PT,
                                4_110_000L,
                                3_270_000L,
                                40_000L,
                                PlayerStatus.OK,
                                true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(player));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 25,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                9.0,
                                                                2,
                                                                true,
                                                                5.5,
                                                                10));

                when(playerProtectionService.calculate(player))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.NONE,
                                                                0,
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(LEAGUE_ID);

                assertEquals(1, result.size());

                ActionCandidate action = result.get(0);

                assertEquals(
                                ActionType.HOLD,
                                action.type());

                assertEquals(
                                ActionPriority.HIGH,
                                action.priority());

                assertEquals(
                                "Ryan",
                                action.playerName());

                assertTrue(
                                action.sourceSignals()
                                                .contains("HIGH_PROFIT"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("VALUE_RISING"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("RECENT_FORM_EXCELLENT"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("STARTER"));

                /*
                 * Regla especialmente importante:
                 *
                 * tener una plusvalía elevada no debe convertir
                 * automáticamente al jugador en candidato a venta.
                 */
                assertFalse(
                                action.sourceSignals()
                                                .contains("PROFIT_CAN_BE_REALIZED"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "una plusvalía de 840000 €"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "su valor está subiendo 40000 €"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "9.0 puntos de media reciente"));

                assertFalse(
                                action.explanation()
                                                .contains(
                                                                "beneficio/pérdida"));

                assertFalse(
                                action.explanation()
                                                .contains(
                                                                "€ diarios"));
        }

        @Test
        void shouldSellPlayerWithFallingValuePoorPerformanceAndCoveredPosition() {

                Player player = createOwnedPlayer(
                                500L,
                                "5000",
                                "Jugador en caída",
                                PlayerPosition.MC,
                                3_000_000L,
                                2_000_000L,
                                -60_000L,
                                PlayerStatus.OK,
                                false);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(player));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                1.5,
                                                                3,
                                                                false,
                                                                1.8,
                                                                8));

                when(playerProtectionService.calculate(player))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.NONE,
                                                                0,
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(LEAGUE_ID);

                assertEquals(1, result.size());

                ActionCandidate action = result.get(0);

                assertEquals(
                                ActionType.SELL,
                                action.type());

                assertEquals(
                                ActionPriority.HIGH,
                                action.priority());

                assertTrue(
                                action.sourceSignals()
                                                .contains("VALUE_FALLING_FAST"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("RECENT_FORM_POOR"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("HISTORICAL_PERFORMANCE_POOR"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("POSITION_WELL_COVERED"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("HIGH_PROFIT"));

                /*
                 * Aquí sí tiene sentido realizar el beneficio:
                 * no porque exista plusvalía, sino porque ya hay
                 * suficientes señales negativas independientes.
                 */
                assertTrue(
                                action.sourceSignals()
                                                .contains("PROFIT_CAN_BE_REALIZED"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "una plusvalía de 1000000 €"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "su valor está bajando 60000 €"));

                assertFalse(
                                action.explanation()
                                                .contains(
                                                                "beneficio/pérdida"));

                assertFalse(
                                action.explanation()
                                                .contains(
                                                                "€ al día"));
        }

        @Test
        void shouldHoldCoachWithStrongPerformanceUsingCoachScoringScale() {

                Player coach = createOwnedPlayer(
                                550L,
                                "5500",
                                "Entrenador en buena forma",
                                PlayerPosition.E,
                                1_000_000L,
                                1_000_000L,
                                0L,
                                PlayerStatus.OK,
                                false);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(coach));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(coach))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                2.1,
                                                                3,
                                                                false,
                                                                1.9,
                                                                8));

                when(playerProtectionService.calculate(coach))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.NONE,
                                                                0,
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(LEAGUE_ID);

                assertEquals(1, result.size());

                ActionCandidate action = result.get(0);

                assertEquals(
                                ActionType.HOLD,
                                action.type());

                assertTrue(
                                action.sourceSignals()
                                                .contains("RECENT_FORM_EXCELLENT"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("HISTORICAL_PERFORMANCE_STRONG"));

                assertFalse(
                                action.sourceSignals()
                                                .contains("POSITION_WELL_COVERED"));
        }

        @Test
        void shouldSellCoachWithPoorPerformanceUsingCoachScoringScale() {

                Player coach = createOwnedPlayer(
                                551L,
                                "5501",
                                "Entrenador en mala forma",
                                PlayerPosition.E,
                                1_000_000L,
                                1_000_000L,
                                0L,
                                PlayerStatus.OK,
                                false);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(coach));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(coach))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                0.5,
                                                                3,
                                                                false,
                                                                0.6,
                                                                8));

                when(playerProtectionService.calculate(coach))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.NONE,
                                                                0,
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(LEAGUE_ID);

                assertEquals(1, result.size());

                ActionCandidate action = result.get(0);

                assertEquals(
                                ActionType.SELL,
                                action.type());

                assertTrue(
                                action.sourceSignals()
                                                .contains("RECENT_FORM_POOR"));

                assertTrue(
                                action.sourceSignals()
                                                .contains("HISTORICAL_PERFORMANCE_POOR"));

                assertFalse(
                                action.sourceSignals()
                                                .contains("POSITION_WELL_COVERED"));
        }

        @Test
        void shouldWatchPlayerWhenSignalsAreStillInsufficient() {

                Player player = createOwnedPlayer(
                                600L,
                                "6000",
                                "Jugador sin datos suficientes",
                                PlayerPosition.DF,
                                1_000_000L,
                                1_000_000L,
                                0L,
                                PlayerStatus.OK,
                                false);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(player));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 25,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                0,
                                                                1,
                                                                false,
                                                                0,
                                                                0));

                when(playerProtectionService.calculate(player))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.NONE,
                                                                0,
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(LEAGUE_ID);

                assertEquals(1, result.size());

                ActionCandidate action = result.get(0);

                assertEquals(
                                ActionType.WATCH,
                                action.type());

                assertEquals(
                                ActionPriority.LOW,
                                action.priority());

                assertTrue(
                                action.sourceSignals()
                                                .contains(
                                                                "RECENT_FORM_INSUFFICIENT_DATA"));

                assertFalse(
                                action.sourceSignals()
                                                .contains("VALUE_RISING"));

                assertFalse(
                                action.sourceSignals()
                                                .contains("VALUE_FALLING"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "todavía hay pocos datos recientes"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "todavía hay pocos datos recientes"));

                assertFalse(
                                action.explanation()
                                                .contains(
                                                                "todavía no justifican una decisión clara"));

                assertEquals(
                                "Conviene seguir su evolución: todavía hay pocos datos recientes.",
                                action.explanation());

                assertFalse(
                                action.explanation()
                                                .contains(
                                                                "todavía no justifican una decisión clara"));
        }

        @Test
        void shouldReturnHoldAndProtectForValuablePlayer() {

                Player player = createOwnedPlayer(
                                700L,
                                "7000",
                                "Jugador protegido",
                                PlayerPosition.PT,
                                4_000_000L,
                                3_000_000L,
                                80_000L,
                                PlayerStatus.OK,
                                true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(player));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 50,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                8.0,
                                                                3,
                                                                true,
                                                                6.0,
                                                                10));

                when(playerProtectionService.calculate(player))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.PROTECT,
                                                                85,
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(LEAGUE_ID);

                assertEquals(2, result.size());

                assertTrue(
                                result.stream()
                                                .anyMatch(action -> action.type() == ActionType.HOLD));

                assertTrue(
                                result.stream()
                                                .anyMatch(action -> action.type() == ActionType.PROTECT));

                assertFalse(
                                result.stream()
                                                .anyMatch(action -> action.type() == ActionType.WATCH));
        }

        @Test
        void shouldNotProtectPlayerWhenSellIsRecommended() {

                Player player = createOwnedPlayer(
                                800L,
                                "8000",
                                "Jugador vendible",
                                PlayerPosition.MC,
                                3_000_000L,
                                2_000_000L,
                                -100_000L,
                                PlayerStatus.OK,
                                false);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of(player));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                1.0,
                                                                3,
                                                                false,
                                                                1.5,
                                                                10));

                /*
                 * Forzamos deliberadamente una alerta PROTECT
                 * para comprobar que SELL tiene prioridad.
                 */
                when(playerProtectionService.calculate(player))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.PROTECT,
                                                                90,
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(LEAGUE_ID);

                assertEquals(1, result.size());

                assertEquals(
                                ActionType.SELL,
                                result.get(0).type());

                assertFalse(
                                result.stream()
                                                .anyMatch(action -> action.type() == ActionType.PROTECT));
        }

        @Test
        void shouldCreateStarterReplacementFromRecommendedLineup() {

                Player starter = createOwnedPlayer(
                                900L,
                                "9000",
                                "Titular flojo",
                                PlayerPosition.MC,
                                3_000_000L,
                                2_500_000L,
                                0L,
                                PlayerStatus.OK,
                                true);

                Player reserve = createOwnedPlayer(
                                901L,
                                "9001",
                                "Suplente recomendado",
                                PlayerPosition.MC,
                                2_000_000L,
                                1_500_000L,
                                0L,
                                PlayerStatus.OK,
                                false);

                ReflectionTestUtils.setField(
                                starter,
                                "lineupPosition",
                                PlayerPosition.MC);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(
                                                List.of(
                                                                starter,
                                                                reserve));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(starter))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                4.0,
                                                                3,
                                                                false,
                                                                4.0,
                                                                10));

                when(playerPerformanceSignalService.analyze(reserve))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                7.0,
                                                                3,
                                                                true,
                                                                6.0,
                                                                10));

                when(playerProtectionService.calculate(starter))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.NONE,
                                                                0,
                                                                List.of()));

                when(playerProtectionService.calculate(reserve))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.NONE,
                                                                0,
                                                                List.of()));

                when(recommendationService.getRecommendedLineup(LEAGUE_ID))
                                .thenReturn(
                                                new RecommendedLineupResponse(
                                                                "5-4-1",
                                                                "5-4-1",
                                                                40.0,
                                                                44.5,
                                                                4.5,
                                                                82,
                                                                List.of(
                                                                                new RecommendedLineupPlayerResponse(
                                                                                                reserve.getId(),
                                                                                                reserve.getName(),
                                                                                                "MC",
                                                                                                7.0)),
                                                                List.of(
                                                                                new RecommendedLineupChangeResponse(
                                                                                                "OUT",
                                                                                                starter.getId(),
                                                                                                starter.getName()),
                                                                                new RecommendedLineupChangeResponse(
                                                                                                "IN",
                                                                                                reserve.getId(),
                                                                                                reserve.getName()))));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(
                                                LEAGUE_ID);

                ActionCandidate replacement = result.stream()
                                .filter(action -> action.type() == ActionType.REPLACE_STARTER)
                                .findFirst()
                                .orElseThrow();

                assertEquals(
                                starter.getId(),
                                replacement.playerId());

                assertEquals(
                                ActionPriority.HIGH,
                                replacement.priority());

                assertEquals(
                                82,
                                replacement.confidence());

                assertTrue(
                                replacement.title()
                                                .contains(
                                                                "Suplente recomendado"));

                assertTrue(
                                replacement.sourceSignals()
                                                .contains(
                                                                "RECOMMENDED_LINEUP_CHANGE"));

                assertTrue(
                                replacement.sourceSignals()
                                                .contains(
                                                                "IN_PLAYER_901"));
        }

        @Test
        void shouldNotCreateStarterReplacementWhenRecommendedLineupHasNoChanges() {

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(recommendationService.getRecommendedLineup(LEAGUE_ID))
                                .thenReturn(
                                                new RecommendedLineupResponse(
                                                                "5-4-1",
                                                                "5-4-1",
                                                                38.27,
                                                                38.27,
                                                                0,
                                                                0,
                                                                List.of(),
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(
                                                LEAGUE_ID);

                assertFalse(
                                result.stream()
                                                .anyMatch(action -> action.type() == ActionType.REPLACE_STARTER));

                assertFalse(
                                result.stream()
                                                .anyMatch(action -> action.type() == ActionType.CHANGE_FORMATION));
        }

        @Test
        void shouldRecommendFormationChangeFromRecommendedLineup() {

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(recommendationService.getRecommendedLineup(LEAGUE_ID))
                                .thenReturn(
                                                new RecommendedLineupResponse(
                                                                "5-4-1",
                                                                "4-4-2",
                                                                55.0,
                                                                58.5,
                                                                3.5,
                                                                73,
                                                                List.of(),
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(
                                                LEAGUE_ID);

                assertEquals(
                                1,
                                result.size());

                ActionCandidate action = result.get(0);

                assertEquals(
                                ActionType.CHANGE_FORMATION,
                                action.type());

                assertEquals(
                                ActionPriority.MEDIUM,
                                action.priority());

                assertEquals(
                                73,
                                action.confidence());

                assertTrue(
                                action.title()
                                                .contains("4-4-2"));

                assertTrue(
                                action.sourceSignals()
                                                .contains(
                                                                "FORMATION_IMPROVEMENT"));

                assertTrue(
                                action.sourceSignals()
                                                .contains(
                                                                "RECOMMENDED_LINEUP"));
        }

        @Test
        void shouldNotRecommendFormationChangeWhenLineupImprovementIsMarginal() {

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(List.of());

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 0,
                                                                                "DL", 0)));

                when(recommendationService.getRecommendedLineup(LEAGUE_ID))
                                .thenReturn(
                                                new RecommendedLineupResponse(
                                                                "5-4-1",
                                                                "4-4-2",
                                                                55.0,
                                                                56.2,
                                                                1.2,
                                                                61,
                                                                List.of(),
                                                                List.of()));

                List<ActionCandidate> result = actionRecommendationService
                                .getSquadActions(
                                                LEAGUE_ID);

                assertTrue(
                                result.isEmpty());
        }

        @Test
        void shouldCreateHighPriorityBidForStrongBuyAuction() {

                MarketRecommendationResponse recommendation = new MarketRecommendationResponse(
                                515L,
                                "515",
                                "Tete Morente",
                                "Elche",
                                List.of(PlayerPosition.MC),
                                MarketListingType.AUCTION,
                                350_000L,
                                350_000L,
                                360_000L,
                                385_000L,
                                -10_000L,
                                -2.86,
                                10_000L,
                                null,
                                null,
                                null,
                                null,
                                0,
                                PlayerStatus.OK,
                                true,
                                81,
                                RecommendationType.STRONG_BUY,
                                null,
                                null,
                                List.of(),
                                null);

                when(
                                recommendationService
                                                .getMarketRecommendations(1L))
                                .thenReturn(
                                                List.of(recommendation));

                List<ActionCandidate> actions = actionRecommendationService
                                .getMarketActions(1L);

                assertEquals(1, actions.size());

                ActionCandidate action = actions.get(0);

                assertEquals(
                                ActionType.BID,
                                action.type());

                assertEquals(
                                ActionPriority.HIGH,
                                action.priority());

                assertEquals(
                                515L,
                                action.playerId());

                assertEquals(
                                "Tete Morente",
                                action.playerName());

                assertEquals(
                                385_000L,
                                action.suggestedAmount());

                assertEquals(
                                81,
                                action.confidence());

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "puja máxima recomendada"));
        }

        @Test
        void shouldCreateMediumPriorityBuyForBuySale() {

                MarketRecommendationResponse recommendation = new MarketRecommendationResponse(
                                485L,
                                "485",
                                "Sazonov",
                                "Torino",
                                List.of(PlayerPosition.DF),
                                MarketListingType.SALE,
                                360_000L,
                                340_000L,
                                null,
                                null,
                                20_000L,
                                5.56,
                                10_000L,
                                null,
                                null,
                                null,
                                null,
                                0,
                                PlayerStatus.OK,
                                true,
                                75,
                                RecommendationType.BUY,
                                null,
                                null,
                                List.of(
                                                MarketRecommendationReason.PRICE_BELOW_MARKET,
                                                MarketRecommendationReason.VALUE_RISING_FAST),
                                null);

                when(
                                recommendationService
                                                .getMarketRecommendations(1L))
                                .thenReturn(
                                                List.of(recommendation));

                List<ActionCandidate> actions = actionRecommendationService
                                .getMarketActions(1L);

                assertEquals(1, actions.size());

                ActionCandidate action = actions.get(0);

                assertEquals(
                                ActionType.BUY,
                                action.type());

                assertEquals(
                                ActionPriority.MEDIUM,
                                action.priority());

                assertEquals(
                                485L,
                                action.playerId());

                assertEquals(
                                "Sazonov",
                                action.playerName());

                assertEquals(
                                340_000L,
                                action.suggestedAmount());

                assertEquals(
                                75,
                                action.confidence());

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "su precio está por debajo del valor de mercado"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "su valor está subiendo con fuerza"));

                assertTrue(
                                action.explanation()
                                                .contains(
                                                                "un precio de 340000 €"));
        }

        @Test
        void shouldCombineSquadAndMarketActionsOrderedByPriorityAndConfidence() {

                Player squadPlayer = createOwnedPlayer(
                                700L,
                                "7000",
                                "Jugador plantilla",
                                PlayerPosition.MC,
                                2_000_000L,
                                1_500_000L,
                                30_000L,
                                PlayerStatus.OK,
                                false);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(
                                                List.of(squadPlayer));

                when(recommendationService.getSquadNeeds(LEAGUE_ID))
                                .thenReturn(
                                                createSquadNeeds(
                                                                Map.of(
                                                                                "PT", 0,
                                                                                "DF", 0,
                                                                                "MC", 25,
                                                                                "DL", 0)));

                when(playerPerformanceSignalService.analyze(squadPlayer))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                5.5,
                                                                2,
                                                                false,
                                                                0,
                                                                0));

                when(playerProtectionService.calculate(squadPlayer))
                                .thenReturn(
                                                new PlayerProtectionAlert(
                                                                PlayerProtectionAlertLevel.NONE,
                                                                0,
                                                                List.of()));

                MarketRecommendationResponse bidRecommendation = new MarketRecommendationResponse(
                                515L,
                                "515",
                                "Tete Morente",
                                "Elche",
                                List.of(PlayerPosition.MC),
                                MarketListingType.AUCTION,
                                350_000L,
                                350_000L,
                                360_000L,
                                385_000L,
                                -10_000L,
                                -2.86,
                                10_000L,
                                null,
                                null,
                                null,
                                null,
                                0,
                                PlayerStatus.OK,
                                true,
                                81,
                                RecommendationType.STRONG_BUY,
                                null,
                                null,
                                List.of(),
                                null);

                MarketRecommendationResponse buyRecommendation = new MarketRecommendationResponse(
                                485L,
                                "485",
                                "Sazonov",
                                "Torino",
                                List.of(PlayerPosition.DF),
                                MarketListingType.SALE,
                                360_000L,
                                340_000L,
                                null,
                                null,
                                20_000L,
                                5.56,
                                10_000L,
                                null,
                                null,
                                null,
                                null,
                                0,
                                PlayerStatus.OK,
                                true,
                                75,
                                RecommendationType.BUY,
                                null,
                                null,
                                List.of(),
                                null);

                when(recommendationService
                                .getMarketRecommendations(LEAGUE_ID))
                                .thenReturn(
                                                List.of(
                                                                buyRecommendation,
                                                                bidRecommendation));

                List<ActionCandidate> result = actionRecommendationService
                                .getAllActions(LEAGUE_ID);

                assertEquals(
                                3,
                                result.size());

                assertEquals(
                                ActionType.BID,
                                result.get(0).type());

                assertEquals(
                                ActionPriority.HIGH,
                                result.get(0).priority());

                assertEquals(
                                81,
                                result.get(0).confidence());

                assertEquals(
                                ActionType.BUY,
                                result.get(1).type());

                assertEquals(
                                ActionPriority.MEDIUM,
                                result.get(1).priority());

                assertEquals(
                                75,
                                result.get(1).confidence());

                assertEquals(
                                ActionType.HOLD,
                                result.get(2).type());

                assertEquals(
                                ActionPriority.MEDIUM,
                                result.get(2).priority());

                assertTrue(
                                result.get(1).confidence() > result.get(2).confidence());
        }

        private Player createOwnedPlayer(
                        Long id,
                        String biwengerPlayerId,
                        String name,
                        PlayerPosition position,
                        Long marketValue,
                        Long purchasePrice,
                        Long valueFluctuation,
                        PlayerStatus status,
                        boolean starter) {

                Player player = new Player(
                                biwengerPlayerId,
                                name,
                                List.of(position),
                                "Equipo",
                                marketValue,
                                createLeague());

                ReflectionTestUtils.setField(
                                player,
                                "id",
                                id);

                ReflectionTestUtils.setField(
                                player,
                                "owner",
                                manager);

                ReflectionTestUtils.setField(
                                player,
                                "purchasePrice",
                                purchasePrice);

                ReflectionTestUtils.setField(
                                player,
                                "valueFluctuation",
                                valueFluctuation);

                ReflectionTestUtils.setField(
                                player,
                                "status",
                                status);

                ReflectionTestUtils.setField(
                                player,
                                "starter",
                                starter);

                return player;
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

        private SquadNeedsResponse createSquadNeeds(
                        Map<String, Integer> needScoreByPosition) {

                return new SquadNeedsResponse(
                                13L,
                                "Califato Omeya",
                                1,
                                Map.of(
                                                "PT", 0,
                                                "DF", 0,
                                                "MC", 0,
                                                "DL", 0),
                                Map.of(
                                                "PT", 0,
                                                "DF", 0,
                                                "MC", 0,
                                                "DL", 0),
                                Map.of(
                                                "PT", 0,
                                                "DF", 0,
                                                "MC", 0,
                                                "DL", 0),
                                needScoreByPosition);
        }
}