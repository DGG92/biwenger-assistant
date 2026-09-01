package com.artajerjes.biwengerassistant.player.dto;

import java.util.List;

import com.artajerjes.biwengerassistant.player.PlayerPosition;

public record PlayerAnalyticsResponse(
                Long playerId,
                String name,
                List<PlayerPosition> positions,

                Long currentValue,
                Long value1DayAgo,
                Long value7DaysAgo,
                Long value30DaysAgo,

                Long change1Day,
                Long change7Days,
                Long change30Days,

                Double changePercent7Days,
                Double changePercent30Days,

                Long historicalMinValue,
                Long historicalMaxValue,

                Long purchasePrice,
                Long unrealizedProfit,
                Double unrealizedProfitPercent,

                String season,
                Integer totalPoints,
                Integer matchesPlayed,
                Double averagePoints,
                Double recentAveragePoints) {
}