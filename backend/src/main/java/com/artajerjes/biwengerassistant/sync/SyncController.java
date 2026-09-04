package com.artajerjes.biwengerassistant.sync;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leagues/{leagueId}/sync")
public class SyncController {

    private final SyncStatusService syncStatusService;

    public SyncController(
            SyncStatusService syncStatusService) {

        this.syncStatusService = syncStatusService;
    }

    @GetMapping("/status")
    public SyncStatusResponse getStatus(
            @PathVariable Long leagueId) {

        return syncStatusService.getStatus(leagueId);
    }
}