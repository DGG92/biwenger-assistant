package com.artajerjes.biwengerassistant.sync;

import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.biwenger.BiwengerSyncService;

@Service
public class SyncExecutionService {

        private final BiwengerSyncService biwengerSyncService;
        private final SyncExecutionStateService syncExecutionStateService;

        public SyncExecutionService(
                        BiwengerSyncService biwengerSyncService,
                        SyncExecutionStateService syncExecutionStateService) {

                this.biwengerSyncService = biwengerSyncService;
                this.syncExecutionStateService = syncExecutionStateService;
        }

        public SyncNowResponse syncNow(
                        Long leagueId) {

                if (biwengerSyncService.isSyncRunning(leagueId)) {
                        return new SyncNowResponse(
                                        leagueId,
                                        false,
                                        SyncExecutionStatus.RUNNING);
                }

                syncExecutionStateService.markRunning(leagueId);

                try {

                        boolean started = biwengerSyncService
                                        .syncScheduled(leagueId);

                        if (!started) {
                                syncExecutionStateService.markIdle(leagueId);

                                return new SyncNowResponse(
                                                leagueId,
                                                false,
                                                SyncExecutionStatus.RUNNING);
                        }

                        syncExecutionStateService.markSuccess(leagueId);

                        return new SyncNowResponse(
                                        leagueId,
                                        true,
                                        SyncExecutionStatus.SUCCESS);

                } catch (Exception exception) {

                        syncExecutionStateService.markFailed(
                                        leagueId,
                                        exception.getMessage());

                        throw exception;
                }
        }
}