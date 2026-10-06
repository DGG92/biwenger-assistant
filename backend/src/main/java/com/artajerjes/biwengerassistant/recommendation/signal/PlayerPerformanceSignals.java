package com.artajerjes.biwengerassistant.recommendation.signal;

public record PlayerPerformanceSignals(
                double recentWeightedAverage,
                int recentSampleSize,
                boolean allRecentMatchesExcellent,
                double historicalAveragePoints,
                int historicalSampleSize,
                int recentObservedMatches,
                Double recentFormDelta,
                Double historicalConsistency) {

        private static final int MIN_RECENT_SAMPLE_SIZE = 2;
        private static final int MIN_HISTORICAL_SAMPLE_SIZE = 5;

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
                                recentSampleSize,
                                null,
                                null);
        }

        /*
         * Constructor compatible con Engine 2.1 - sparse data.
         *
         * Mantiene los consumidores introducidos antes de añadir
         * las señales deportivas avanzadas de 14.C.
         */
        public PlayerPerformanceSignals(
                        double recentWeightedAverage,
                        int recentSampleSize,
                        boolean allRecentMatchesExcellent,
                        double historicalAveragePoints,
                        int historicalSampleSize,
                        int recentObservedMatches) {

                this(
                                recentWeightedAverage,
                                recentSampleSize,
                                allRecentMatchesExcellent,
                                historicalAveragePoints,
                                historicalSampleSize,
                                recentObservedMatches,
                                null,
                                null);
        }

        public boolean recentSignalAvailable() {
                return recentSampleSize >= MIN_RECENT_SAMPLE_SIZE;
        }

        public boolean historicalSignalAvailable() {
                return historicalSampleSize >= MIN_HISTORICAL_SAMPLE_SIZE;
        }

        public boolean recentFormDeltaAvailable() {
                return recentFormDelta != null;
        }

        public boolean historicalConsistencyAvailable() {
                return historicalConsistency != null;
        }

        public boolean hasAnyPerformanceEvidence() {
                return recentObservedMatches > 0
                                || historicalSampleSize > 0;
        }
}