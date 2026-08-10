package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BiwengerSyncSchedulerTest {

    @Mock
    private BiwengerSyncService biwengerSyncService;

    @InjectMocks
    private BiwengerSyncScheduler biwengerSyncScheduler;

    @Test
    void syncShouldCallUnifiedSyncForDefaultLeague() {
        biwengerSyncScheduler.sync();

        verify(biwengerSyncService)
                .syncAll(1L);
    }

    @Test
    void syncShouldAbsorbExceptionAndNotPropagateIt() {
        doThrow(
                new IllegalStateException(
                        "Temporary Biwenger failure"))
                .when(biwengerSyncService)
                .syncAll(1L);

        assertDoesNotThrow(
                () -> biwengerSyncScheduler.sync());

        verify(biwengerSyncService)
                .syncAll(1L);
    }
}