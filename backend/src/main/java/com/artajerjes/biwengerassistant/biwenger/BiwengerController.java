package com.artajerjes.biwengerassistant.biwenger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.biwenger.dto.TestApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerLeagueApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.sync.BiwengerSyncResponse;

@RestController
@RequestMapping("/api/biwenger")
public class BiwengerController {
    private final BiwengerClient biwengerClient;
    private final BiwengerSyncService biwengerSyncService;

    public BiwengerController(BiwengerClient biwengerClient, BiwengerSyncService biwengerSyncService) {
        this.biwengerClient = biwengerClient;
        this.biwengerSyncService = biwengerSyncService;
    }

    @GetMapping("/test")
    public TestApiResponse testConnection() {
        return biwengerClient.testConnection();
    }

    @GetMapping("/league")
    public BiwengerLeagueApiResponse getLeague() {
        return biwengerClient.getLeague();
    }

    @GetMapping("/competition")
    public BiwengerCompetitionResponse getCompetitionData() {
        return biwengerClient.getCompetition();
    }

    @PostMapping("/sync/{leagueId}")
    public BiwengerSyncResponse syncAll(
            @PathVariable Long leagueId) {
        return biwengerSyncService.syncAll(leagueId);
    }
}
