package com.artajerjes.biwengerassistant.sync;

import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncExecutionStartupReconcilerTest {

    @Test
    void shouldReconcileInterruptedExecutionsOnStartup() {

        SyncExecutionStateService service = mock(SyncExecutionStateService.class);

        when(service.failInterruptedExecutions())
                .thenReturn(2);

        SyncExecutionStartupReconciler reconciler = new SyncExecutionStartupReconciler(service);

        reconciler.reconcileInterruptedExecutions();

        verify(service)
                .failInterruptedExecutions();
    }

    @Test
    void shouldRunEvenWhenThereAreNoInterruptedExecutions() {

        SyncExecutionStateService service = mock(SyncExecutionStateService.class);

        when(service.failInterruptedExecutions())
                .thenReturn(0);

        SyncExecutionStartupReconciler reconciler = new SyncExecutionStartupReconciler(service);

        reconciler.reconcileInterruptedExecutions();

        verify(service)
                .failInterruptedExecutions();
    }
}