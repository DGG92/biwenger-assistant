package com.artajerjes.biwengerassistant.recommendation.action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.market.MarketListingType;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerProtectionService;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlert;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlertLevel;
import com.artajerjes.biwengerassistant.recommendation.RecommendationService;
import com.artajerjes.biwengerassistant.recommendation.RecommendationType;
import com.artajerjes.biwengerassistant.recommendation.dto.FormationRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignalService;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignals;

@Service
public class ActionRecommendationService {

        private final LeagueRepository leagueRepository;
        private final PlayerRepository playerRepository;
        private final RecommendationService recommendationService;
        private final PlayerPerformanceSignalService playerPerformanceSignalService;
        private final PlayerProtectionService playerProtectionService;

        @Value("${biwenger.user-id}")
        private Long biwengerUserId;

        public ActionRecommendationService(
                        LeagueRepository leagueRepository,
                        PlayerRepository playerRepository,
                        RecommendationService recommendationService,
                        PlayerPerformanceSignalService playerPerformanceSignalService,
                        PlayerProtectionService playerProtectionService) {

                this.leagueRepository = leagueRepository;
                this.playerRepository = playerRepository;
                this.recommendationService = recommendationService;
                this.playerPerformanceSignalService = playerPerformanceSignalService;
                this.playerProtectionService = playerProtectionService;
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

                        actions.addAll(
                                        evaluatePlayerActions(
                                                        player,
                                                        squadNeeds));
                }

                actions.addAll(
                                evaluateStarterReplacementActions(
                                                squadPlayers));

                ActionCandidate formationAction = evaluateFormationChange(
                                leagueId);

                if (formationAction != null) {
                        actions.add(formationAction);
                }

                return actions.stream()
                                .sorted(
                                                Comparator.comparing(
                                                                ActionCandidate::priority))
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ActionCandidate> getMarketActions(
                        Long leagueId) {

                return recommendationService
                                .getMarketRecommendations(leagueId)
                                .stream()
                                .map(this::evaluateMarketAction)
                                .filter(java.util.Objects::nonNull)
                                .sorted(
                                                Comparator.comparing(
                                                                ActionCandidate::priority))
                                .toList();
        }

        @Transactional(readOnly = true)
        public List<ActionCandidate> getAllActions(
                        Long leagueId) {

                List<ActionCandidate> actions = new ArrayList<>();

                actions.addAll(
                                getSquadActions(leagueId));

                actions.addAll(
                                getMarketActions(leagueId));

                return actions.stream()
                                .sorted(
                                                Comparator.comparing(
                                                                ActionCandidate::priority)
                                                                .thenComparing(
                                                                                ActionCandidate::confidence,
                                                                                Comparator.nullsLast(
                                                                                                Comparator.reverseOrder())))
                                .toList();
        }

        private ActionCandidate evaluateMarketAction(
                        MarketRecommendationResponse recommendation) {

                if (recommendation.recommendation() != RecommendationType.STRONG_BUY
                                && recommendation.recommendation() != RecommendationType.BUY) {

                        return null;
                }

                ActionType actionType = recommendation.marketType() == MarketListingType.AUCTION
                                ? ActionType.BID
                                : ActionType.BUY;

                ActionPriority priority = recommendation.recommendation() == RecommendationType.STRONG_BUY
                                ? ActionPriority.HIGH
                                : ActionPriority.MEDIUM;

                Long suggestedAmount = actionType == ActionType.BID
                                ? recommendation.maximumRecommendedBid()
                                : recommendation.askingPrice();

                String title = actionType == ActionType.BID
                                ? "Puja por "
                                                + recommendation.playerName()
                                : "Valora comprar a "
                                                + recommendation.playerName();

                String explanation = actionType == ActionType.BID
                                ? "Es una oportunidad de mercado con una puntuación de "
                                                + recommendation.score()
                                                + "/100. La puja máxima recomendada es "
                                                + suggestedAmount
                                                + " €."
                                : "Es una oportunidad de mercado con una puntuación de "
                                                + recommendation.score()
                                                + "/100 y un precio de "
                                                + recommendation.askingPrice()
                                                + " €.";

                return new ActionCandidate(
                                actionType,
                                priority,
                                recommendation.playerId(),
                                recommendation.playerName(),
                                title,
                                explanation,
                                recommendation.score(),
                                suggestedAmount,
                                recommendation.reasons()
                                                .stream()
                                                .map(Enum::name)
                                                .toList());
        }

