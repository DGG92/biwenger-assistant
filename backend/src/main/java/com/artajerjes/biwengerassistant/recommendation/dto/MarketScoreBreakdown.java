package com.artajerjes.biwengerassistant.recommendation.dto;

public record MarketScoreBreakdown(
                double base,
                double price,

                /*
                 * Contribución económica final utilizada por el score.
                 *
                 * Se mantiene este campo por compatibilidad con Motor 2.0.
                 */
                double valueTrend,

                /*
                 * Engine 2.1 explainability.
                 *
                 * valueTrendBase representa la contribución que habría producido
                 * el algoritmo V1 usando únicamente la tendencia semanal/fallback.
                 *
                 * economicTrendCorrection es la corrección 2.1 efectiva después
                 * de momentum, aceleración, consistencia y el cap existente ±25.
                 *
                 * Por tanto:
                 *
                 * valueTrend = valueTrendBase + economicTrendCorrection
                 */
                double valueTrendBase,
                double economicTrendCorrection,

                /*
                 * Evidencia económica que explica la corrección 2.1.
                 * null significa que la señal no estaba disponible.
                 */
                Double marketMomentumPercentPerDay,
                Double valueAccelerationPercentPerDaySquared,
                Double economicTrendConsistency,

                double squadNeed,
                double recentForm,
                int recentFormSampleSize,
                double historicalAveragePoints,
                int historicalSampleSize,
                int historicalPerformance,

                /*
                 * Engine 2.1 sports explainability.
                 *
                 * sportsTrendCorrection es la contribución efectiva añadida
                 * al score por la evolución reciente frente al histórico.
                 */
                double sportsTrendCorrection,
                Double recentFormDelta,
                Double historicalConsistency,

                double status,
                double scoreBeforeCaps,
                boolean affordabilityCapApplied,
                boolean auctionBidCapApplied) {
}