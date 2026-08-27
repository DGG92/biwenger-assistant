package com.artajerjes.biwengerassistant.recommendation.action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.recommendation.RecommendationService;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignalService;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignals;

@Service
public class ActionRecommendationService {

    private final LeagueRepository leagueRepository;
    private final PlayerRepository playerRepository;
    private final RecommendationService recommendationService;
    private final PlayerPerformanceSignalService playerPerformanceSignalService;

    @Value("${biwenger.user-id}")
    private Long biwengerUserId;

    public ActionRecommendationService(
            LeagueRepository leagueRepository,
            PlayerRepository playerRepository,
            RecommendationService recommendationService,
            PlayerPerformanceSignalService playerPerformanceSignalService) {

        this.leagueRepository = leagueRepository;
        this.playerRepository = playerRepository;
        this.recommendationService = recommendationService;
        this.playerPerformanceSignalService = playerPerformanceSignalService;
    }

    @Transactional(readOnly = true)
    public List<ActionCandidate> getSquadActions(
            Long leagueId) {

        if (!leagueRepository.existsById(leagueId)) {
            throw new LeagueNotFoundException(leagueId);
        }

        List<Player> squadPlayers = playerRepository
                .findAllByLeague_Id(leagueId)
                .stream()
                .filter(player -> player.getOwner() != null)
                .filter(player -> biwengerUserId.equals(
                        player.getOwner()
                                .getBiwengerManagerId()))
                .toList();

        List<ActionCandidate> actions = new ArrayList<>();

        SquadNeedsResponse squadNeeds = recommendationService.getSquadNeeds(leagueId);

        for (Player player : squadPlayers) {
            ActionCandidate action = evaluatePlayer(
                    player,
                    squadNeeds);

            if (action != null) {
                actions.add(action);
            }
        }

        return actions.stream()
                .sorted(
                        Comparator.comparing(
                                ActionCandidate::priority))
                .toList();
    }

