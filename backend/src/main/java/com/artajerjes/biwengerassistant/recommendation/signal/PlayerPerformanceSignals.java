package com.artajerjes.biwengerassistant.recommendation.signal;

public record PlayerPerformanceSignals(
                double recentWeightedAverage,
                int recentSampleSize,
                boolean allRecentMatchesExcellent,
                double historicalAveragePoints,
                int historicalSampleSize,
                int recentObservedMatches) {

        /*
         * Constructor compatible con Motor 2.0.
         *
         * Mantiene los consumidores y tests existentes que todavía
         * construyen PlayerPerformanceSignals con cinco argumentos.
         */
        public PlayerPerformanceSignals(
                        double recentWeightedAverage,
                        int recentSampleSize,
                        boolean allRecentMatchesExcellent,
                        double historicalAveragePoints,
                        int historicalSampleSize) {

                this(
                                recentWeightedAverage,
                                recentSampleSize,
                                allRecentMatchesExcellent,
                                historicalAveragePoints,
                                historicalSampleSize,
                                recentSampleSize);
        }

        public boolean recentSignalAvailable() {
                return recentSampleSize >= 2;
        }

        public boolean historicalSignalAvailable() {
                return historicalSampleSize >= 5;
        }

        public boolean hasAnyPerformanceEvidence() {
                return recentObservedMatches > 0
                                || historicalSampleSize > 0;
        }
}