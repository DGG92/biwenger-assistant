package com.artajerjes.biwengerassistant.playerreport.dto;

public record PlayerReportSyncResponse(
        int playersTotal,
        int playersEligible,
        int playersAttempted,
        int playersCompleted,
        int reportsProcessed,
        boolean completed,
        String stopReason,
        Long lastCompletedPlayerId,
        Long rateLimitedPlayerId) {
}