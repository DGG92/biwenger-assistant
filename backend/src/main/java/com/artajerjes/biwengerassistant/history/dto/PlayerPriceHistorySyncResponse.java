package com.artajerjes.biwengerassistant.history.dto;

public record PlayerPriceHistorySyncResponse(
        int playersTotal,
        int playersEligible,
        int playersAttempted,
        int playersCompleted,
        int pricesProcessed,
        boolean completed,
        String stopReason,
        Long lastCompletedPlayerId,
        Long rateLimitedPlayerId) {
}