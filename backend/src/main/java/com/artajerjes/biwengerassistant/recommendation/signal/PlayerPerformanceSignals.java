package com.artajerjes.biwengerassistant.recommendation.signal;

public record PlayerPerformanceSignals(
        double recentWeightedAverage,
        int recentSampleSize,
        boolean allRecentMatchesExcellent,
        double historicalAveragePoints,
        int historicalSampleSize) {
}