package com.artajerjes.biwengerassistant.playerreport;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.playerreport.dto.PlayerReportSyncResponse;

@ExtendWith(MockitoExtension.class)
class PlayerMatchReportSyncServiceTest {

    private static final Long LEAGUE_ID = 1L;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private BiwengerClient biwengerClient;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerMatchReportPersistenceService persistenceService;

    private PlayerMatchReportService service;

    @BeforeEach
    void setUp() {

        service = new PlayerMatchReportService(
                biwengerClient,
                playerRepository,
                new CustomScoreEvaluator(),
                persistenceService);

        ReflectionTestUtils.setField(
                service,
                "reportsSyncBatchSize",
                25);

        when(
                biwengerClient
                        .getLeague()
                        .data()
                        .scoreID())
                .thenReturn(1);
    }

    @Test
    void leagueSyncShouldPrioritizePlayersWithOldestOrMissingReports() {

        Player noReports = createPlayer(
                1L,
                "no-reports");

        Player oldReports = createPlayer(
                2L,
                "old-reports");

        Player recentReports = createPlayer(
                3L,
                "recent-reports");

        /*
         * El repository devuelve los jugadores en un orden
         * deliberadamente incorrecto.
         */
        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(
                        List.of(
                                recentReports,
                                noReports,
                                oldReports));

        ReflectionTestUtils.setField(
                oldReports,
                "reportsLastSyncAttemptAt",
                LocalDateTime.of(
                        2026,
                        8,
                        1,
                        20,
                        0));

        ReflectionTestUtils.setField(
                recentReports,
                "reportsLastSyncAttemptAt",
                LocalDateTime.of(
                        2026,
                        8,
                        25,
                        20,
                        0));

        /*
         * null es suficiente:
         * el jugador cuenta como procesado,
         * pero no hay reports que persistir.
         */
        when(biwengerClient.getPlayerDetail("no-reports"))
                .thenReturn(null);

        when(biwengerClient.getPlayerDetail("old-reports"))
                .thenReturn(null);

        when(biwengerClient.getPlayerDetail("recent-reports"))
                .thenReturn(null);

        PlayerReportSyncResponse result = service.syncLeagueReports(
                LEAGUE_ID);

        InOrder order = inOrder(biwengerClient);

        order.verify(biwengerClient)
                .getPlayerDetail("no-reports");

        order.verify(biwengerClient)
                .getPlayerDetail("old-reports");

        order.verify(biwengerClient)
                .getPlayerDetail("recent-reports");

        assertEquals(
                3,
                result.playersCompleted());

        assertEquals(
                true,
                result.completed());
    }

    @Test
    void leagueSyncShouldStopImmediatelyWhenBiwengerReturns429() {

        Player first = createPlayer(
                10L,
                "first");

        Player rateLimited = createPlayer(
                20L,
                "rate-limited");

        Player neverAttempted = createPlayer(
                30L,
                "never-attempted");

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(
                        List.of(
                                first,
                                rateLimited,
                                neverAttempted));

        when(biwengerClient.getPlayerDetail("first"))
                .thenReturn(null);

        when(biwengerClient.getPlayerDetail("rate-limited"))
                .thenThrow(
                        HttpClientErrorException.create(
                                HttpStatus.TOO_MANY_REQUESTS,
                                "Too Many Requests",
                                HttpHeaders.EMPTY,
                                new byte[0],
                                StandardCharsets.UTF_8));

        PlayerReportSyncResponse result = service.syncLeagueReports(
                LEAGUE_ID);

        assertFalse(
                result.completed());

        assertEquals(
                "RATE_LIMIT",
                result.stopReason());

        assertEquals(
                3,
                result.playersTotal());

        assertEquals(
                3,
                result.playersEligible());

        assertEquals(
                2,
                result.playersAttempted());

        assertEquals(
                1,
                result.playersCompleted());

        assertEquals(
                10L,
                result.lastCompletedPlayerId());

        assertEquals(
                20L,
                result.rateLimitedPlayerId());

        InOrder order = inOrder(biwengerClient);

        order.verify(biwengerClient)
                .getPlayerDetail("first");

        order.verify(biwengerClient)
                .getPlayerDetail("rate-limited");

        verify(
                biwengerClient,
                never())
                .getPlayerDetail(
                        "never-attempted");

        /*
         * Si intentara "never-attempted",
         * el InOrder/verifyNoMoreInteractions posterior
         * nos descubriría el problema.
         */
    }

    @Test
    void leagueSyncShouldRespectConfiguredBatchSize() {

        Player first = createPlayer(
                10L,
                "first");

        Player second = createPlayer(
                20L,
                "second");

        Player third = createPlayer(
                30L,
                "third");

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(
                        List.of(
                                first,
                                second,
                                third));

        when(biwengerClient.getPlayerDetail("first"))
                .thenReturn(null);

        when(biwengerClient.getPlayerDetail("second"))
                .thenReturn(null);

        /*
         * Para este test reducimos el lote a 2.
         */
        ReflectionTestUtils.setField(
                service,
                "reportsSyncBatchSize",
                2);

        PlayerReportSyncResponse result = service.syncLeagueReports(
                LEAGUE_ID);

        assertEquals(
                3,
                result.playersTotal());

        assertEquals(
                3,
                result.playersEligible());

        assertEquals(
                2,
                result.playersAttempted());

        assertEquals(
                2,
                result.playersCompleted());

        assertEquals(
                true,
                result.completed());

        verify(
                biwengerClient)
                .getPlayerDetail("first");

        verify(
                biwengerClient)
                .getPlayerDetail("second");

        verify(
                biwengerClient,
                never())
                .getPlayerDetail("third");
    }

    private Player createPlayer(
            Long id,
            String slug) {

        Player player = new Player(
                String.valueOf(id),
                "Jugador " + id,
                List.of(PlayerPosition.MC),
                "Equipo",
                1_000_000L,
                null);

        ReflectionTestUtils.setField(
                player,
                "id",
                id);

        ReflectionTestUtils.setField(
                player,
                "slug",
                slug);

        return player;
    }
}