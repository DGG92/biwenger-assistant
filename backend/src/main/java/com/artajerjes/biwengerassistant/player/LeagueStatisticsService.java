package com.artajerjes.biwengerassistant.player;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.history.PlayerPriceHistory;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.player.dto.LeaguePlayerEconomicStatisticsResponse;
import com.artajerjes.biwengerassistant.player.dto.LeaguePlayerStatisticsResponse;
import com.artajerjes.biwengerassistant.player.dto.LeagueStatisticsResponse;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@Service
public class LeagueStatisticsService {

        private static final int RANKING_LIMIT = 10;
        private static final int ECONOMIC_CHANGE_DAYS = 7;

        private final PlayerRepository playerRepository;
        private final PlayerMatchReportRepository playerMatchReportRepository;
        private final PlayerPriceHistoryRepository playerPriceHistoryRepository;

        public LeagueStatisticsService(
                        PlayerRepository playerRepository,
                        PlayerMatchReportRepository playerMatchReportRepository,
                        PlayerPriceHistoryRepository playerPriceHistoryRepository) {

                this.playerRepository = playerRepository;
                this.playerMatchReportRepository = playerMatchReportRepository;
                this.playerPriceHistoryRepository = playerPriceHistoryRepository;
        }

        @Transactional(readOnly = true)
        public LeagueStatisticsResponse getLeagueStatistics(Long leagueId) {

                List<Player> players = playerRepository.findAllByLeague_Id(leagueId);

                List<PlayerMatchReport> reports = playerMatchReportRepository
                                .findAllScoredReportsByLeague(leagueId);

                List<PlayerPriceHistory> priceHistory = playerPriceHistoryRepository
                                .findAllByLeagueIdOrderByPlayerAndPriceDate(leagueId);

                String season = reports.stream()
                                .map(PlayerMatchReport::getSeason)
                                .filter(reportSeason -> reportSeason != null
                                                && !reportSeason.isBlank())
                                .findFirst()
                                .orElse(null);

                Map<Long, Player> playersById = players.stream()
                                .collect(Collectors.toMap(
                                                Player::getId,
                                                player -> player));

                List<LeaguePlayerStatisticsResponse> statistics = buildSportsStatistics(
                                season,
                                reports,
                                playersById);

                int playersWithData = statistics.size();

                double coveragePercent = calculateCoverage(
                                playersWithData,
                                players.size());

                Map<Long, List<PlayerPriceHistory>> priceHistoryByPlayer = priceHistory.stream()
                                .collect(Collectors.groupingBy(
                                                PlayerPriceHistory::getPlayerId,
                                                LinkedHashMap::new,
                                                Collectors.toList()));

                List<LeaguePlayerEconomicStatisticsResponse> economicStatistics = players.stream()
                                .map(player -> toEconomicStatistics(
                                                player,
                                                priceHistoryByPlayer.getOrDefault(
                                                                player.getId(),
                                                                List.of())))
                                .toList();

                int playersWithPriceHistory = (int) players.stream()
                                .filter(player -> priceHistoryByPlayer.containsKey(
                                                player.getId()))
                                .count();

                double priceHistoryCoveragePercent = calculateCoverage(
                                playersWithPriceHistory,
                                players.size());

                List<LeaguePlayerStatisticsResponse> topPoints = statistics.stream()
                                .sorted(Comparator
                                                .comparingInt(
                                                                LeaguePlayerStatisticsResponse::totalPoints)
                                                .reversed()
                                                .thenComparing(
                                                                LeaguePlayerStatisticsResponse::playerId))
                                .limit(RANKING_LIMIT)
                                .toList();

                List<LeaguePlayerStatisticsResponse> topAverage = statistics.stream()
                                .sorted(Comparator
                                                .comparingDouble(
                                                                LeaguePlayerStatisticsResponse::averagePoints)
                                                .reversed()
                                                .thenComparing(
                                                                LeaguePlayerStatisticsResponse::playerId))
                                .limit(RANKING_LIMIT)
                                .toList();

                List<LeaguePlayerStatisticsResponse> topEfficiency = statistics.stream()
                                .filter(statistic -> statistic.pointsPerMillion() != null)
                                .sorted(Comparator
                                                .comparingDouble(
                                                                LeaguePlayerStatisticsResponse::pointsPerMillion)
                                                .reversed()
                                                .thenComparing(
                                                                LeaguePlayerStatisticsResponse::playerId))
                                .limit(RANKING_LIMIT)
                                .toList();

                List<LeaguePlayerEconomicStatisticsResponse> mostValuable = economicStatistics.stream()
                                .filter(statistic -> statistic.currentValue() != null)
                                .sorted(Comparator
                                                .comparingLong(
                                                                LeaguePlayerEconomicStatisticsResponse::currentValue)
                                                .reversed()
                                                .thenComparing(
                                                                LeaguePlayerEconomicStatisticsResponse::playerId))
                                .limit(RANKING_LIMIT)
                                .toList();

                List<LeaguePlayerEconomicStatisticsResponse> biggestRisers = economicStatistics.stream()
                                .filter(statistic -> statistic.changePercent7Days() != null)
                                .sorted(Comparator
                                                .comparingDouble(
                                                                LeaguePlayerEconomicStatisticsResponse::changePercent7Days)
                                                .reversed()
                                                .thenComparing(
                                                                LeaguePlayerEconomicStatisticsResponse::playerId))
                                .limit(RANKING_LIMIT)
                                .toList();

                List<LeaguePlayerEconomicStatisticsResponse> biggestFallers = economicStatistics.stream()
                                .filter(statistic -> statistic.changePercent7Days() != null)
                                .sorted(Comparator
                                                .comparingDouble(
                                                                LeaguePlayerEconomicStatisticsResponse::changePercent7Days)
                                                .thenComparing(
                                                                LeaguePlayerEconomicStatisticsResponse::playerId))
                                .limit(RANKING_LIMIT)
                                .toList();

                List<LeaguePlayerEconomicStatisticsResponse> bestInvestments = economicStatistics.stream()
                                .filter(statistic -> statistic.unrealizedProfit() != null)
                                .sorted(Comparator
                                                .comparingLong(
                                                                LeaguePlayerEconomicStatisticsResponse::unrealizedProfit)
                                                .reversed()
                                                .thenComparing(
                                                                LeaguePlayerEconomicStatisticsResponse::playerId))
                                .limit(RANKING_LIMIT)
                                .toList();

                List<LeaguePlayerEconomicStatisticsResponse> worstInvestments = economicStatistics.stream()
                                .filter(statistic -> statistic.unrealizedProfit() != null)
                                .sorted(Comparator
                                                .comparingLong(
                                                                LeaguePlayerEconomicStatisticsResponse::unrealizedProfit)
                                                .thenComparing(
                                                                LeaguePlayerEconomicStatisticsResponse::playerId))
                                .limit(RANKING_LIMIT)
                                .toList();

                return new LeagueStatisticsResponse(
                                leagueId,
                                season,
                                players.size(),
                                playersWithData,
                                coveragePercent,
                                topPoints,
                                topAverage,
                                topEfficiency,
                                playersWithPriceHistory,
                                priceHistoryCoveragePercent,
                                mostValuable,
                                biggestRisers,
                                biggestFallers,
                                bestInvestments,
                                worstInvestments);
        }

