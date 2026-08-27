package com.artajerjes.biwengerassistant.recommendation.action;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.recommendation.RecommendationService;
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

    private ActionRecommendationService actionRecommendationService;

    @BeforeEach
    void setUp() {

        when(manager.getBiwengerManagerId())
                .thenReturn(BIWENGER_USER_ID);

        actionRecommendationService = new ActionRecommendationService(
                leagueRepository,
                playerRepository,
                recommendationService,
                playerPerformanceSignalService);

        ReflectionTestUtils.setField(
                actionRecommendationService,
                "biwengerUserId",
                BIWENGER_USER_ID);

        when(leagueRepository.existsById(LEAGUE_ID))
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