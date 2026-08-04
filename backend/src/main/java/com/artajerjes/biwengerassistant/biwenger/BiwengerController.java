package com.artajerjes.biwengerassistant.biwenger;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.biwenger.dto.TestApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerLeagueApiResponse;


@RestController
@RequestMapping("/api/biwenger")
public class BiwengerController {
    private final BiwengerClient biwengerClient;

    public BiwengerController(BiwengerClient biwengerClient) {
        this.biwengerClient = biwengerClient;
    }

    @GetMapping("/test")
    public TestApiResponse testConnection() {
        return biwengerClient.testConnection();
    }

    @GetMapping("/league")
    public BiwengerLeagueApiResponse getLeague() {
        return biwengerClient.getLeague();
    }
}
