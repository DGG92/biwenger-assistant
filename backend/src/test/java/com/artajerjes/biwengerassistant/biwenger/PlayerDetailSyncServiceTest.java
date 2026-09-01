package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.sync.PlayerDetailSyncResponse;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryService;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportService;
import com.artajerjes.biwengerassistant.playerreport.PlayerReportScoreConfig;

class PlayerDetailSyncServiceTest {

    private BiwengerClient biwengerClient;
    private PlayerRepository playerRepository;
    private PlayerPriceHistoryRepository playerPriceHistoryRepository;
    private PlayerPriceHistoryService playerPriceHistoryService;
    private PlayerMatchReportService playerMatchReportService;

    private PlayerDetailSyncService service;

    @BeforeEach
    void setUp() {

        biwengerClient = mock(BiwengerClient.class);
        playerRepository = mock(PlayerRepository.class);
        playerPriceHistoryRepository = mock(PlayerPriceHistoryRepository.class);
        playerPriceHistoryService = mock(PlayerPriceHistoryService.class);
        playerMatchReportService = mock(PlayerMatchReportService.class);

        service = new PlayerDetailSyncService(
                biwengerClient,
                playerRepository,
                playerPriceHistoryRepository,
                playerPriceHistoryService,
                playerMatchReportService);

        ReflectionTestUtils.setField(
                service,
                "batchSize",
                25);
    }

    @Test
    void syncLeaguePlayerDetailsShouldUseOneDetailResponseForPricesAndReports() {

        Player player = player(
                10L,
                "raphinha");

        BiwengerPlayerDetailResponse response = mock(
                BiwengerPlayerDetailResponse.class);

        PlayerReportScoreConfig scoreConfig = new PlayerReportScoreConfig(
                100,
                "custom-score");

        when(playerMatchReportService.loadLeagueScoreConfig())
                .thenReturn(scoreConfig);

        when(playerRepository.findAllByLeague_Id(1L))
                .thenReturn(
                        List.of(player));

        when(playerPriceHistoryRepository
                .findPlayerIdsWithHistoryByLeagueId(1L))
                .thenReturn(
                        List.of());

        when(biwengerClient.getPlayerDetail("raphinha"))
                .thenReturn(response);

        when(playerPriceHistoryService.syncPlayerPriceHistory(
                player,
                response))
                .thenReturn(366);

        when(playerMatchReportService.syncPlayerReports(
                player,
                response,
                scoreConfig))
                .thenReturn(20);

        PlayerDetailSyncResponse result = service
                .syncLeaguePlayerDetails(1L);

        assertEquals(
                1,
                result.playersTotal());

        assertEquals(
                1,
                result.playersEligible());

        assertEquals(
                1,
                result.playersAttempted());

        assertEquals(
                1,
                result.playersCompleted());

        assertEquals(
                366,
                result.pricesProcessed());

        assertEquals(
                20,
                result.reportsProcessed());

        assertTrue(
                result.completed());

        assertNull(
                result.stopReason());

        assertEquals(
                10L,
                result.lastCompletedPlayerId());

        assertNull(
                result.rateLimitedPlayerId());

        /*
         * La comprobación fundamental:
         *
         * el coordinador realiza UNA única petición HTTP
         * de detalle.
         */
        verify(biwengerClient)
                .getPlayerDetail(
                        "raphinha");

        /*
         * Y exactamente esa misma respuesta se entrega
         * a los dos subsistemas.
         */
        verify(playerPriceHistoryService)
                .syncPlayerPriceHistory(
                        player,
                        response);

        verify(playerMatchReportService)
                .syncPlayerReports(
                        player,
                        response,
                        scoreConfig);

        verify(playerMatchReportService, times(1))
                .loadLeagueScoreConfig();
    }

