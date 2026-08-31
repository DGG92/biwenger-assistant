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
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerProtectionService;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlert;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlertLevel;
import com.artajerjes.biwengerassistant.recommendation.RecommendationService;
import com.artajerjes.biwengerassistant.recommendation.RecommendationType;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupChangeResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupResponse;
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

                RecommendedLineupResponse recommendedLineup = recommendationService
                                .getRecommendedLineup(
                                                leagueId);

                actions.addAll(
                                evaluateRecommendedLineupActions(
                                                squadPlayers,
                                                recommendedLineup));

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

        private List<ActionCandidate> evaluateRecommendedLineupActions(
                        List<Player> squadPlayers,
                        RecommendedLineupResponse lineup) {

                if (lineup == null) {
                        return List.of();
                }

                List<ActionCandidate> actions = new ArrayList<>();

                ActionCandidate formationAction = evaluateRecommendedFormationChange(
                                lineup);

                if (formationAction != null) {
                        actions.add(formationAction);
                }

                /*
                 * Si además cambia la formación, dejamos que esa acción
                 * represente la reestructuración completa del once.
                 *
                 * No tiene sentido generar también sustituciones
                 * individuales potencialmente engañosas, porque los
                 * puestos requeridos han cambiado.
                 */
                boolean formationChanges = lineup.currentFormation() != null
                                && lineup.recommendedFormation() != null
                                && !lineup.currentFormation()
                                                .equals(
                                                                lineup.recommendedFormation());

                if (formationChanges) {
                        return actions;
                }

                /*
                 * No generamos acciones individuales por diferencias
                 * insignificantes ni por soluciones empatadas.
                 */
                if (lineup.improvement() < 1.0) {
                        return actions;
                }

                List<RecommendedLineupChangeResponse> incomingChanges = new ArrayList<>(
                                lineup.changes()
                                                .stream()
                                                .filter(change -> "IN".equals(
                                                                change.type()))
                                                .toList());

                List<RecommendedLineupChangeResponse> outgoingChanges = lineup.changes()
                                .stream()
                                .filter(change -> "OUT".equals(
                                                change.type()))
                                .toList();

                for (RecommendedLineupChangeResponse outgoing : outgoingChanges) {

                        Player outgoingPlayer = squadPlayers.stream()
                                        .filter(player -> player.getId()
                                                        .equals(
                                                                        outgoing.playerId()))
                                        .findFirst()
                                        .orElse(null);

                        if (outgoingPlayer == null
                                        || incomingChanges.isEmpty()) {

                                continue;
                        }

                        String outgoingPosition = outgoingPlayer.getLineupPosition() == null
                                        ? null
                                        : outgoingPlayer
                                                        .getLineupPosition()
                                                        .name();

                        RecommendedLineupChangeResponse incoming = incomingChanges.stream()
                                        .filter(change -> incomingMatchesPosition(
                                                        change,
                                                        outgoingPosition,
                                                        lineup))
                                        .findFirst()
                                        .orElse(
                                                        incomingChanges.get(0));

                        incomingChanges.remove(incoming);

                        actions.add(
                                        buildRecommendedStarterReplacementAction(
                                                        outgoing,
                                                        incoming,
                                                        outgoingPosition,
                                                        lineup));
                }

                return actions;
        }

        private boolean incomingMatchesPosition(
                        RecommendedLineupChangeResponse incoming,
                        String outgoingPosition,
                        RecommendedLineupResponse lineup) {

                if (outgoingPosition == null) {
                        return false;
                }

                return lineup.recommendedStarters()
                                .stream()
                                .anyMatch(player -> player.playerId()
                                                .equals(
                                                                incoming.playerId())
                                                && outgoingPosition.equals(
                                                                player.position()));
        }

        private ActionCandidate buildRecommendedStarterReplacementAction(
                        RecommendedLineupChangeResponse outgoing,
                        RecommendedLineupChangeResponse incoming,
                        String position,
                        RecommendedLineupResponse lineup) {

                ActionPriority priority = lineup.improvement() >= 4.0
                                ? ActionPriority.HIGH
                                : ActionPriority.MEDIUM;

                String explanation = "El XI recomendado por el asistente incluye a "
                                + incoming.playerName()
                                + " en lugar de "
                                + outgoing.playerName()
                                + ", dentro de una alineación que mejora "
                                + "la puntuación estimada de "
                                + lineup.currentScore()
                                + " a "
                                + lineup.recommendedScore()
                                + ".";

                List<String> signals = new ArrayList<>();

                signals.add(
                                "RECOMMENDED_LINEUP_CHANGE");

                signals.add(
                                "OUT_PLAYER_"
                                                + outgoing.playerId());

                signals.add(
                                "IN_PLAYER_"
                                                + incoming.playerId());

                if (position != null) {
                        signals.add(
                                        "POSITION_"
                                                        + position);
                }

                return new ActionCandidate(
                                ActionType.REPLACE_STARTER,
                                priority,
                                outgoing.playerId(),
                                outgoing.playerName(),
                                "Sustituye a "
                                                + outgoing.playerName()
                                                + " por "
                                                + incoming.playerName(),
                                explanation,
                                lineup.confidence(),
                                null,
                                List.copyOf(signals));
        }

        private ActionCandidate evaluateRecommendedFormationChange(
                        RecommendedLineupResponse lineup) {

                if (lineup.currentFormation() == null
                                || lineup.recommendedFormation() == null) {

                        return null;
                }

                if (lineup.currentFormation()
                                .equals(
                                                lineup.recommendedFormation())) {

                        return null;
                }

                /*
                 * Para cambiar toda la estructura del once mantenemos
                 * un umbral más exigente que para una sustitución.
                 */
                if (lineup.improvement() < 2.0) {
                        return null;
                }

                ActionPriority priority = lineup.improvement() >= 4.0
                                ? ActionPriority.HIGH
                                : ActionPriority.MEDIUM;

                String title = "Cambiar formación a "
                                + lineup.recommendedFormation();

                String explanation = "El XI recomendado mejora la alineación actual "
                                + "al pasar de "
                                + lineup.currentFormation()
                                + " a "
                                + lineup.recommendedFormation()
                                + ", con una mejora estimada de "
                                + lineup.improvement()
                                + " puntos.";

                return new ActionCandidate(
                                ActionType.CHANGE_FORMATION,
                                priority,
                                null,
                                null,
                                title,
                                explanation,
                                lineup.confidence(),
                                null,
                                List.of(
                                                "FORMATION_IMPROVEMENT",
                                                "RECOMMENDED_LINEUP",
                                                "CURRENT_FORMATION_"
                                                                + lineup.currentFormation(),
                                                "RECOMMENDED_FORMATION_"
                                                                + lineup.recommendedFormation()));
        }

        private List<ActionCandidate> evaluatePlayerActions(
                        Player player,
                        SquadNeedsResponse squadNeeds) {

                List<ActionCandidate> actions = new ArrayList<>();

                PlayerPerformanceSignals performance = playerPerformanceSignalService.analyze(player);

                boolean coach = player.getPositions() != null
                                && player.getPositions()
                                                .contains(PlayerPosition.E);

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

                        double recentAverage = performance.recentWeightedAverage();

                        if (coach) {

                                /*
                                 * Los entrenadores puntúan:
                                 * victoria = 3, empate = 1, derrota = 0.
                                 *
                                 * Por tanto necesitan una escala propia y no pueden
                                 * compararse con los umbrales de los futbolistas.
                                 */
                                if (recentAverage >= 2.0) {
                                        holdPressure += 4;
                                        signals.add("RECENT_FORM_EXCELLENT");

                                } else if (recentAverage >= 1.3) {
                                        holdPressure += 2;
                                        signals.add("RECENT_FORM_GOOD");

                                } else if (recentAverage < 0.8) {
                                        sellPressure += 3;
                                        signals.add("RECENT_FORM_POOR");
                                }

                        } else {

                                if (recentAverage >= 7) {
                                        holdPressure += 4;
                                        signals.add("RECENT_FORM_EXCELLENT");

                                } else if (recentAverage >= 5) {
                                        holdPressure += 2;
                                        signals.add("RECENT_FORM_GOOD");

                                } else if (recentAverage < 2) {
                                        sellPressure += 3;
                                        signals.add("RECENT_FORM_POOR");
                                }
                        }

                } else {
                        signals.add("RECENT_FORM_INSUFFICIENT_DATA");
                }

                /*
                 * HISTÓRICO
                 */

                if (performance.historicalSampleSize() >= 5) {

                        double historicalAverage = performance.historicalAveragePoints();

                        if (coach) {

                                if (historicalAverage >= 1.8) {
                                        holdPressure += 3;
                                        signals.add("HISTORICAL_PERFORMANCE_STRONG");

                                } else if (historicalAverage >= 1.3) {
                                        holdPressure += 1;
                                        signals.add("HISTORICAL_PERFORMANCE_GOOD");

                                } else if (historicalAverage < 0.8) {
                                        sellPressure += 2;
                                        signals.add("HISTORICAL_PERFORMANCE_POOR");
                                }

                        } else {

                                if (historicalAverage >= 6) {
                                        holdPressure += 3;
                                        signals.add("HISTORICAL_PERFORMANCE_STRONG");

                                } else if (historicalAverage >= 5) {
                                        holdPressure += 1;
                                        signals.add("HISTORICAL_PERFORMANCE_GOOD");

                                } else if (historicalAverage < 2) {
                                        sellPressure += 2;
                                        signals.add("HISTORICAL_PERFORMANCE_POOR");
                                }
                        }
                }

                /*
                 * CONTEXTO DE PLANTILLA
                 */

                if (!coach) {

                        if (positionNeed >= 50) {
                                holdPressure += 4;
                                signals.add("POSITION_NEEDED");

                        } else if (positionNeed == 0) {
                                sellPressure += 1;
                                signals.add("POSITION_WELL_COVERED");
                        }
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