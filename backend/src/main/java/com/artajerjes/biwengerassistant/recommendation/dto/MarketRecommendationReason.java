package com.artajerjes.biwengerassistant.recommendation.dto;

public enum MarketRecommendationReason {
    PRICE_BELOW_MARKET,
    PRICE_ABOVE_MARKET,

    VALUE_RISING,
    VALUE_RISING_FAST,
    VALUE_FALLING,

    GOOD_RECENT_FORM,
    EXCELLENT_RECENT_FORM,

    SQUAD_POSITION_NEEDED,

    INJURED,
    UNAFFORDABLE,

    STRONG_HISTORICAL_PERFORMANCE,
    POOR_HISTORICAL_PERFORMANCE,
}