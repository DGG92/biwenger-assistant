package com.artajerjes.biwengerassistant.sync;

public record SyncNowResponse(
                Long leagueId,
                boolean started,
                SyncExecutionStatus status) {
}