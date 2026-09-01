package com.artajerjes.biwengerassistant.biwenger.dto.sync;

public record PlayerDetailSyncResponse(
        int playersTotal,
        int playersEligible,
        int playersAttempted,
        int playersCompleted,
        int pricesProcessed,
        int reportsProcessed,
        boolean completed,
        String stopReason,
        Long lastCompletedPlayerId,
        Long rateLimitedPlayerId) {
}