        private ActionCandidate evaluateFormationChange(
                        Long leagueId) {

                FormationRecommendationResponse formation = recommendationService
                                .getFormationRecommendation(
                                                leagueId);

                if (formation == null
                                || formation.currentFormation() == null
                                || formation.recommendedFormation() == null) {

                        return null;
                }

                /*
                 * Si la mejor formación encontrada sigue siendo
                 * la actual, no existe ninguna acción que recomendar.
                 */
                if (formation.currentFormation()
                                .equals(
                                                formation.recommendedFormation())) {

                        return null;
                }

                /*
                 * Evitamos recomendar cambios por diferencias
                 * marginales.
                 *
                 * La mejora representa puntos esperados del XI
                 * completo, por lo que exigimos al menos +2.
                 */
                if (formation.improvement() < 2.0) {
                        return null;
                }

                ActionPriority priority = formation.improvement() >= 4.0
                                ? ActionPriority.HIGH
                                : ActionPriority.MEDIUM;

                String title = "Cambiar formación a "
                                + formation.recommendedFormation();

                String explanation = "La formación "
                                + formation.recommendedFormation()
                                + " aprovecha mejor la plantilla disponible que "
                                + formation.currentFormation()
                                + ", con una mejora estimada de "
                                + formation.improvement()
                                + " puntos.";

                return new ActionCandidate(
                                ActionType.CHANGE_FORMATION,
                                priority,
                                null,
                                null,
                                title,
                                explanation,
                                formation.confidence(),
                                null,
                                List.of(
                                                "FORMATION_IMPROVEMENT",
                                                "CURRENT_FORMATION_"
                                                                + formation.currentFormation(),
                                                "RECOMMENDED_FORMATION_"
                                                                + formation.recommendedFormation()));
        }

        private List<ActionCandidate> evaluatePlayerActions(
                        Player player,
                        SquadNeedsResponse squadNeeds) {

                List<ActionCandidate> actions = new ArrayList<>();

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

                        actions.add(
                                        new ActionCandidate(
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
                                                        List.copyOf(signals)));
                }

                if (holdPressure >= 5
                                && difference >= 2) {

                        actions.add(
                                        new ActionCandidate(
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
                                                        List.copyOf(signals)));
                }

                PlayerProtectionAlert protectionAlert = playerProtectionService.calculate(player);

                boolean hasSellAction = actions.stream()
                                .anyMatch(action -> action.type() == ActionType.SELL);

                if (!hasSellAction
                                && protectionAlert.level() == PlayerProtectionAlertLevel.PROTECT) {

                        actions.add(
                                        new ActionCandidate(
                                                        ActionType.PROTECT,
                                                        protectionAlert.score() >= 80
                                                                        ? ActionPriority.HIGH
                                                                        : ActionPriority.MEDIUM,
                                                        player.getId(),
                                                        player.getName(),
                                                        "Protege a " + player.getName(),
                                                        "Su evolución económica y deportiva justifica "
                                                                        + "revisar al alza su cláusula.",
                                                        protectionAlert.score(),
                                                        null,
                                                        protectionAlert.reasons()
                                                                        .stream()
                                                                        .map(Enum::name)
                                                                        .toList()));
                }

                if (actions.isEmpty()) {

                        actions.add(
                                        new ActionCandidate(
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
                                                        List.copyOf(signals)));
                }

