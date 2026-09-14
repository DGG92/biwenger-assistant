package com.artajerjes.biwengerassistant.recommendation.dto;

import java.util.List;

import com.artajerjes.biwengerassistant.recommendation.action.ActionCandidate;

public record RecommendationOverviewResponse(
        List<MarketRecommendationResponse> market,
        List<ActionCandidate> actions) {
}