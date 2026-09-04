package com.artajerjes.biwengerassistant.sync;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leagues/{leagueId}/sync")
public class SyncController {

    private final SyncStatusService syncStatusService;
    private final SyncExecutionService syncExecutionService;

    public SyncController(
            SyncStatusService syncStatusService,
            SyncExecutionService syncExecutionService) {

        this.syncStatusService = syncStatusService;
        this.syncExecutionService = syncExecutionService;
    }

    @GetMapping("/status")
    public SyncStatusResponse getStatus(
            @PathVariable Long leagueId) {

        return syncStatusService.getStatus(leagueId);
    }

    @PostMapping("/now")
    public SyncNowResponse syncNow(
            @PathVariable Long leagueId) {

        return syncExecutionService.syncNow(
                leagueId);
    }
}