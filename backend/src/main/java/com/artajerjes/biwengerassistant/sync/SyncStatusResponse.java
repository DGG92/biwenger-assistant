package com.artajerjes.biwengerassistant.sync;

import java.time.OffsetDateTime;

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
                        OffsetDateTime startedAt,
                        OffsetDateTime finishedAt,
                        String lastError) {
        }

        public record DetailSyncStatus(
                        String state,
                        OffsetDateTime lastRateLimitAt,
                        Long rateLimitedPlayerId,
                        Long retryAfterSeconds,
                        OffsetDateTime cooldownUntil) {
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
                        OffsetDateTime oldestSuccessAt,
                        OffsetDateTime lastSuccessAt,
                        OffsetDateTime lastAttemptAt) {
        }

        public record PriceHistorySyncStatus(
                        int completed,
                        int pending,
                        double coveragePercent) {
        }
}