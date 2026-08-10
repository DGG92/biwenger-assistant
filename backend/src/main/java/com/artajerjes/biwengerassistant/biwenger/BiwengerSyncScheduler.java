package com.artajerjes.biwengerassistant.biwenger;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "biwenger.sync.enabled", havingValue = "true")
public class BiwengerSyncScheduler {

    private static final Long DEFAULT_LEAGUE_ID = 1L;

    private final BiwengerSyncService biwengerSyncService;

    public BiwengerSyncScheduler(
            BiwengerSyncService biwengerSyncService) {
        this.biwengerSyncService = biwengerSyncService;
    }

    @Scheduled(fixedDelayString = "${biwenger.sync.interval-ms:300000}")
    public void sync() {
        biwengerSyncService.syncAll(DEFAULT_LEAGUE_ID);
    }
}