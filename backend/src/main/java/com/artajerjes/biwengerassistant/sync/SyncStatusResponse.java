package com.artajerjes.biwengerassistant.sync;

import java.time.LocalDateTime;

public record SyncStatusResponse(
                Long leagueId,
                SchedulerStatus scheduler,
                ExecutionStatus execution,
                DetailSyncStatus details,
                PlayerSyncStatus players) {

        public record SchedulerStatus(
                        boolean enabled,
                        long intervalMs) {
        }

        public record ExecutionStatus(
                        SyncExecutionStatus status,
                        LocalDateTime startedAt,
                        LocalDateTime finishedAt,
                        String lastError) {
        }

        public record DetailSyncStatus(
                        String state,
                        LocalDateTime lastRateLimitAt,
                        Long rateLimitedPlayerId,
                        Long retryAfterSeconds,
                        LocalDateTime cooldownUntil) {
        }

        public record PlayerSyncStatus(
                        int total,
                        int eligible,
                        ReportSyncStatus reports,
                        PriceHistorySyncStatus priceHistory) {
        }

        public record ReportSyncStatus(
                        int completed,
                        int pending,
                        double coveragePercent,
                        LocalDateTime oldestSuccessAt,
                        LocalDateTime lastSuccessAt,
                        LocalDateTime lastAttemptAt) {
        }

        public record PriceHistorySyncStatus(
                        int completed,
                        int pending,
                        double coveragePercent) {
        }
}