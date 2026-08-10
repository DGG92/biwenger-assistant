package com.artajerjes.biwengerassistant.biwenger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "biwenger.sync.enabled", havingValue = "true")
public class BiwengerSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(
            BiwengerSyncScheduler.class);

    private static final Long DEFAULT_LEAGUE_ID = 1L;

    private final BiwengerSyncService biwengerSyncService;

    public BiwengerSyncScheduler(
            BiwengerSyncService biwengerSyncService) {
        this.biwengerSyncService = biwengerSyncService;
    }

    @Scheduled(fixedDelayString = "${biwenger.sync.interval-ms:300000}")
    public void sync() {
        try {
            log.info(
                    "Starting automatic Biwenger sync for league {}",
                    DEFAULT_LEAGUE_ID);

            biwengerSyncService.syncAll(
                    DEFAULT_LEAGUE_ID);

            log.info(
                    "Automatic Biwenger sync completed successfully for league {}",
                    DEFAULT_LEAGUE_ID);

        } catch (Exception exception) {
            log.error(
                    "Automatic Biwenger sync failed for league {}",
                    DEFAULT_LEAGUE_ID,
                    exception);
        }
    }
}