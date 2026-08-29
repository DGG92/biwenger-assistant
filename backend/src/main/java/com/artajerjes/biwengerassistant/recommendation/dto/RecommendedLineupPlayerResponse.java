package com.artajerjes.biwengerassistant.recommendation.dto;

public record RecommendedLineupPlayerResponse(
        Long playerId,
        String playerName,
        String position,
        double rating) {
}