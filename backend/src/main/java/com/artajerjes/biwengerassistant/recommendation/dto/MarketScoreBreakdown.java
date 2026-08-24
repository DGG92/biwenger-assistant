package com.artajerjes.biwengerassistant.recommendation.dto;

public record MarketScoreBreakdown(
        double base,
        double price,
        double valueTrend,
        double squadNeed,
        double recentForm,
        int recentFormSampleSize,
        double historicalAveragePoints,
        int historicalSampleSize,
        int historicalPerformance,
        double status,
        double scoreBeforeCaps,
        boolean affordabilityCapApplied,
        boolean auctionBidCapApplied) {
}