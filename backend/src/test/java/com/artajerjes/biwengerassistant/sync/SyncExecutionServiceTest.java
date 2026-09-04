package com.artajerjes.biwengerassistant.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artajerjes.biwengerassistant.biwenger.BiwengerSyncService;

class SyncExecutionServiceTest {

        private static final Long LEAGUE_ID = 1L;

        @Test
        void syncNowShouldReturnSuccessWhenSyncStarts() {

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                when(biwengerSyncService.isSyncRunning(LEAGUE_ID))
                                .thenReturn(false);

                when(biwengerSyncService.syncScheduled(LEAGUE_ID))
                                .thenReturn(true);

                SyncExecutionService service = new SyncExecutionService(
                                biwengerSyncService,
                                syncExecutionStateService);

                SyncNowResponse response = service.syncNow(LEAGUE_ID);

                assertThat(response.leagueId())
                                .isEqualTo(LEAGUE_ID);

                assertThat(response.started())
                                .isTrue();

                assertThat(response.status())
                                .isEqualTo(SyncExecutionStatus.SUCCESS);

                verify(syncExecutionStateService)
                                .markRunning(LEAGUE_ID);

                verify(syncExecutionStateService)
                                .markSuccess(LEAGUE_ID);

                verify(syncExecutionStateService, never())
                                .markFailed(
                                                org.mockito.ArgumentMatchers.anyLong(),
                                                org.mockito.ArgumentMatchers.any());

                verify(biwengerSyncService)
                                .syncScheduled(LEAGUE_ID);
        }

        @Test
        void syncNowShouldReturnRunningWhenSyncAlreadyRunning() {

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                when(biwengerSyncService.isSyncRunning(LEAGUE_ID))
                                .thenReturn(true);

                SyncExecutionService service = new SyncExecutionService(
                                biwengerSyncService,
                                syncExecutionStateService);

                SyncNowResponse response = service.syncNow(LEAGUE_ID);

                assertThat(response.leagueId())
                                .isEqualTo(LEAGUE_ID);

                assertThat(response.started())
                                .isFalse();

                assertThat(response.status())
                                .isEqualTo(SyncExecutionStatus.RUNNING);

                verify(biwengerSyncService, never())
                                .syncScheduled(LEAGUE_ID);

                verify(syncExecutionStateService, never())
                                .markRunning(LEAGUE_ID);

                verify(syncExecutionStateService, never())
                                .markSuccess(LEAGUE_ID);
        }

        @Test
        void shouldResetExecutionStateWhenSyncCannotStart() {

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                when(biwengerSyncService.isSyncRunning(LEAGUE_ID))
                                .thenReturn(false);

                when(biwengerSyncService.syncScheduled(LEAGUE_ID))
                                .thenReturn(false);

                SyncExecutionService service = new SyncExecutionService(
                                biwengerSyncService,
                                syncExecutionStateService);

                SyncNowResponse response = service.syncNow(LEAGUE_ID);

                assertEquals(LEAGUE_ID, response.leagueId());
                assertFalse(response.started());
                assertEquals(
                                SyncExecutionStatus.RUNNING,
                                response.status());

                verify(syncExecutionStateService)
                                .markRunning(LEAGUE_ID);

                verify(syncExecutionStateService)
                                .markIdle(LEAGUE_ID);

                verify(syncExecutionStateService, never())
                                .markSuccess(LEAGUE_ID);

                verify(syncExecutionStateService, never())
                                .markFailed(
                                                eq(LEAGUE_ID),
                                                anyString());
        }

        @Test
        void syncNowShouldMarkFailedWhenSyncThrowsException() {

                BiwengerSyncService biwengerSyncService = mock(BiwengerSyncService.class);

                SyncExecutionStateService syncExecutionStateService = mock(SyncExecutionStateService.class);

                when(biwengerSyncService.isSyncRunning(LEAGUE_ID))
                                .thenReturn(false);

                when(biwengerSyncService.syncScheduled(LEAGUE_ID))
                                .thenThrow(new IllegalStateException(
                                                "Biwenger unavailable"));

                SyncExecutionService service = new SyncExecutionService(
                                biwengerSyncService,
                                syncExecutionStateService);

                assertThatThrownBy(() -> service.syncNow(LEAGUE_ID))
                                .isInstanceOf(IllegalStateException.class)
                                .hasMessage("Biwenger unavailable");

                verify(syncExecutionStateService)
                                .markRunning(LEAGUE_ID);

                verify(syncExecutionStateService)
                                .markFailed(
                                                LEAGUE_ID,
                                                "Biwenger unavailable");

                verify(syncExecutionStateService, never())
                                .markSuccess(LEAGUE_ID);
        }
}