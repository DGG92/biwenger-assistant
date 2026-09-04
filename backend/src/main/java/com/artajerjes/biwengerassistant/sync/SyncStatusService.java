package com.artajerjes.biwengerassistant.sync;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.biwenger.BiwengerSyncService;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@Service
public class SyncStatusService {

        private final PlayerRepository playerRepository;
        private final PlayerPriceHistoryRepository playerPriceHistoryRepository;
        private final SyncStateService syncStateService;
        private final BiwengerSyncService biwengerSyncService;
        private final SyncExecutionStateService syncExecutionStateService;
        private final boolean schedulerEnabled;
        private final long schedulerIntervalMs;

        public SyncStatusService(
                        PlayerRepository playerRepository,
                        PlayerPriceHistoryRepository playerPriceHistoryRepository,
                        SyncStateService syncStateService,
                        BiwengerSyncService biwengerSyncService,
                        SyncExecutionStateService syncExecutionStateService,
                        @Value("${biwenger.sync.enabled:false}") boolean schedulerEnabled,
                        @Value("${biwenger.sync.interval-ms:900000}") long schedulerIntervalMs) {

                this.playerRepository = playerRepository;
                this.playerPriceHistoryRepository = playerPriceHistoryRepository;
                this.syncStateService = syncStateService;
                this.biwengerSyncService = biwengerSyncService;
                this.syncExecutionStateService = syncExecutionStateService;
                this.schedulerEnabled = schedulerEnabled;
                this.schedulerIntervalMs = schedulerIntervalMs;
        }

        public SyncStatusResponse getStatus(
                        Long leagueId) {

                List<Player> players = playerRepository.findAllByLeague_Id(leagueId);

                SyncState detailSyncState = syncStateService.findState(
                                leagueId,
                                SyncType.PLAYER_DETAILS);

                SyncExecutionState executionState = syncExecutionStateService.findState(leagueId);

                boolean syncRunning = biwengerSyncService.isSyncRunning(leagueId);

                boolean detailSyncInCooldown = syncStateService.isInCooldown(
                                leagueId,
                                SyncType.PLAYER_DETAILS);

                List<Player> eligiblePlayers = players.stream()
                                .filter(this::isEligibleForDetailSync)
                                .toList();

                Set<Long> playerIdsWithPriceHistory = new HashSet<>(
                                playerPriceHistoryRepository
                                                .findPlayerIdsWithHistoryByLeagueId(leagueId));

                int reportsCompleted = (int) eligiblePlayers.stream()
                                .filter(player -> player.getReportsLastSyncSuccessAt() != null)
                                .count();

                int reportsPending = eligiblePlayers.size() - reportsCompleted;

                int priceHistoryCompleted = (int) eligiblePlayers.stream()
                                .filter(player -> playerIdsWithPriceHistory.contains(player.getId()))
                                .count();

                int priceHistoryPending = eligiblePlayers.size() - priceHistoryCompleted;

                LocalDateTime oldestSuccessAt = eligiblePlayers.stream()
                                .map(Player::getReportsLastSyncSuccessAt)
                                .filter(value -> value != null)
                                .min(LocalDateTime::compareTo)
                                .orElse(null);

                LocalDateTime lastSuccessAt = eligiblePlayers.stream()
                                .map(Player::getReportsLastSyncSuccessAt)
                                .filter(value -> value != null)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);

                LocalDateTime lastAttemptAt = eligiblePlayers.stream()
                                .map(Player::getReportsLastSyncAttemptAt)
                                .filter(value -> value != null)
                                .max(LocalDateTime::compareTo)
                                .orElse(null);

                SyncExecutionStatus executionStatus;

                if (syncRunning) {
                        executionStatus = SyncExecutionStatus.RUNNING;
                } else if (executionState != null) {
                        executionStatus = executionState.getStatus();
                } else {
                        executionStatus = SyncExecutionStatus.IDLE;
                }

                return new SyncStatusResponse(
                                leagueId,
                                new SyncStatusResponse.SchedulerStatus(
                                                schedulerEnabled,
                                                schedulerIntervalMs),
                                new SyncStatusResponse.ExecutionStatus(
                                                executionStatus,
                                                executionState == null
                                                                ? null
                                                                : executionState.getStartedAt(),
                                                executionState == null
                                                                ? null
                                                                : executionState.getFinishedAt(),
                                                executionState == null
                                                                ? null
                                                                : executionState.getLastError()),
                                new SyncStatusResponse.DetailSyncStatus(
                                                detailSyncInCooldown
                                                                ? "RATE_LIMITED"
                                                                : "READY",
                                                detailSyncState == null
                                                                ? null
                                                                : detailSyncState.getLastRateLimitAt(),
                                                detailSyncState == null
                                                                ? null
                                                                : detailSyncState.getRateLimitedPlayerId(),
                                                detailSyncState == null
                                                                ? null
                                                                : detailSyncState.getRetryAfterSeconds(),
                                                detailSyncState == null
                                                                ? null
                                                                : detailSyncState.getCooldownUntil()),
                                new SyncStatusResponse.PlayerSyncStatus(
                                                players.size(),
                                                eligiblePlayers.size(),
                                                new SyncStatusResponse.ReportSyncStatus(
                                                                reportsCompleted,
                                                                reportsPending,
                                                                calculateCoverage(
                                                                                reportsCompleted,
                                                                                eligiblePlayers.size()),
                                                                oldestSuccessAt,
                                                                lastSuccessAt,
                                                                lastAttemptAt),
                                                new SyncStatusResponse.PriceHistorySyncStatus(
                                                                priceHistoryCompleted,
                                                                priceHistoryPending,
                                                                calculateCoverage(
                                                                                priceHistoryCompleted,
                                                                                eligiblePlayers.size()))));
        }

        private boolean isEligibleForDetailSync(
                        Player player) {

                return player.getSlug() != null
                                && !player.getSlug().isBlank();
        }

        private double calculateCoverage(
                        int completed,
                        int total) {

                if (total == 0) {
                        return 0.0;
                }

                return Math.round(
                                ((double) completed / total) * 10000.0)
                                / 100.0;
        }
}