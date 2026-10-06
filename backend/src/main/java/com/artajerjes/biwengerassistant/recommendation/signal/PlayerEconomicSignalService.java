package com.artajerjes.biwengerassistant.recommendation.signal;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.history.PlayerPriceHistory;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;

@Service
public class PlayerEconomicSignalService {

    private static final int HISTORY_LOOKBACK_DAYS = 8;
    private static final int TREND_DAYS = 7;
    private static final int VELOCITY_WINDOW_DAYS = 3;
    private static final int MAX_ANCHOR_DRIFT_DAYS = 1;

    private final PlayerPriceHistoryRepository playerPriceHistoryRepository;

    public PlayerEconomicSignalService(
            PlayerPriceHistoryRepository playerPriceHistoryRepository) {

        this.playerPriceHistoryRepository = playerPriceHistoryRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, PlayerEconomicSignals> analyzeLeague(Long leagueId) {

        LocalDate fromDate = LocalDate.now()
                .minusDays(HISTORY_LOOKBACK_DAYS);

        List<PlayerPriceHistory> history = playerPriceHistoryRepository
                .findRecentPricesByLeagueId(
                        leagueId,
                        fromDate);

        Map<Long, List<PlayerPriceHistory>> byPlayer = new LinkedHashMap<>();

        for (PlayerPriceHistory price : history) {

            if (price == null
                    || price.getPlayerId() == null) {
                continue;
            }

            byPlayer.computeIfAbsent(
                            price.getPlayerId(),
                            ignored -> new ArrayList<>())
                    .add(price);
        }

        Map<Long, PlayerEconomicSignals> result = new LinkedHashMap<>();

        byPlayer.forEach(
                (playerId, prices) -> result.put(
                        playerId,
                        analyzeHistory(prices)));

        return Map.copyOf(result);
    }

    public PlayerEconomicSignals analyzeHistory(
            List<PlayerPriceHistory> history) {

        if (history == null || history.isEmpty()) {
            return new PlayerEconomicSignals(
                    null,
                    0,
                    null,
                    null,
                    null,
                    null);
        }

        List<PlayerPriceHistory> validHistory = history.stream()
                .filter(this::isValidPrice)
                .sorted((first, second) -> first.getPriceDate()
                        .compareTo(second.getPriceDate()))
                .toList();

        if (validHistory.isEmpty()) {
            return new PlayerEconomicSignals(
                    null,
                    0,
                    null,
                    null,
                    null,
                    null);
        }

        PlayerPriceHistory latest = validHistory.get(
                validHistory.size() - 1);

        LocalDate latestDate = latest.getPriceDate();

        PlayerPriceHistory trendAnchor = findAnchor(
                validHistory,
                latestDate.minusDays(TREND_DAYS));

        PlayerPriceHistory recentVelocityAnchor = findAnchor(
                validHistory,
                latestDate.minusDays(VELOCITY_WINDOW_DAYS));

        PlayerPriceHistory previousVelocityAnchor = findAnchor(
                validHistory,
                latestDate.minusDays(
                        VELOCITY_WINDOW_DAYS * 2));

        Double valueTrend = calculatePercentageChange(
                trendAnchor,
                latest);

        Double recentVelocity = calculateVelocity(
                recentVelocityAnchor,
                latest);

        Double previousVelocity = calculateVelocity(
                previousVelocityAnchor,
                recentVelocityAnchor);

        Double acceleration = calculateAcceleration(
                previousVelocity,
                recentVelocity);

        Double consistency = calculateTrendConsistency(
                validHistory,
                latestDate,
                valueTrend);

        return new PlayerEconomicSignals(
                latestDate,
                validHistory.size(),
                valueTrend,
                recentVelocity,
                acceleration,
                consistency);
    }

    private boolean isValidPrice(
            PlayerPriceHistory price) {

        return price != null
                && price.getPriceDate() != null
                && price.getMarketValue() != null
                && price.getMarketValue() > 0;
    }

    private PlayerPriceHistory findAnchor(
            List<PlayerPriceHistory> history,
            LocalDate targetDate) {

        PlayerPriceHistory candidate = null;

        for (PlayerPriceHistory price : history) {

            if (price.getPriceDate().isAfter(targetDate)) {
                break;
            }

            candidate = price;
        }

        if (candidate == null) {
            return null;
        }

        long driftDays = ChronoUnit.DAYS.between(
                candidate.getPriceDate(),
                targetDate);

        if (driftDays < 0
                || driftDays > MAX_ANCHOR_DRIFT_DAYS) {
            return null;
        }

        return candidate;
    }

    private Double calculatePercentageChange(
            PlayerPriceHistory start,
            PlayerPriceHistory end) {

        if (start == null || end == null) {
            return null;
        }

        long startValue = start.getMarketValue();
        long endValue = end.getMarketValue();

        if (startValue <= 0) {
            return null;
        }

        return ((double) (endValue - startValue)
                / startValue) * 100.0;
    }

    private Double calculateVelocity(
            PlayerPriceHistory start,
            PlayerPriceHistory end) {

        if (start == null || end == null) {
            return null;
        }

        long days = ChronoUnit.DAYS.between(
                start.getPriceDate(),
                end.getPriceDate());

        if (days <= 0) {
            return null;
        }

        Double change = calculatePercentageChange(
                start,
                end);

        if (change == null) {
            return null;
        }

        return change / days;
    }

    private Double calculateAcceleration(
            Double previousVelocity,
            Double recentVelocity) {

        if (previousVelocity == null
                || recentVelocity == null) {
            return null;
        }

        return (recentVelocity - previousVelocity)
                / VELOCITY_WINDOW_DAYS;
    }

    private Double calculateTrendConsistency(
            List<PlayerPriceHistory> history,
            LocalDate latestDate,
            Double valueTrend) {

        if (valueTrend == null) {
            return null;
        }

        LocalDate fromDate = latestDate.minusDays(
                TREND_DAYS);

        List<PlayerPriceHistory> window = history.stream()
                .filter(price -> !price.getPriceDate()
                        .isBefore(fromDate))
                .filter(price -> !price.getPriceDate()
                        .isAfter(latestDate))
                .toList();

        if (window.size() < 4) {
            return null;
        }

        int matchingIntervals = 0;
        int totalIntervals = 0;

        int overallDirection = direction(valueTrend);

        for (int index = 1; index < window.size(); index++) {

            PlayerPriceHistory previous = window.get(
                    index - 1);

            PlayerPriceHistory current = window.get(index);

            if (!current.getPriceDate().isAfter(
                    previous.getPriceDate())) {
                continue;
            }

            long difference = current.getMarketValue()
                    - previous.getMarketValue();

            int intervalDirection = Long.compare(
                    difference,
                    0L);

            totalIntervals++;

            if (overallDirection == 0) {

                if (intervalDirection == 0) {
                    matchingIntervals++;
                }

            } else if (intervalDirection == overallDirection) {

                matchingIntervals++;
            }
        }

        if (totalIntervals < 3) {
            return null;
        }

        return (double) matchingIntervals
                / totalIntervals;
    }

    private int direction(double value) {

        double epsilon = 0.000001;

        if (value > epsilon) {
            return 1;
        }

        if (value < -epsilon) {
            return -1;
        }

        return 0;
    }
}
