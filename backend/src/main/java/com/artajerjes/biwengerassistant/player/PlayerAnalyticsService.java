package com.artajerjes.biwengerassistant.player;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.history.PlayerPriceHistory;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.player.dto.PlayerAnalyticsResponse;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@Service
public class PlayerAnalyticsService {

        private static final int RECENT_MATCHES_LIMIT = 5;

        private final PlayerRepository playerRepository;
        private final PlayerPriceHistoryRepository playerPriceHistoryRepository;
        private final PlayerMatchReportRepository playerMatchReportRepository;

        public PlayerAnalyticsService(
                        PlayerRepository playerRepository,
                        PlayerPriceHistoryRepository playerPriceHistoryRepository,
                        PlayerMatchReportRepository playerMatchReportRepository) {

                this.playerRepository = playerRepository;
                this.playerPriceHistoryRepository = playerPriceHistoryRepository;
                this.playerMatchReportRepository = playerMatchReportRepository;
        }

        @Transactional(readOnly = true)
        public PlayerAnalyticsResponse getPlayerAnalytics(
                        Long leagueId,
                        Long playerId) {

                Player player = playerRepository
                                .findByIdAndLeague_Id(
                                                playerId,
                                                leagueId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Player not found in league"));

                LocalDate today = LocalDate.now();

                Long currentValue = player.getMarketValue();

                Long value1DayAgo = findHistoricalValue(
                                playerId,
                                today.minusDays(1));

                Long value7DaysAgo = findHistoricalValue(
                                playerId,
                                today.minusDays(7));

                Long value30DaysAgo = findHistoricalValue(
                                playerId,
                                today.minusDays(30));

                List<PlayerPriceHistory> priceHistory = playerPriceHistoryRepository
                                .findAllByPlayerIdOrderByPriceDateAsc(
                                                playerId);

                Long historicalMinValue = priceHistory.stream()
                                .map(PlayerPriceHistory::getMarketValue)
                                .min(Comparator.naturalOrder())
                                .orElse(currentValue);

                Long historicalMaxValue = priceHistory.stream()
                                .map(PlayerPriceHistory::getMarketValue)
                                .max(Comparator.naturalOrder())
                                .orElse(currentValue);

                Long distanceFromHistoricalMin = calculateAbsoluteChange(
                                historicalMinValue,
                                currentValue);

                Long distanceFromHistoricalMax = calculateAbsoluteChange(
                                historicalMaxValue,
                                currentValue);

                Long purchasePrice = player.getPurchasePrice();

                Long unrealizedProfit = player.getProfitability();

                Double unrealizedProfitPercent = calculatePercentageChange(
                                purchasePrice,
                                currentValue);

                List<PlayerMatchReport> reports = playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                playerId);

                String season = reports.stream()
                                .map(PlayerMatchReport::getSeason)
                                .filter(reportSeason -> reportSeason != null
                                                && !reportSeason.isBlank())
                                .findFirst()
                                .orElse(null);

                List<PlayerMatchReport> currentSeasonReports = season == null
                                ? List.of()
                                : reports.stream()
                                                .filter(report -> season.equals(report.getSeason()))
                                                .toList();

                int matchesPlayed = currentSeasonReports.size();

                int totalPoints = currentSeasonReports.stream()
                                .mapToInt(PlayerMatchReport::getPoints)
                                .sum();

                Double averagePoints = matchesPlayed == 0
                                ? null
                                : (double) totalPoints / matchesPlayed;

                List<PlayerMatchReport> recentReports = currentSeasonReports.stream()
                                .limit(RECENT_MATCHES_LIMIT)
                                .toList();

                Double recentAveragePoints = recentReports.isEmpty()
                                ? null
                                : recentReports.stream()
                                                .mapToInt(PlayerMatchReport::getPoints)
                                                .average()
                                                .orElse(0.0);

                Double recentFormDifference = averagePoints == null
                                || recentAveragePoints == null
                                                ? null
                                                : recentAveragePoints - averagePoints;

                Double pointsPerMillion = calculatePointsPerMillion(
                                totalPoints,
                                currentValue);

                return new PlayerAnalyticsResponse(
                                player.getId(),
                                player.getName(),
                                player.getPositions(),
                                currentValue,
                                value1DayAgo,
                                value7DaysAgo,
                                value30DaysAgo,
                                calculateAbsoluteChange(value1DayAgo, currentValue),
                                calculateAbsoluteChange(value7DaysAgo, currentValue),
                                calculateAbsoluteChange(value30DaysAgo, currentValue),
                                calculatePercentageChange(value1DayAgo, currentValue),
                                calculatePercentageChange(value7DaysAgo, currentValue),
                                calculatePercentageChange(value30DaysAgo, currentValue),
                                historicalMinValue,
                                historicalMaxValue,
                                distanceFromHistoricalMin,
                                distanceFromHistoricalMax,
                                purchasePrice,
                                unrealizedProfit,
                                unrealizedProfitPercent,
                                season,
                                totalPoints,
                                matchesPlayed,
                                averagePoints,
                                recentAveragePoints,
                                recentFormDifference,
                                pointsPerMillion);
        }

        private Long findHistoricalValue(
                        Long playerId,
                        LocalDate date) {

                return playerPriceHistoryRepository
                                .findTopByPlayerIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                                                playerId,
                                                date)
                                .map(PlayerPriceHistory::getMarketValue)
                                .orElse(null);
        }

        private Long calculateAbsoluteChange(
                        Long previousValue,
                        Long currentValue) {

                if (previousValue == null || currentValue == null) {
                        return null;
                }

                return currentValue - previousValue;
        }

        private Double calculatePercentageChange(
                        Long previousValue,
                        Long currentValue) {

                if (previousValue == null
                                || currentValue == null
                                || previousValue == 0) {

                        return null;
                }

                return ((double) (currentValue - previousValue)
                                / previousValue)
                                * 100.0;
        }

        private Double calculatePointsPerMillion(
                        Integer points,
                        Long marketValue) {

                if (points == null
                                || marketValue == null
                                || marketValue <= 0) {
                        return null;
                }

                return points / (marketValue / 1_000_000.0);
        }
}