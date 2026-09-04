package com.artajerjes.biwengerassistant.biwenger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.artajerjes.biwengerassistant.sync.SyncExecutionService;
import com.artajerjes.biwengerassistant.sync.SyncExecutionStatus;
import com.artajerjes.biwengerassistant.sync.SyncNowResponse;

@Component
@ConditionalOnProperty(name = "biwenger.sync.enabled", havingValue = "true")
public class BiwengerSyncScheduler {

        private static final Logger log = LoggerFactory.getLogger(
                        BiwengerSyncScheduler.class);

        private static final Long DEFAULT_LEAGUE_ID = 1L;

        private final SyncExecutionService syncExecutionService;

        public BiwengerSyncScheduler(
                        SyncExecutionService syncExecutionService) {

                this.syncExecutionService = syncExecutionService;
        }

        @Scheduled(fixedDelayString = "${biwenger.sync.interval-ms:900000}")
        public void sync() {

                try {
                        log.info(
                                        "Starting automatic Biwenger sync for league {}",
                                        DEFAULT_LEAGUE_ID);

                        SyncNowResponse response = syncExecutionService.syncNow(
                                        DEFAULT_LEAGUE_ID);

                        if (response.status() == SyncExecutionStatus.RUNNING
                                        && !response.started()) {

                                log.info(
                                                "Automatic Biwenger sync skipped for league {} because another sync is already running",
                                                DEFAULT_LEAGUE_ID);

                                return;
                        }

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