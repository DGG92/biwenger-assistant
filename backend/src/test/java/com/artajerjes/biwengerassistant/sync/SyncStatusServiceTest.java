package com.artajerjes.biwengerassistant.sync;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.artajerjes.biwengerassistant.biwenger.BiwengerSyncService;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

class SyncStatusServiceTest {

        @Test
        void shouldCalculateLeagueSyncStatus() {

                PlayerRepository playerRepository = mock(PlayerRepository.class);

                PlayerPriceHistoryRepository playerPriceHistoryRepository = mock(PlayerPriceHistoryRepository.class);

                SyncStateService syncStateService = mock(SyncStateService.class);

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                LocalDateTime oldestSuccess = LocalDateTime.of(2026, 9, 1, 10, 0);

                LocalDateTime newestSuccess = LocalDateTime.of(2026, 9, 3, 18, 0);

                LocalDateTime newestAttempt = LocalDateTime.of(2026, 9, 3, 18, 30);

                Player player1 = mock(Player.class);
                when(player1.getId()).thenReturn(1L);
                when(player1.getSlug()).thenReturn("player-1");
                when(player1.getReportsLastSyncSuccessAt())
                                .thenReturn(oldestSuccess);
                when(player1.getReportsLastSyncAttemptAt())
                                .thenReturn(oldestSuccess);

                Player player2 = mock(Player.class);
                when(player2.getId()).thenReturn(2L);
                when(player2.getSlug()).thenReturn("player-2");
                when(player2.getReportsLastSyncSuccessAt())
                                .thenReturn(newestSuccess);
                when(player2.getReportsLastSyncAttemptAt())
                                .thenReturn(newestSuccess);

                Player player3 = mock(Player.class);
                when(player3.getId()).thenReturn(3L);
                when(player3.getSlug()).thenReturn("player-3");
                when(player3.getReportsLastSyncSuccessAt())
                                .thenReturn(null);
                when(player3.getReportsLastSyncAttemptAt())
                                .thenReturn(newestAttempt);

                Player playerWithoutSlug = mock(Player.class);
                when(playerWithoutSlug.getId()).thenReturn(4L);
                when(playerWithoutSlug.getSlug()).thenReturn(null);

                when(playerRepository.findAllByLeague_Id(1L))
                                .thenReturn(List.of(
                                                player1,
                                                player2,
                                                player3,
                                                playerWithoutSlug));

                when(playerPriceHistoryRepository
                                .findPlayerIdsWithHistoryByLeagueId(1L))
                                .thenReturn(List.of(1L, 3L, 4L));

                when(syncStateService.findState(
                                1L,
                                SyncType.PLAYER_DETAILS))
                                .thenReturn(null);

                when(syncStateService.isInCooldown(
                                1L,
                                SyncType.PLAYER_DETAILS))
                                .thenReturn(false);

                SyncStatusService service = new SyncStatusService(
                                playerRepository,
                                playerPriceHistoryRepository,
                                syncStateService,
                                biwengerSyncService,
                                syncExecutionStateService,
                                false,
                                900000L);

                SyncStatusResponse response = service.getStatus(1L);

                assertThat(response.leagueId()).isEqualTo(1L);

                assertThat(response.scheduler().enabled()).isFalse();
                assertThat(response.scheduler().intervalMs())
                                .isEqualTo(900000L);

                assertThat(response.execution().status())
                                .isEqualTo(SyncExecutionStatus.IDLE);

                assertThat(response.execution().startedAt())
                                .isNull();

                assertThat(response.execution().finishedAt())
                                .isNull();

                assertThat(response.execution().lastError())
                                .isNull();

                assertThat(response.details().state())
                                .isEqualTo("READY");

                assertThat(response.details().lastRateLimitAt())
                                .isNull();

                assertThat(response.details().rateLimitedPlayerId())
                                .isNull();

                assertThat(response.details().retryAfterSeconds())
                                .isNull();

                assertThat(response.details().cooldownUntil())
                                .isNull();

                assertThat(response.players().total()).isEqualTo(4);
                assertThat(response.players().eligible()).isEqualTo(3);

                assertThat(response.players().reports().completed())
                                .isEqualTo(2);

                assertThat(response.players().reports().pending())
                                .isEqualTo(1);

                assertThat(response.players().reports().coveragePercent())
                                .isEqualTo(66.67);

                assertThat(response.players().reports().oldestSuccessAt())
                                .isEqualTo(oldestSuccess);

                assertThat(response.players().reports().lastSuccessAt())
                                .isEqualTo(newestSuccess);

                assertThat(response.players().reports().lastAttemptAt())
                                .isEqualTo(newestAttempt);

                assertThat(response.players().priceHistory().completed())
                                .isEqualTo(2);

                assertThat(response.players().priceHistory().pending())
                                .isEqualTo(1);

                assertThat(response.players().priceHistory().coveragePercent())
                                .isEqualTo(66.67);
        }

