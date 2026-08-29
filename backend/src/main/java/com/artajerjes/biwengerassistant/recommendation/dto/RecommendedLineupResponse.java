package com.artajerjes.biwengerassistant.recommendation.dto;

import java.util.List;

public record RecommendedLineupResponse(
        String currentFormation,
        String recommendedFormation,
        double currentScore,
        double recommendedScore,
        double improvement,
        int confidence,
        List<RecommendedLineupPlayerResponse> recommendedStarters,
        List<RecommendedLineupChangeResponse> changes) {
}