                return actions;

        }

        private List<ActionCandidate> evaluateStarterReplacementActions(
                        List<Player> squadPlayers) {

                List<ActionCandidate> actions = new ArrayList<>();

                List<Player> starters = squadPlayers.stream()
                                .filter(Player::isStarter)
                                .toList();

                List<Player> reserves = squadPlayers.stream()
                                .filter(Player::isReserve)
                                .filter(this::isAvailableForLineup)
                                .toList();

                for (Player starter : starters) {

                        PlayerPerformanceSignals starterPerformance = playerPerformanceSignalService
                                        .analyze(starter);

                        /*
                         * No queremos recomendar cambios de once
                         * con una muestra demasiado pequeña.
                         */
                        if (starterPerformance.recentSampleSize() < 2) {
                                continue;
                        }

                        Player bestReserve = null;
                        PlayerPerformanceSignals bestReservePerformance = null;

                        for (Player reserve : reserves) {

                                if (!canReplaceStarter(
                                                starter,
                                                reserve)) {

                                        continue;
                                }

                                PlayerPerformanceSignals reservePerformance = playerPerformanceSignalService
                                                .analyze(reserve);

                                if (reservePerformance.recentSampleSize() < 2) {
                                        continue;
                                }

                                double improvement = reservePerformance.recentWeightedAverage()
                                                - starterPerformance.recentWeightedAverage();

                                /*
                                 * Exigimos una ventaja clara.
                                 * Dos puntos de media reciente evita
                                 * recomendaciones por diferencias pequeñas.
                                 */
                                if (improvement < 2.0) {
                                        continue;
                                }

                                /*
                                 * Si tenemos histórico suficiente de ambos jugadores,
                                 * evitamos cambiar el once por una racha corta cuando
                                 * el suplente ha rendido claramente peor a largo plazo.
                                 *
                                 * Una ventaja reciente muy grande (>= 4 puntos)
                                 * sí puede imponerse al histórico.
                                 */
                                boolean enoughHistoricalData = starterPerformance.historicalSampleSize() >= 5
                                                && reservePerformance.historicalSampleSize() >= 5;

                                if (enoughHistoricalData) {

                                        double historicalDifference = reservePerformance.historicalAveragePoints()
                                                        - starterPerformance.historicalAveragePoints();

                                        if (historicalDifference <= -2.0
                                                        && improvement < 4.0) {

                                                continue;
                                        }
                                }

                                if (bestReserve == null
                                                || reservePerformance.recentWeightedAverage() > bestReservePerformance
                                                                .recentWeightedAverage()) {

                                        bestReserve = reserve;
                                        bestReservePerformance = reservePerformance;
                                }
                        }

                        if (bestReserve == null) {
                                continue;
                        }

                        double improvement = bestReservePerformance
                                        .recentWeightedAverage()
                                        - starterPerformance
                                                        .recentWeightedAverage();

                        actions.add(
                                        new ActionCandidate(
                                                        ActionType.REPLACE_STARTER,
                                                        improvement >= 4.0
                                                                        ? ActionPriority.HIGH
                                                                        : ActionPriority.MEDIUM,
                                                        starter.getId(),
                                                        starter.getName(),
                                                        "Replantea la titularidad de "
                                                                        + starter.getName(),
                                                        bestReserve.getName()
                                                                        + " está rindiendo mejor recientemente: "
                                                                        + round(
                                                                                        bestReservePerformance
                                                                                                        .recentWeightedAverage())
                                                                        + " puntos de media frente a "
                                                                        + round(
                                                                                        starterPerformance
                                                                                                        .recentWeightedAverage())
                                                                        + ".",
                                                        calculateLineupChangeConfidence(
                                                                        starterPerformance,
                                                                        bestReservePerformance,
                                                                        improvement),
                                                        null,
                                                        List.of(
                                                                        "STARTER_UNDERPERFORMING",
                                                                        "RESERVE_OUTPERFORMING",
                                                                        "SAME_POSITION")));
                }

                return actions;
        }

        private boolean canReplaceStarter(
                        Player starter,
                        Player reserve) {

                if (starter.getLineupPosition() != null
                                && reserve.getBenchPosition() != null) {

                        return starter.getLineupPosition() == reserve.getBenchPosition();
                }

                return starter.getPositions()
                                .stream()
                                .anyMatch(
                                                reserve.getPositions()::contains);
        }

        private boolean isAvailableForLineup(
                        Player player) {

                PlayerStatus status = player.getStatus();

                return status != PlayerStatus.INJURED
                                && status != PlayerStatus.SANCTIONED
                                && status != PlayerStatus.DISCARDED;
        }

        private int calculateLineupChangeConfidence(
                        PlayerPerformanceSignals starterPerformance,
                        PlayerPerformanceSignals reservePerformance,
                        double improvement) {

                int confidence = 40;

                if (starterPerformance.recentSampleSize() >= 3
                                && reservePerformance.recentSampleSize() >= 3) {

                        confidence += 20;
                }

                if (starterPerformance.historicalSampleSize() >= 5
                                && reservePerformance.historicalSampleSize() >= 5) {

                        confidence += 15;
                }

                confidence += Math.min(
                                (int) Math.round(improvement * 5),
                                25);

                return Math.min(
                                confidence,
                                100);
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

                String recentFormText = performance.recentSampleSize() >= 2
                                ? round(performance.recentWeightedAverage())
                                                + " puntos de media reciente"
                                : "datos recientes todavía insuficientes";

                return "Acumula "
                                + profit
                                + " € de beneficio/pérdida, cambia "
                                + dailyChange
                                + " € al día, tiene "
                                + recentFormText
                                + " y la necesidad de su posición es "
                                + positionNeed
                                + "/100.";
        }

        private String buildHoldExplanation(
                        long profit,
                        long dailyChange,
                        PlayerPerformanceSignals performance,
                        int positionNeed) {

                String recentFormText = performance.recentSampleSize() >= 2
                                ? round(performance.recentWeightedAverage())
                                                + " puntos de media reciente"
                                : "datos recientes todavía insuficientes";

                return "Mantenerlo sigue teniendo valor deportivo y económico: "
                                + profit
                                + " € de beneficio/pérdida, "
                                + dailyChange
                                + " € diarios y "
                                + recentFormText
                                + ".";
        }

        private double round(
                        double value) {

                return Math.round(value * 100.0) / 100.0;
        }
}