        @Test
        void shouldExposePlayerDetailsRateLimitStatus() {

                PlayerRepository playerRepository = mock(PlayerRepository.class);

                PlayerPriceHistoryRepository playerPriceHistoryRepository = mock(PlayerPriceHistoryRepository.class);

                SyncStateService syncStateService = mock(SyncStateService.class);

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                LocalDateTime lastRateLimitAt = LocalDateTime.of(2026, 9, 4, 12, 0);

                LocalDateTime cooldownUntil = LocalDateTime.of(2026, 9, 4, 13, 0);

                SyncState syncState = new SyncState(
                                1L,
                                SyncType.PLAYER_DETAILS);

                syncState.registerRateLimit(
                                lastRateLimitAt,
                                502L,
                                3600L,
                                cooldownUntil);

                when(playerRepository.findAllByLeague_Id(1L))
                                .thenReturn(List.of());

                when(playerPriceHistoryRepository
                                .findPlayerIdsWithHistoryByLeagueId(1L))
                                .thenReturn(List.of());

                when(syncStateService.findState(
                                1L,
                                SyncType.PLAYER_DETAILS))
                                .thenReturn(syncState);

                when(syncStateService.isInCooldown(
                                1L,
                                SyncType.PLAYER_DETAILS))
                                .thenReturn(true);

                SyncStatusService service = new SyncStatusService(
                                playerRepository,
                                playerPriceHistoryRepository,
                                syncStateService,
                                biwengerSyncService,
                                syncExecutionStateService,
                                false,
                                900000L);

                SyncStatusResponse response = service.getStatus(1L);

                assertThat(response.details().state())
                                .isEqualTo("RATE_LIMITED");

                assertThat(response.details().lastRateLimitAt())
                                .isEqualTo(lastRateLimitAt);

                assertThat(response.details().rateLimitedPlayerId())
                                .isEqualTo(502L);

                assertThat(response.details().retryAfterSeconds())
                                .isEqualTo(3600L);

                assertThat(response.details().cooldownUntil())
                                .isEqualTo(cooldownUntil);

                assertThat(response.execution().status())
                                .isEqualTo(SyncExecutionStatus.IDLE);
        }

        @Test
        void shouldExposeRunningSync() {

                PlayerRepository playerRepository = mock(PlayerRepository.class);

                PlayerPriceHistoryRepository playerPriceHistoryRepository = mock(PlayerPriceHistoryRepository.class);

                SyncStateService syncStateService = mock(SyncStateService.class);

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                when(playerRepository.findAllByLeague_Id(1L))
                                .thenReturn(List.of());

                when(playerPriceHistoryRepository
                                .findPlayerIdsWithHistoryByLeagueId(1L))
                                .thenReturn(List.of());

                when(syncStateService.findState(
                                1L,
                                SyncType.PLAYER_DETAILS))
                                .thenReturn(null);

                when(syncStateService.isInCooldown(
                                1L,
                                SyncType.PLAYER_DETAILS))
                                .thenReturn(false);

                when(biwengerSyncService.isSyncRunning(1L))
                                .thenReturn(true);

                SyncStatusService service = new SyncStatusService(
                                playerRepository,
                                playerPriceHistoryRepository,
                                syncStateService,
                                biwengerSyncService,
                                syncExecutionStateService,
                                false,
                                900000L);

                SyncStatusResponse response = service.getStatus(1L);

                assertThat(response.execution().status())
                                .isEqualTo(SyncExecutionStatus.RUNNING);
        }