    private ActionCandidate evaluatePlayer(
            Player player,
            SquadNeedsResponse squadNeeds) {

        PlayerPerformanceSignals performance = playerPerformanceSignalService.analyze(player);

        long marketValue = player.getMarketValue() == null
                ? 0L
                : player.getMarketValue();

        long purchasePrice = player.getPurchasePrice() == null
                ? 0L
                : player.getPurchasePrice();

        long profit = marketValue - purchasePrice;

        double profitPercentage = purchasePrice <= 0
                ? 0
                : ((double) profit / purchasePrice) * 100;

        long dailyChange = player.getValueFluctuation() == null
                ? 0L
                : player.getValueFluctuation();

        double dailyChangePercentage = marketValue <= 0
                ? 0
                : ((double) dailyChange / marketValue) * 100;

        int positionNeed = player.getPositions() == null
                ? 0
                : player.getPositions()
                        .stream()
                        .filter(position -> !"E".equals(position.name()))
                        .mapToInt(position -> squadNeeds
                                .needScoreByPosition()
                                .getOrDefault(
                                        position.name(),
                                        0))
                        .max()
                        .orElse(0);

        List<String> signals = new ArrayList<>();

        int sellPressure = 0;
        int holdPressure = 0;

        /*
         * ECONOMÍA
         */

        if (dailyChangePercentage >= 1.0) {
            holdPressure += 3;
            signals.add("VALUE_RISING_FAST");

        } else if (dailyChange > 0) {
            holdPressure += 1;
            signals.add("VALUE_RISING");

        } else if (dailyChangePercentage <= -1.0) {
            sellPressure += 3;
            signals.add("VALUE_FALLING_FAST");

        } else if (dailyChange < 0) {
            sellPressure += 1;
            signals.add("VALUE_FALLING");
        }

        if (profitPercentage >= 20) {
            signals.add("HIGH_PROFIT");

            /*
             * Ojo:
             * tener beneficio NO empuja a vender.
             *
             * Solo sirve después para reforzar una venta
             * si ya existen señales negativas.
             */
        }

        if (profitPercentage <= -15) {
            signals.add("SIGNIFICANT_LOSS");
        }

        /*
         * RENDIMIENTO RECIENTE
         */

        if (performance.recentSampleSize() >= 2) {

            if (performance.recentWeightedAverage() >= 7) {
                holdPressure += 4;
                signals.add("RECENT_FORM_EXCELLENT");

            } else if (performance.recentWeightedAverage() >= 5) {
                holdPressure += 2;
                signals.add("RECENT_FORM_GOOD");

            } else if (performance.recentWeightedAverage() < 2) {
                sellPressure += 3;
                signals.add("RECENT_FORM_POOR");
            }

        } else {
            signals.add("RECENT_FORM_INSUFFICIENT_DATA");
        }

        /*
         * HISTÓRICO
         */

        if (performance.historicalSampleSize() >= 5) {

            if (performance.historicalAveragePoints() >= 6) {
                holdPressure += 3;
                signals.add("HISTORICAL_PERFORMANCE_STRONG");

            } else if (performance.historicalAveragePoints() >= 5) {
                holdPressure += 1;
                signals.add("HISTORICAL_PERFORMANCE_GOOD");

            } else if (performance.historicalAveragePoints() < 2) {
                sellPressure += 2;
                signals.add("HISTORICAL_PERFORMANCE_POOR");
            }
        }

        /*
         * CONTEXTO DE PLANTILLA
         */

        if (positionNeed >= 50) {
            holdPressure += 4;
            signals.add("POSITION_NEEDED");

        } else if (positionNeed == 0) {
            sellPressure += 1;
            signals.add("POSITION_WELL_COVERED");
        }

        if (player.isStarter()) {
            holdPressure += 2;
            signals.add("STARTER");
        }

        /*
         * DISPONIBILIDAD
         */

        PlayerStatus status = player.getStatus();

        if (status == PlayerStatus.INJURED
                || status == PlayerStatus.SANCTIONED
                || status == PlayerStatus.DISCARDED) {

            sellPressure += 2;
            signals.add("UNAVAILABLE");

        } else if (status == PlayerStatus.DOUBT) {

            sellPressure += 1;
            signals.add("DOUBT");
        }

        /*
         * La plusvalía solo aumenta la oportunidad de venta
         * cuando YA tenemos deterioro real.
         */
        if (profitPercentage >= 20
                && sellPressure >= 4) {

            sellPressure += 1;
            signals.add("PROFIT_CAN_BE_REALIZED");
        }

        int difference = holdPressure - sellPressure;

        if (sellPressure >= 5
                && difference <= -2) {

            return new ActionCandidate(
                    ActionType.SELL,
                    sellPressure >= 8
                            ? ActionPriority.HIGH
                            : ActionPriority.MEDIUM,
                    player.getId(),
                    player.getName(),
                    "Valora vender a " + player.getName(),
                    buildSellExplanation(
                            profit,
                            dailyChange,
                            performance,
                            positionNeed),
                    calculateConfidence(
                            performance,
                            Math.abs(difference)),
                    null,
                    List.copyOf(signals));
        }

        if (holdPressure >= 5
                && difference >= 2) {

            return new ActionCandidate(
                    ActionType.HOLD,
                    holdPressure >= 8
                            ? ActionPriority.HIGH
                            : ActionPriority.MEDIUM,
                    player.getId(),
                    player.getName(),
                    "Mantén a " + player.getName(),
                    buildHoldExplanation(
                            profit,
                            dailyChange,
                            performance,
                            positionNeed),
                    calculateConfidence(
                            performance,
                            Math.abs(difference)),
                    null,
                    List.copyOf(signals));
        }

        return new ActionCandidate(
                ActionType.WATCH,
                ActionPriority.LOW,
                player.getId(),
                player.getName(),
                "Sigue de cerca a " + player.getName(),
                "Las señales económicas, deportivas y de plantilla "
                        + "todavía no justifican una decisión clara.",
                calculateConfidence(
                        performance,
                        Math.abs(difference)),
                null,
                List.copyOf(signals));
    }

    private int calculateConfidence(
            PlayerPerformanceSignals performance,
            int decisionStrength) {

        int confidence = 35;

        if (performance.recentSampleSize() >= 2) {
            confidence += 20;
        }

        if (performance.recentSampleSize() >= 4) {
            confidence += 10;
        }

        if (performance.historicalSampleSize() >= 5) {
            confidence += 15;
        }

        confidence += Math.min(
                decisionStrength * 3,
                20);

        return Math.min(
                confidence,
                100);
    }

    private String buildSellExplanation(
            long profit,
            long dailyChange,
            PlayerPerformanceSignals performance,
            int positionNeed) {

        return "Acumula "
                + profit
                + " € de beneficio/pérdida, cambia "
                + dailyChange
                + " € al día, su forma reciente es "
                + round(performance.recentWeightedAverage())
                + " puntos y la necesidad de su posición es "
                + positionNeed
                + "/100.";
    }

    private String buildHoldExplanation(
            long profit,
            long dailyChange,
            PlayerPerformanceSignals performance,
            int positionNeed) {

        return "Mantenerlo sigue teniendo valor deportivo y económico: "
                + profit
                + " € de beneficio/pérdida, "
                + dailyChange
                + " € diarios y una forma reciente de "
                + round(performance.recentWeightedAverage())
                + " puntos.";
    }

    private double round(
            double value) {

        return Math.round(value * 100.0) / 100.0;
    }
}