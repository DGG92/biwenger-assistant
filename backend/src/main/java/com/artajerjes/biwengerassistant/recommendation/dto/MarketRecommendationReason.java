package com.artajerjes.biwengerassistant.recommendation.dto;

public enum MarketRecommendationReason {
    PRICE_BELOW_MARKET,
    PRICE_ABOVE_MARKET,

    VALUE_RISING,
    VALUE_RISING_FAST,
    VALUE_FALLING,

    /*
     * Engine 2.1.
     *
     * Estas razones solo aparecen cuando la dinámica económica avanzada
     * modifica realmente la contribución valueTrend del Motor V1.
     */
    ECONOMIC_DYNAMICS_IMPROVING,
    ECONOMIC_DYNAMICS_WEAKENING,

    GOOD_RECENT_FORM,
    EXCELLENT_RECENT_FORM,

    /*
     * Engine 2.1.
     *
     * Distinguen el nivel absoluto de forma reciente de su evolución
     * frente al histórico del propio jugador.
     */
    RECENT_FORM_IMPROVING,
    RECENT_FORM_DECLINING,

    SQUAD_POSITION_NEEDED,

    INJURED,
    UNAFFORDABLE,

    STRONG_HISTORICAL_PERFORMANCE,
    POOR_HISTORICAL_PERFORMANCE,
}