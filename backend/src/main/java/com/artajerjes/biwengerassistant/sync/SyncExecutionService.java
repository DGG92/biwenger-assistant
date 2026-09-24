package com.artajerjes.biwengerassistant.sync;

import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.biwenger.BiwengerSyncService;
import com.artajerjes.biwengerassistant.biwenger.ScheduledSyncResult;

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

                        biwengerSyncService.syncAll(leagueId);

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

        public SyncNowResponse syncScheduled(
                        Long leagueId) {

                if (biwengerSyncService.isSyncRunning(leagueId)) {
                        return new SyncNowResponse(
                                        leagueId,
                                        false,
                                        SyncExecutionStatus.RUNNING);
                }

                syncExecutionStateService.markRunning(leagueId);

                try {

                        ScheduledSyncResult result = biwengerSyncService
                                        .syncScheduled(leagueId);

                        if (!result.started()) {
                                syncExecutionStateService.markIdle(leagueId);

                                return new SyncNowResponse(
                                                leagueId,
                                                false,
                                                SyncExecutionStatus.RUNNING);
                        }

                        if (result.partial()) {

                                String details = String.join(
                                                "; ",
                                                result.partialReasons());

                                syncExecutionStateService.markPartial(
                                                leagueId,
                                                details);

                                return new SyncNowResponse(
                                                leagueId,
                                                true,
                                                SyncExecutionStatus.PARTIAL);
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