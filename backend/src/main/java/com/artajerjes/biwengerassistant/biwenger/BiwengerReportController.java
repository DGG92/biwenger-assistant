package com.artajerjes.biwengerassistant.biwenger;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.biwenger.dto.report.BiwengerReportResponse;

@RestController
@RequestMapping("/api/biwenger/reports")
public class BiwengerReportController {

    private final BiwengerClient biwengerClient;

    public BiwengerReportController(BiwengerClient biwengerClient) {
        this.biwengerClient = biwengerClient;
    }

    @GetMapping
    public BiwengerReportResponse getReport(
            @RequestParam String report,
            @RequestParam(required = false) String param) {

        return biwengerClient.getReport(report, param);
    }
}