package com.artajerjes.biwengerassistant.biwenger.dto.playerdetail;

import java.util.Map;

public record BiwengerPlayerReport(
        Boolean home,
        BiwengerPlayerMatch match,
        Map<String, Integer> points,
        Map<String, Object> rawStats,
        BiwengerPlayerReportStatus status) {
}