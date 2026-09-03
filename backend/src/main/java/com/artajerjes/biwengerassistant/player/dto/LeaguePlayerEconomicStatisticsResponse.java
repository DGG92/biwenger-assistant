package com.artajerjes.biwengerassistant.player.dto;

import java.util.List;

import com.artajerjes.biwengerassistant.player.PlayerPosition;

public record LeaguePlayerEconomicStatisticsResponse(
        Long playerId,
        String name,
        List<PlayerPosition> positions,

        Long currentValue,

        Long value7DaysAgo,
        Long change7Days,
        Double changePercent7Days,

        Long purchasePrice,
        Long unrealizedProfit,
        Double unrealizedProfitPercent) {
}