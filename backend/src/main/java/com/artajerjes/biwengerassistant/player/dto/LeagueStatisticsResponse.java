package com.artajerjes.biwengerassistant.player.dto;

import java.util.List;

public record LeagueStatisticsResponse(
                Long leagueId,
                String season,

                Integer players,
                Integer playersWithData,
                Double coveragePercent,

                List<LeaguePlayerStatisticsResponse> topPoints,
                List<LeaguePlayerStatisticsResponse> topAverage,
                List<LeaguePlayerStatisticsResponse> topEfficiency,

                Integer playersWithPriceHistory,
                Double priceHistoryCoveragePercent,

                List<LeaguePlayerEconomicStatisticsResponse> mostValuable,
                List<LeaguePlayerEconomicStatisticsResponse> biggestRisers,
                List<LeaguePlayerEconomicStatisticsResponse> biggestFallers,
                List<LeaguePlayerEconomicStatisticsResponse> bestInvestments,
                List<LeaguePlayerEconomicStatisticsResponse> worstInvestments) {
}