    @Test
    void syncLeaguePlayerDetailsShouldPrioritizePlayersWithoutPriceHistory() {

        Player withHistory = player(
                1L,
                "with-history");

        Player withoutHistory = player(
                2L,
                "without-history");

        when(playerRepository.findAllByLeague_Id(1L))
                .thenReturn(
                        List.of(
                                withHistory,
                                withoutHistory));

        when(playerPriceHistoryRepository
                .findPlayerIdsWithHistoryByLeagueId(1L))
                .thenReturn(
                        List.of(1L));

        ReflectionTestUtils.setField(
                service,
                "batchSize",
                1);

        BiwengerPlayerDetailResponse response = mock(
                BiwengerPlayerDetailResponse.class);

        PlayerReportScoreConfig scoreConfig = new PlayerReportScoreConfig(
                100,
                "custom-score");

        when(playerMatchReportService.loadLeagueScoreConfig())
                .thenReturn(scoreConfig);

        when(biwengerClient.getPlayerDetail("without-history"))
                .thenReturn(response);

        PlayerDetailSyncResponse result = service
                .syncLeaguePlayerDetails(1L);

        assertEquals(
                1,
                result.playersAttempted());

        assertEquals(
                1,
                result.playersCompleted());

        assertEquals(
                2L,
                result.lastCompletedPlayerId());

        verify(biwengerClient)
                .getPlayerDetail(
                        "without-history");

        verify(biwengerClient, never())
                .getPlayerDetail(
                        "with-history");
    }

    @Test
    void syncLeaguePlayerDetailsShouldStopOnRateLimitWithoutSkippingPlayer() {

        Player first = player(
                1L,
                "first");

        Player rateLimited = player(
                2L,
                "rate-limited");

        Player third = player(
                3L,
                "third");

        when(playerRepository.findAllByLeague_Id(1L))
                .thenReturn(
                        List.of(
                                first,
                                rateLimited,
                                third));

        when(playerPriceHistoryRepository
                .findPlayerIdsWithHistoryByLeagueId(1L))
                .thenReturn(
                        List.of());

        BiwengerPlayerDetailResponse firstResponse = mock(
                BiwengerPlayerDetailResponse.class);

        PlayerReportScoreConfig scoreConfig = new PlayerReportScoreConfig(
                100,
                "custom-score");

        when(playerMatchReportService.loadLeagueScoreConfig())
                .thenReturn(scoreConfig);

        when(biwengerClient.getPlayerDetail("first"))
                .thenReturn(firstResponse);

        when(playerPriceHistoryService.syncPlayerPriceHistory(
                first,
                firstResponse))
                .thenReturn(10);

        when(playerMatchReportService.syncPlayerReports(
                first,
                firstResponse,
                scoreConfig))
                .thenReturn(5);

        when(biwengerClient.getPlayerDetail("rate-limited"))
                .thenThrow(
                        HttpClientErrorException.create(
                                HttpStatus.TOO_MANY_REQUESTS,
                                "Too Many Requests",
                                HttpHeaders.EMPTY,
                                null,
                                null));

        PlayerDetailSyncResponse result = service
                .syncLeaguePlayerDetails(1L);

        assertFalse(
                result.completed());

        assertEquals(
                "RATE_LIMIT",
                result.stopReason());

        assertEquals(
                2,
                result.playersAttempted());

        assertEquals(
                1,
                result.playersCompleted());

        assertEquals(
                10,
                result.pricesProcessed());

        assertEquals(
                5,
                result.reportsProcessed());

        assertEquals(
                1L,
                result.lastCompletedPlayerId());

        assertEquals(
                2L,
                result.rateLimitedPlayerId());

        /*
         * El primer jugador sí se persistió correctamente.
         */
        verify(playerPriceHistoryService)
                .syncPlayerPriceHistory(
                        first,
                        firstResponse);

        verify(playerMatchReportService)
                .syncPlayerReports(
                        first,
                        firstResponse,
                        scoreConfig);

        /*
         * Como el 429 ocurre en getPlayerDetail(), para el segundo
         * jugador nunca existe una response que pueda enviarse a
         * los servicios de persistencia.
         *
         * El hecho de que el tercer jugador tampoco sea consultado
         * demuestra que la tanda se detuvo exactamente en el 429.
         */
        verify(biwengerClient, never())
                .getPlayerDetail(
                        "third");
    }

    private Player player(
            Long id,
            String slug) {

        Player player = mock(
                Player.class);

        when(player.getId())
                .thenReturn(id);

        when(player.getSlug())
                .thenReturn(slug);

        when(player.getReportsLastSyncSuccessAt())
                .thenReturn(null);

        return player;
    }
}