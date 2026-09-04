package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.sync.SyncExecutionService;
import com.artajerjes.biwengerassistant.sync.SyncExecutionStatus;
import com.artajerjes.biwengerassistant.sync.SyncNowResponse;

@ExtendWith(MockitoExtension.class)
class BiwengerSyncSchedulerTest {

        @Mock
        private SyncExecutionService syncExecutionService;

        @InjectMocks
        private BiwengerSyncScheduler biwengerSyncScheduler;

        @Test
        void syncShouldExecuteSyncForDefaultLeague() {

                when(syncExecutionService.syncNow(1L))
                                .thenReturn(
                                                new SyncNowResponse(
                                                                1L,
                                                                true,
                                                                SyncExecutionStatus.SUCCESS));

                biwengerSyncScheduler.sync();

                verify(syncExecutionService)
                                .syncNow(1L);
        }

        @Test
        void syncShouldSkipWhenAnotherSyncIsAlreadyRunning() {

                when(syncExecutionService.syncNow(1L))
                                .thenReturn(
                                                new SyncNowResponse(
                                                                1L,
                                                                false,
                                                                SyncExecutionStatus.RUNNING));

                assertDoesNotThrow(
                                () -> biwengerSyncScheduler.sync());

                verify(syncExecutionService)
                                .syncNow(1L);
        }

        @Test
        void syncShouldAbsorbUnexpectedExceptionAndNotPropagateIt() {

                doThrow(
                                new IllegalStateException(
                                                "Unexpected scheduled sync failure"))
                                .when(syncExecutionService)
                                .syncNow(1L);

                assertDoesNotThrow(
                                () -> biwengerSyncScheduler.sync());

                verify(syncExecutionService)
                                .syncNow(1L);
        }
}