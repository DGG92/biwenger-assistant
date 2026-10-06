package com.artajerjes.biwengerassistant.recommendation.signal;

import java.time.LocalDate;

public record PlayerEconomicSignals(
        LocalDate latestPriceDate,
        int observedPrices,
        Double valueTrend7DaysPercent,
        Double marketMomentumPercentPerDay,
        Double valueAccelerationPercentPerDaySquared,
        Double trendConsistency) {

    public boolean valueTrendAvailable() {
        return valueTrend7DaysPercent != null;
    }

    public boolean momentumAvailable() {
        return marketMomentumPercentPerDay != null;
    }

    public boolean accelerationAvailable() {
        return valueAccelerationPercentPerDaySquared != null;
    }

    public boolean consistencyAvailable() {
        return trendConsistency != null;
    }

    public boolean hasAnyEconomicEvidence() {
        return observedPrices > 0;
    }
}
