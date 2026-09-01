package com.artajerjes.biwengerassistant.history;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.web.client.HttpClientErrorException;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailData;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailResponse;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

class PlayerPriceHistoryServiceTest {

    private PlayerRepository playerRepository;
    private PlayerPriceHistoryRepository playerPriceHistoryRepository;
    private BiwengerClient biwengerClient;

    private PlayerPriceHistoryService service;

    @BeforeEach
    void setUp() {

        playerRepository = mock(PlayerRepository.class);
        playerPriceHistoryRepository = mock(
                PlayerPriceHistoryRepository.class);
        biwengerClient = mock(BiwengerClient.class);

        service = new PlayerPriceHistoryService(
                playerRepository,
                playerPriceHistoryRepository,
                biwengerClient);
    }

    @Test
    void parsePriceDateShouldConvertBiwengerDate() {

        LocalDate result = service.parsePriceDate(
                260901L);

        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        1),
                result);
    }

    @Test
    void syncPlayerPriceHistoryShouldCreateHistoricalPrices() {

        League league = mock(League.class);
        Player player = mock(Player.class);

        when(player.getId())
                .thenReturn(10L);

        when(player.getSlug())
                .thenReturn("raphinha");

        when(player.getLeague())
                .thenReturn(league);

        when(league.getId())
                .thenReturn(1L);

        when(playerRepository.findByIdAndLeague_Id(
                10L,
                1L))
                .thenReturn(Optional.of(player));

        when(biwengerClient.getPlayerDetail(
                "raphinha"))
                .thenReturn(createResponse(
                        List.of(
                                List.of(
                                        260830L,
                                        17_570_000L),
                                List.of(
                                        260831L,
                                        17_760_000L),
                                List.of(
                                        260901L,
                                        17_950_000L))));

        when(playerPriceHistoryRepository
                .findByPlayerIdAndPriceDate(
                        any(),
                        any()))
                .thenReturn(Optional.empty());

        int processed = service.syncPlayerPriceHistory(
                1L,
                10L);

        assertEquals(
                3,
                processed);

        ArgumentCaptor<PlayerPriceHistory> captor = ArgumentCaptor
                .forClass(
                        PlayerPriceHistory.class);

        verify(
                playerPriceHistoryRepository,
                org.mockito.Mockito.times(3))
                .save(
                        captor.capture());

        List<PlayerPriceHistory> saved = captor
                .getAllValues();

        assertEquals(
                LocalDate.of(
                        2026,
                        8,
                        30),
                saved.get(0).getPriceDate());

        assertEquals(
                17_570_000L,
                saved.get(0).getMarketValue());

        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        1),
                saved.get(2).getPriceDate());

        assertEquals(
                17_950_000L,
                saved.get(2).getMarketValue());

        assertEquals(
                PlayerPriceSource.BIWENGER_DETAIL,
                saved.get(2).getSource());
    }

    @Test
    void syncPlayerPriceHistoryShouldUpdateExistingPrice() {

        League league = mock(League.class);
        Player player = mock(Player.class);

        when(player.getId())
                .thenReturn(10L);

        when(player.getSlug())
                .thenReturn("raphinha");

        when(player.getLeague())
                .thenReturn(league);

        when(league.getId())
                .thenReturn(1L);

        when(playerRepository.findByIdAndLeague_Id(
                10L,
                1L))
                .thenReturn(Optional.of(player));

        when(biwengerClient.getPlayerDetail(
                "raphinha"))
                .thenReturn(createResponse(
                        List.of(
                                List.of(
                                        260901L,
                                        17_950_000L))));

        PlayerPriceHistory existing = new PlayerPriceHistory(
                10L,
                1L,
                LocalDate.of(
                        2026,
                        9,
                        1),
                17_800_000L,
                PlayerPriceSource.BIWENGER_DETAIL,
                java.time.LocalDateTime.of(
                        2026,
                        9,
                        1,
                        8,
                        0));

        when(playerPriceHistoryRepository
                .findByPlayerIdAndPriceDate(
                        10L,
                        LocalDate.of(
                                2026,
                                9,
                                1)))
                .thenReturn(
                        Optional.of(
                                existing));

        int processed = service.syncPlayerPriceHistory(
                1L,
                10L);

        assertEquals(
                1,
                processed);

        assertEquals(
                17_950_000L,
                existing.getMarketValue());

        verify(playerPriceHistoryRepository)
                .save(existing);
    }

    @Test
    void syncLeaguePriceHistoryShouldProcessOnlyPlayersWithoutHistory() {

        League league = mock(League.class);

        Player alreadySynced = mock(Player.class);
        Player neverSyncedFirst = mock(Player.class);
        Player neverSyncedSecond = mock(Player.class);

        when(alreadySynced.getId())
                .thenReturn(10L);

        when(alreadySynced.getSlug())
                .thenReturn("already-synced");

        when(alreadySynced.getLeague())
                .thenReturn(league);

        when(neverSyncedFirst.getId())
                .thenReturn(20L);

        when(neverSyncedFirst.getSlug())
                .thenReturn("never-synced-first");

        when(neverSyncedFirst.getLeague())
                .thenReturn(league);

        when(neverSyncedSecond.getId())
                .thenReturn(30L);

        when(neverSyncedSecond.getSlug())
                .thenReturn("never-synced-second");

        when(neverSyncedSecond.getLeague())
                .thenReturn(league);

        when(league.getId())
                .thenReturn(1L);

        when(playerRepository.findAllByLeague_Id(
                1L))
                .thenReturn(
                        List.of(
                                alreadySynced,
                                neverSyncedSecond,
                                neverSyncedFirst));

        /*
         * Solo el jugador 10 tiene histórico.
         *
         * Aunque aparece el primero en la lista original,
         * los jugadores 20 y 30 deben tener prioridad.
         */
        when(playerPriceHistoryRepository
                .findPlayerIdsWithHistoryByLeagueId(
                        1L))
                .thenReturn(
                        List.of(10L));

        when(playerRepository.findByIdAndLeague_Id(
                20L,
                1L))
                .thenReturn(
                        Optional.of(
                                neverSyncedFirst));

        when(playerRepository.findByIdAndLeague_Id(
                30L,
                1L))
                .thenReturn(
                        Optional.of(
                                neverSyncedSecond));

        when(biwengerClient.getPlayerDetail(
                any()))
                .thenReturn(
                        createResponse(
                                List.of(
                                        List.of(
                                                260901L,
                                                10_000_000L))));

        when(playerPriceHistoryRepository
                .findByPlayerIdAndPriceDate(
                        any(),
                        any()))
                .thenReturn(
                        Optional.empty());

        org.springframework.test.util.ReflectionTestUtils
                .setField(
                        service,
                        "pricesSyncBatchSize",
                        2);

        var result = service.syncLeaguePriceHistory(
                1L);

        assertEquals(
                3,
                result.playersTotal());

        assertEquals(
                2,
                result.playersEligible());

        assertEquals(
                2,
                result.playersAttempted());

        assertEquals(
                2,
                result.playersCompleted());

        assertEquals(
                2,
                result.pricesProcessed());

        /*
         * Entre los nunca sincronizados se desempata por ID:
         *
         * 20 → primero
         * 30 → segundo
         */
        assertEquals(
                30L,
                result.lastCompletedPlayerId());

        verify(biwengerClient)
                .getPlayerDetail(
                        "never-synced-first");

        verify(biwengerClient)
                .getPlayerDetail(
                        "never-synced-second");

        /*
         * El jugador 10 ya tenía histórico.
         * Con batch-size 2 no debe llegar a procesarse.
         */
        verify(
                biwengerClient,
                times(0))
                .getPlayerDetail(
                        "already-synced");
    }

    @Test
    void syncLeaguePriceHistoryShouldStopOnRateLimit() {

        League league = mock(League.class);

        Player first = mock(Player.class);
        Player second = mock(Player.class);
        Player third = mock(Player.class);

        when(first.getId())
                .thenReturn(10L);

        when(first.getSlug())
                .thenReturn("first");

        when(first.getLeague())
                .thenReturn(league);

        when(second.getId())
                .thenReturn(20L);

        when(second.getSlug())
                .thenReturn("second");

        when(second.getLeague())
                .thenReturn(league);

        when(third.getId())
                .thenReturn(30L);

        when(third.getSlug())
                .thenReturn("third");

        when(third.getLeague())
                .thenReturn(league);

        when(league.getId())
                .thenReturn(1L);

        when(playerRepository.findAllByLeague_Id(
                1L))
                .thenReturn(
                        List.of(
                                first,
                                second,
                                third));

        when(playerPriceHistoryRepository
                .findPlayerIdsWithHistoryByLeagueId(
                        1L))
                .thenReturn(List.of());

        when(playerRepository.findByIdAndLeague_Id(
                10L,
                1L))
                .thenReturn(Optional.of(first));

        when(playerRepository.findByIdAndLeague_Id(
                20L,
                1L))
                .thenReturn(Optional.of(second));

        when(biwengerClient.getPlayerDetail(
                "first"))
                .thenReturn(
                        createResponse(
                                List.of(
                                        List.of(
                                                260901L,
                                                10_000_000L))));

        when(biwengerClient.getPlayerDetail(
                "second"))
                .thenThrow(
                        mock(
                                HttpClientErrorException.TooManyRequests.class));

        when(playerPriceHistoryRepository
                .findByPlayerIdAndPriceDate(
                        any(),
                        any()))
                .thenReturn(Optional.empty());

        org.springframework.test.util.ReflectionTestUtils
                .setField(
                        service,
                        "pricesSyncBatchSize",
                        25);

        var result = service.syncLeaguePriceHistory(
                1L);

        assertEquals(
                2,
                result.playersAttempted());

        assertEquals(
                1,
                result.playersCompleted());

        assertEquals(
                1,
                result.pricesProcessed());

        assertEquals(
                false,
                result.completed());

        assertEquals(
                "RATE_LIMIT",
                result.stopReason());

        assertEquals(
                10L,
                result.lastCompletedPlayerId());

        assertEquals(
                20L,
                result.rateLimitedPlayerId());

        verify(
                biwengerClient,
                times(0))
                .getPlayerDetail(
                        "third");
    }

    @Test
    void syncLeaguePriceHistoryShouldDoNothingWhenAllPlayersHaveHistory() {

        Player first = mock(Player.class);
        Player second = mock(Player.class);

        when(first.getId())
                .thenReturn(10L);

        when(first.getSlug())
                .thenReturn("first");

        when(second.getId())
                .thenReturn(20L);

        when(second.getSlug())
                .thenReturn("second");

        when(playerRepository.findAllByLeague_Id(
                1L))
                .thenReturn(
                        List.of(
                                first,
                                second));

        when(playerPriceHistoryRepository
                .findPlayerIdsWithHistoryByLeagueId(
                        1L))
                .thenReturn(
                        List.of(
                                10L,
                                20L));

        org.springframework.test.util.ReflectionTestUtils
                .setField(
                        service,
                        "pricesSyncBatchSize",
                        25);

        var result = service.syncLeaguePriceHistory(
                1L);

        assertEquals(
                2,
                result.playersTotal());

        assertEquals(
                0,
                result.playersEligible());

        assertEquals(
                0,
                result.playersAttempted());

        assertEquals(
                0,
                result.playersCompleted());

        assertEquals(
                0,
                result.pricesProcessed());

        assertEquals(
                true,
                result.completed());

        verify(
                biwengerClient,
                times(0))
                .getPlayerDetail(
                        any());
    }

    private BiwengerPlayerDetailResponse createResponse(
            List<List<Long>> prices) {

        return new BiwengerPlayerDetailResponse(
                200,
                new BiwengerPlayerDetailData(
                        26930L,
                        "Raphinha",
                        "raphinha",
                        prices,
                        List.of(),
                        List.of()));
    }
}