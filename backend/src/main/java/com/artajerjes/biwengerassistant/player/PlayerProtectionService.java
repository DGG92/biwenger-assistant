package com.artajerjes.biwengerassistant.player;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlert;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlertLevel;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionReason;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignalService;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignals;

@Service
public class PlayerProtectionService {

    private final PlayerPerformanceSignalService playerPerformanceSignalService;

    public PlayerProtectionService(
            PlayerPerformanceSignalService playerPerformanceSignalService) {

        this.playerPerformanceSignalService = playerPerformanceSignalService;
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
         * Estado de forma reciente.
         *
         * Utilizamos la señal común de rendimiento para que
         * PROTECT respete las mismas reglas que el motor
         * general de recomendaciones:
         *
         * - temporada actual
         * - jornadas consecutivas
         * - participación real
         * - mínimo 2 partidos
         */
        PlayerPerformanceSignals performance = playerPerformanceSignalService.analyze(player);

        if (performance.recentSampleSize() >= 2) {

            if (performance.allRecentMatchesExcellent()) {
                score += 30;

                reasons.add(
                        PlayerProtectionReason.EXCELLENT_RECENT_FORM);

            } else if (performance.recentWeightedAverage() >= 7) {
                score += 15;

                reasons.add(
                        PlayerProtectionReason.GOOD_RECENT_FORM);
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

    private PlayerProtectionAlert emptyAlert() {
        return new PlayerProtectionAlert(
                PlayerProtectionAlertLevel.NONE,
                0,
                List.of());
    }
}