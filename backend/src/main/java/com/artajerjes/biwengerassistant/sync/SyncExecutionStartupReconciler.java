package com.artajerjes.biwengerassistant.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SyncExecutionStartupReconciler {

    private static final Logger log = LoggerFactory.getLogger(
            SyncExecutionStartupReconciler.class);

    private final SyncExecutionStateService syncExecutionStateService;

    public SyncExecutionStartupReconciler(
            SyncExecutionStateService syncExecutionStateService) {

        this.syncExecutionStateService = syncExecutionStateService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void reconcileInterruptedExecutions() {

        int reconciled = syncExecutionStateService.failInterruptedExecutions();

        if (reconciled > 0) {
            log.warn(
                    "Marked {} interrupted sync execution(s) as FAILED after application startup",
                    reconciled);
        }
    }
}