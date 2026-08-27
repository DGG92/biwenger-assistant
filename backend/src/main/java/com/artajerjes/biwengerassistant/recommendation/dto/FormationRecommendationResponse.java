package com.artajerjes.biwengerassistant.recommendation.dto;

public record FormationRecommendationResponse(

        String currentFormation,

        String recommendedFormation,

        double currentScore,

        double recommendedScore,

        double improvement,

        int confidence) {
}