        private List<LeaguePlayerStatisticsResponse> buildSportsStatistics(
                        String season,
                        List<PlayerMatchReport> reports,
                        Map<Long, Player> playersById) {

                if (season == null) {
                        return List.of();
                }

                List<PlayerMatchReport> currentSeasonReports = reports.stream()
                                .filter(report -> season.equals(report.getSeason()))
                                .toList();

                Map<Long, List<PlayerMatchReport>> reportsByPlayer = new LinkedHashMap<>();

                for (PlayerMatchReport report : currentSeasonReports) {
                        reportsByPlayer
                                        .computeIfAbsent(
                                                        report.getPlayer().getId(),
                                                        ignored -> new java.util.ArrayList<>())
                                        .add(report);
                }

                return reportsByPlayer.entrySet().stream()
                                .map(entry -> toSportsStatistics(
                                                playersById.get(entry.getKey()),
                                                entry.getValue()))
                                .filter(Objects::nonNull)
                                .toList();
        }

        private LeaguePlayerStatisticsResponse toSportsStatistics(
                        Player player,
                        List<PlayerMatchReport> reports) {

                if (player == null || reports.isEmpty()) {
                        return null;
                }

                int matchesPlayed = reports.size();

                int totalPoints = reports.stream()
                                .mapToInt(PlayerMatchReport::getPoints)
                                .sum();

                double averagePoints = (double) totalPoints / matchesPlayed;

                Double pointsPerMillion = calculatePointsPerMillion(
                                totalPoints,
                                player.getMarketValue());

                return new LeaguePlayerStatisticsResponse(
                                player.getId(),
                                player.getName(),
                                player.getPositions(),
                                player.getMarketValue(),
                                totalPoints,
                                matchesPlayed,
                                averagePoints,
                                pointsPerMillion);
        }

