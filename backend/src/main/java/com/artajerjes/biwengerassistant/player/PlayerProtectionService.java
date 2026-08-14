package com.artajerjes.biwengerassistant.player;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlert;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlertLevel;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionReason;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@Service
public class PlayerProtectionService {

    private final PlayerMatchReportRepository playerMatchReportRepository;

    public PlayerProtectionService(
            PlayerMatchReportRepository playerMatchReportRepository) {
        this.playerMatchReportRepository = playerMatchReportRepository;
    }

    public PlayerProtectionAlert calculate(Player player) {
        /*
         * Solo tiene sentido recomendar proteger jugadores
         * que pertenecen a algún manager.
         */
        if (player.getOwner() == null) {
            return emptyAlert();
        }

        int score = 0;

        List<PlayerProtectionReason> reasons = new ArrayList<>();

        /*
         * Evolución económica diaria.
         *
         * Es el factor con más peso individual.
         */
        Long fluctuation = player.getValueFluctuation();

        if (fluctuation != null) {
            if (fluctuation >= 100_000L) {
                score += 40;
                reasons.add(
                        PlayerProtectionReason.VALUE_RISING_FAST);
            } else if (fluctuation >= 50_000L) {
                score += 25;
                reasons.add(
                        PlayerProtectionReason.VALUE_RISING);
            }
        }

        /*
         * Estado de forma de la temporada actual.
         */
        List<PlayerMatchReport> recentReports = playerMatchReportRepository
                .findTop2ByPlayer_IdOrderByMatchDateDesc(
                        player.getId());

        if (hasValidRecentForm(recentReports)) {
            int latestPoints = recentReports.get(0).getPoints();

            int previousPoints = recentReports.get(1).getPoints();

            if (latestPoints >= 8
                    && previousPoints >= 8) {
                score += 30;

                reasons.add(
                        PlayerProtectionReason.EXCELLENT_RECENT_FORM);
            } else {
                double average = (latestPoints + previousPoints) / 2.0;

                if (average >= 7) {
                    score += 15;

                    reasons.add(
                            PlayerProtectionReason.GOOD_RECENT_FORM);
                }
            }
        }

        /*
         * Rentabilidad obtenida desde la compra.
         */
        Long profitability = player.getProfitability();

        if (profitability != null) {
            if (profitability >= 500_000L) {
                score += 15;

                reasons.add(
                        PlayerProtectionReason.HIGH_PROFITABILITY);
            } else if (profitability >= 200_000L) {
                score += 8;

                reasons.add(
                        PlayerProtectionReason.HIGH_PROFITABILITY);
            }
        }

        /*
         * Una lesión reduce bastante la urgencia de subir cláusula.
         */
        if (player.getStatus() == PlayerStatus.INJURED) {
            score -= 25;

            reasons.add(
                    PlayerProtectionReason.INJURED);
        }

        score = Math.max(
                0,
                Math.min(100, score));

        PlayerProtectionAlertLevel level;

        if (score >= 60) {
            level = PlayerProtectionAlertLevel.PROTECT;
        } else if (score >= 30) {
            level = PlayerProtectionAlertLevel.WATCH;
        } else {
            level = PlayerProtectionAlertLevel.NONE;
        }

        return new PlayerProtectionAlert(
                level,
                score,
                List.copyOf(reasons));
    }

    private boolean hasValidRecentForm(
            List<PlayerMatchReport> reports) {

        if (reports == null
                || reports.size() < 2) {
            return false;
        }

        PlayerMatchReport latest = reports.get(0);
        PlayerMatchReport previous = reports.get(1);

        if (latest.getSeason() == null
                || previous.getSeason() == null) {
            return false;
        }

        String currentSeason = getCurrentSeason();

        if (!currentSeason.equals(latest.getSeason())
                || !currentSeason.equals(previous.getSeason())) {
            return false;
        }

        if (!latest.isParticipated()
                || !previous.isParticipated()) {
            return false;
        }

        return latest.getPoints() != null
                && previous.getPoints() != null;
    }

    private String getCurrentSeason() {
        LocalDate today = LocalDate.now();

        int year = today.getYear();

        if (today.getMonthValue() >= 7) {
            return year + "-" + (year + 1);
        }

        return (year - 1) + "-" + year;
    }

    private PlayerProtectionAlert emptyAlert() {
        return new PlayerProtectionAlert(
                PlayerProtectionAlertLevel.NONE,
                0,
                List.of());
    }
}