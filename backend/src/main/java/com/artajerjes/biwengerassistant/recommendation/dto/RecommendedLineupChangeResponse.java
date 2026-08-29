package com.artajerjes.biwengerassistant.recommendation.dto;

public record RecommendedLineupChangeResponse(
        String type,
        Long playerId,
        String playerName) {
}