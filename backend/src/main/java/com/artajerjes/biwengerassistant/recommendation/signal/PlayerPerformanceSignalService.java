package com.artajerjes.biwengerassistant.recommendation.signal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@Service
public class PlayerPerformanceSignalService {

    private final PlayerMatchReportRepository playerMatchReportRepository;

    public PlayerPerformanceSignalService(
            PlayerMatchReportRepository playerMatchReportRepository) {

        this.playerMatchReportRepository = playerMatchReportRepository;
    }

    public PlayerPerformanceSignals analyze(
            Player player) {

        List<PlayerMatchReport> recentReports = playerMatchReportRepository
                .findTop5ByPlayer_IdOrderByMatchDateDesc(
                        player.getId());

        List<PlayerMatchReport> streak = buildCurrentConsecutiveStreak(
                recentReports);

        double recentWeightedAverage = 0;
        int recentSampleSize = 0;
        boolean allRecentMatchesExcellent = false;

        if (streak.size() >= 2) {
            recentSampleSize = streak.size();

            double weightedPoints = 0;
            int totalWeight = 0;

            for (int i = 0; i < streak.size(); i++) {
                PlayerMatchReport report = streak.get(i);

                int weight = streak.size() - i;

                weightedPoints += report.getPoints() * weight;

                totalWeight += weight;
            }

            recentWeightedAverage = totalWeight == 0
                    ? 0
                    : weightedPoints / totalWeight;

            allRecentMatchesExcellent = streak.stream()
                    .allMatch(
                            report -> report.getPoints() >= 8);
        }

        double historicalAveragePoints = 0;
        int historicalSampleSize = 0;

        /*
         * Los entrenadores utilizan otra escala de puntuación
         * (0 / 1 / 3), por lo que no son comparables con
         * PT / DF / MC / DL.
         */
        if (player.getPositions() == null
                || !player.getPositions()
                        .contains(PlayerPosition.E)) {

            List<PlayerMatchReport> historicalReports = playerMatchReportRepository
                    .findTop10ByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                            player.getId());

            if (historicalReports != null
                    && !historicalReports.isEmpty()) {

                historicalAveragePoints = historicalReports.stream()
                        .map(PlayerMatchReport::getPoints)
                        .mapToInt(Integer::intValue)
                        .average()
                        .orElse(0);

                historicalSampleSize = historicalReports.size();
            }
        }

        return new PlayerPerformanceSignals(
                recentWeightedAverage,
                recentSampleSize,
                allRecentMatchesExcellent,
                historicalAveragePoints,
                historicalSampleSize);
    }

    private List<PlayerMatchReport> buildCurrentConsecutiveStreak(
            List<PlayerMatchReport> recentReports) {

        if (recentReports == null
                || recentReports.isEmpty()) {
            return List.of();
        }

        String currentSeason = getCurrentSeason();

        List<PlayerMatchReport> streak = new ArrayList<>();

        for (PlayerMatchReport report : recentReports) {

            if (report.getSeason() == null
                    || !currentSeason.equals(
                            report.getSeason())) {
                break;
            }

            if (!report.isParticipated()
                    || report.getPoints() == null) {
                break;
            }

            streak.add(report);
        }

        return List.copyOf(streak);
    }

    private String getCurrentSeason() {
        LocalDate today = LocalDate.now();

        int year = today.getYear();

        if (today.getMonthValue() >= 7) {
            return year + "-" + (year + 1);
        }

        return (year - 1) + "-" + year;
    }
}