        @Test
        void shouldExposeLastSuccessfulSync() {

                PlayerRepository playerRepository = mock(PlayerRepository.class);

                PlayerPriceHistoryRepository playerPriceHistoryRepository = mock(PlayerPriceHistoryRepository.class);

                SyncStateService syncStateService = mock(SyncStateService.class);

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                when(playerRepository.findAllByLeague_Id(1L))
                                .thenReturn(List.of());

                when(playerPriceHistoryRepository
                                .findPlayerIdsWithHistoryByLeagueId(1L))
                                .thenReturn(List.of());

                LocalDateTime startedAt = LocalDateTime.of(2026, 9, 4, 18, 0);

                LocalDateTime finishedAt = LocalDateTime.of(2026, 9, 4, 18, 2);

                SyncExecutionState executionState = new SyncExecutionState(1L);

                executionState.markRunning(startedAt);
                executionState.markSuccess(finishedAt);

                when(syncExecutionStateService.findState(1L))
                                .thenReturn(executionState);

                when(biwengerSyncService.isSyncRunning(1L))
                                .thenReturn(false);

                SyncStatusService service = new SyncStatusService(
                                playerRepository,
                                playerPriceHistoryRepository,
                                syncStateService,
                                biwengerSyncService,
                                syncExecutionStateService,
                                false,
                                900000L);

                SyncStatusResponse response = service.getStatus(1L);

                assertThat(response.execution().status())
                                .isEqualTo(SyncExecutionStatus.SUCCESS);

                assertThat(response.execution().startedAt())
                                .isEqualTo(startedAt);

                assertThat(response.execution().finishedAt())
                                .isEqualTo(finishedAt);

                assertThat(response.execution().lastError())
                                .isNull();
        }

        @Test
        void shouldExposeLastFailedSync() {

                PlayerRepository playerRepository = mock(PlayerRepository.class);

                PlayerPriceHistoryRepository playerPriceHistoryRepository = mock(PlayerPriceHistoryRepository.class);

                SyncStateService syncStateService = mock(SyncStateService.class);

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                when(playerRepository.findAllByLeague_Id(1L))
                                .thenReturn(List.of());

                when(playerPriceHistoryRepository
                                .findPlayerIdsWithHistoryByLeagueId(1L))
                                .thenReturn(List.of());

                LocalDateTime startedAt = LocalDateTime.of(2026, 9, 4, 18, 0);

                LocalDateTime finishedAt = LocalDateTime.of(2026, 9, 4, 18, 1);

                SyncExecutionState executionState = new SyncExecutionState(1L);

                executionState.markRunning(startedAt);
                executionState.markFailed(
                                finishedAt,
                                "Biwenger unavailable");

                when(syncExecutionStateService.findState(1L))
                                .thenReturn(executionState);

                when(biwengerSyncService.isSyncRunning(1L))
                                .thenReturn(false);

                SyncStatusService service = new SyncStatusService(
                                playerRepository,
                                playerPriceHistoryRepository,
                                syncStateService,
                                biwengerSyncService,
                                syncExecutionStateService,
                                false,
                                900000L);

                SyncStatusResponse response = service.getStatus(1L);

                assertThat(response.execution().status())
                                .isEqualTo(SyncExecutionStatus.FAILED);

                assertThat(response.execution().startedAt())
                                .isEqualTo(startedAt);

                assertThat(response.execution().finishedAt())
                                .isEqualTo(finishedAt);

                assertThat(response.execution().lastError())
                                .isEqualTo("Biwenger unavailable");
        }
}