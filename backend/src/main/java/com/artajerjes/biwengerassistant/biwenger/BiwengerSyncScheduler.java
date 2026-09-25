package com.artajerjes.biwengerassistant.biwenger;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.sync.SyncExecutionService;
import com.artajerjes.biwengerassistant.sync.SyncExecutionStatus;
import com.artajerjes.biwengerassistant.sync.SyncNowResponse;

@Component
@ConditionalOnProperty(name = "biwenger.sync.enabled", havingValue = "true")
public class BiwengerSyncScheduler {

        private static final Logger log = LoggerFactory.getLogger(
                        BiwengerSyncScheduler.class);

        private final SyncExecutionService syncExecutionService;
        private final LeagueRepository leagueRepository;

        public BiwengerSyncScheduler(
                        SyncExecutionService syncExecutionService,
                        LeagueRepository leagueRepository) {

                this.syncExecutionService = syncExecutionService;
                this.leagueRepository = leagueRepository;
        }

        @Scheduled(fixedDelayString = "${biwenger.sync.interval-ms:900000}")
        public void sync() {

                List<League> leagues = leagueRepository.findAll();

                if (leagues.isEmpty()) {
                        log.info("Automatic Biwenger sync skipped because no leagues are configured");
                        return;
                }

                for (League league : leagues) {
                        syncLeague(league.getId());
                }
        }

        private void syncLeague(Long leagueId) {

                try {
                        log.info(
                                        "Starting automatic Biwenger sync for league {}",
                                        leagueId);

                        SyncNowResponse response = syncExecutionService.syncScheduled(
                                        leagueId);

                        if (response.status() == SyncExecutionStatus.RUNNING
                                        && !response.started()) {

                                log.info(
                                                "Automatic Biwenger sync skipped for league {} because another sync is already running",
                                                leagueId);

                                return;
                        }

                        if (response.status() == SyncExecutionStatus.PARTIAL) {
                                log.warn(
                                                "Automatic Biwenger sync completed partially for league {}",
                                                leagueId);

                                return;
                        }

                        log.info(
                                        "Automatic Biwenger sync completed successfully for league {}",
                                        leagueId);

                } catch (Exception exception) {
                        log.error(
                                        "Automatic Biwenger sync failed for league {}",
                                        leagueId,
                                        exception);
                }
        }
}