package com.artajerjes.biwengerassistant.biwenger;

import java.util.List;

public record ScheduledSyncResult(
        boolean started,
        List<String> partialReasons) {

    public ScheduledSyncResult {
        partialReasons = partialReasons == null
                ? List.of()
                : List.copyOf(partialReasons);
    }

    public boolean partial() {
        return !partialReasons.isEmpty();
    }

    public static ScheduledSyncResult notStarted() {
        return new ScheduledSyncResult(
                false,
                List.of());
    }
}