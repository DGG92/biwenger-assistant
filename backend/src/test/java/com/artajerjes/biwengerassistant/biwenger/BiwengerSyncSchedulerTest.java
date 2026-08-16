package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BiwengerSyncSchedulerTest {

        @Mock
        private BiwengerSyncService biwengerSyncService;

        @InjectMocks
        private BiwengerSyncScheduler biwengerSyncScheduler;

        @Test
        void syncShouldCallScheduledSyncForDefaultLeague() {
                biwengerSyncScheduler.sync();

                verify(biwengerSyncService)
                                .syncScheduled(1L);
        }

        @Test
        void syncShouldAbsorbUnexpectedExceptionAndNotPropagateIt() {
                doThrow(
                                new IllegalStateException(
                                                "Unexpected scheduled sync failure"))
                                .when(biwengerSyncService)
                                .syncScheduled(1L);

                assertDoesNotThrow(
                                () -> biwengerSyncScheduler.sync());

                verify(biwengerSyncService)
                                .syncScheduled(1L);
        }
}