        private LeaguePlayerEconomicStatisticsResponse toEconomicStatistics(
                        Player player,
                        List<PlayerPriceHistory> history) {

                Long currentValue = player.getMarketValue();

                LocalDate targetDate = LocalDate.now()
                                .minusDays(ECONOMIC_CHANGE_DAYS);

                Long value7DaysAgo = history.stream()
                                .filter(price -> price.getPriceDate() != null
                                                && !price.getPriceDate()
                                                                .isAfter(targetDate))
                                .max(Comparator.comparing(
                                                PlayerPriceHistory::getPriceDate))
                                .map(PlayerPriceHistory::getMarketValue)
                                .orElse(null);

                Long change7Days = calculateChange(
                                currentValue,
                                value7DaysAgo);

                Double changePercent7Days = calculateChangePercent(
                                currentValue,
                                value7DaysAgo);

                Long purchasePrice = player.getPurchasePrice();

                Long unrealizedProfit = calculateProfit(
                                currentValue,
                                purchasePrice);

                Double unrealizedProfitPercent = calculateProfitPercent(
                                unrealizedProfit,
                                purchasePrice);

                return new LeaguePlayerEconomicStatisticsResponse(
                                player.getId(),
                                player.getName(),
                                player.getPositions(),
                                currentValue,
                                value7DaysAgo,
                                change7Days,
                                changePercent7Days,
                                purchasePrice,
                                unrealizedProfit,
                                unrealizedProfitPercent);
        }

        private Long calculateChange(
                        Long currentValue,
                        Long previousValue) {

                if (currentValue == null || previousValue == null) {
                        return null;
                }

                return currentValue - previousValue;
        }

        private Double calculateChangePercent(
                        Long currentValue,
                        Long previousValue) {

                if (currentValue == null
                                || previousValue == null
                                || previousValue <= 0) {

                        return null;
                }

                return ((double) (currentValue - previousValue)
                                / previousValue) * 100.0;
        }

        private Long calculateProfit(
                        Long currentValue,
                        Long purchasePrice) {

                if (currentValue == null
                                || purchasePrice == null
                                || purchasePrice <= 0) {

                        return null;
                }

                return currentValue - purchasePrice;
        }

        private Double calculateProfitPercent(
                        Long unrealizedProfit,
                        Long purchasePrice) {

                if (unrealizedProfit == null
                                || purchasePrice == null
                                || purchasePrice <= 0) {

                        return null;
                }

                return ((double) unrealizedProfit
                                / purchasePrice) * 100.0;
        }

        private Double calculatePointsPerMillion(
                        Integer points,
                        Long marketValue) {

                if (marketValue == null || marketValue <= 0) {
                        return null;
                }

                return points
                                / (marketValue / 1_000_000.0);
        }

        private double calculateCoverage(
                        int playersWithData,
                        int totalPlayers) {

                if (totalPlayers == 0) {
                        return 0.0;
                }

                return ((double) playersWithData
                                / totalPlayers) * 100.0;
        }
}