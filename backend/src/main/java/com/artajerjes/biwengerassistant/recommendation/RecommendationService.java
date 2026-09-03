package com.artajerjes.biwengerassistant.recommendation;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.history.PlayerPriceHistory;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.market.MarketListing;
import com.artajerjes.biwengerassistant.market.MarketListingRepository;
import com.artajerjes.biwengerassistant.market.MarketListingType;
import com.artajerjes.biwengerassistant.matchday.MatchdayChangeEligibilityService;
import com.artajerjes.biwengerassistant.matchday.MatchdayDifficultyService;
import com.artajerjes.biwengerassistant.matchday.OpponentDifficulty;
import com.artajerjes.biwengerassistant.offer.OfferService;
import com.artajerjes.biwengerassistant.offer.dto.EconomicStatusResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.recommendation.dto.FormationRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationReason;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketScoreBreakdown;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupChangeResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupPlayerResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignalService;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignals;

@Service
public class RecommendationService {

        private record Formation(
                        int defenders,
                        int midfielders,
                        int forwards) {
        }

        private record FormationAssignment(
                        Player player,
                        PlayerPosition position,
                        double rating) {
        }

        private record FormationLineup(
                        Formation formation,
                        double score,
                        List<FormationAssignment> assignments) {
        }

        private static final List<Formation> VALID_FORMATIONS = List.of(
                        new Formation(3, 4, 3),
                        new Formation(3, 5, 2),
                        new Formation(4, 3, 3),
                        new Formation(4, 4, 2),
                        new Formation(4, 5, 1),
                        new Formation(5, 3, 2),
                        new Formation(5, 4, 1),
                        new Formation(3, 6, 1),
                        new Formation(3, 3, 4),
                        new Formation(4, 2, 4),
                        new Formation(4, 6, 0),
                        new Formation(5, 2, 3),
                        new Formation(3, 2, 5),
                        new Formation(5, 1, 4));

        private final LeagueRepository leagueRepository;
        private final MarketListingRepository marketListingRepository;
        private final OfferService offerService;
        private final PlayerRepository playerRepository;
        private final PlayerPerformanceSignalService playerPerformanceSignalService;
        private final MatchdayDifficultyService matchdayDifficultyService;
        private final MatchdayChangeEligibilityService matchdayChangeEligibilityService;
        private final PlayerPriceHistoryRepository playerPriceHistoryRepository;

        private static final double IMPOSSIBLE_FORMATION_SCORE = -1_000_000;
        private static final double MATCHDAY_DIFFICULTY_MAX_ADJUSTMENT = 0.08;
        private static final double MIN_FORMATION_CHANGE_IMPROVEMENT = 1.0;
        private static final int ECONOMIC_CHANGE_DAYS = 7;

        @Value("${biwenger.user-id}")
        private Long biwengerUserId;

        public RecommendationService(
                        LeagueRepository leagueRepository,
                        MarketListingRepository marketListingRepository,
                        OfferService offerService,
                        PlayerRepository playerRepository,
                        PlayerPerformanceSignalService playerPerformanceSignalService,
                        MatchdayDifficultyService matchdayDifficultyService,
                        MatchdayChangeEligibilityService matchdayChangeEligibilityService,
                        PlayerPriceHistoryRepository playerPriceHistoryRepository) {

                this.leagueRepository = leagueRepository;
                this.marketListingRepository = marketListingRepository;
                this.offerService = offerService;
                this.playerRepository = playerRepository;
                this.playerPerformanceSignalService = playerPerformanceSignalService;
                this.matchdayDifficultyService = matchdayDifficultyService;
                this.matchdayChangeEligibilityService = matchdayChangeEligibilityService;
                this.playerPriceHistoryRepository = playerPriceHistoryRepository;
        }

        private double clampDouble(
                        double value,
                        double min,
                        double max) {
                return Math.max(
                                min,
                                Math.min(max, value));
        }

        @Transactional(readOnly = true)
        public List<MarketRecommendationResponse> getMarketRecommendations(
                        Long leagueId) {

                if (!leagueRepository.existsById(leagueId)) {
                        throw new LeagueNotFoundException(leagueId);
                }

                EconomicStatusResponse economicStatus = offerService.getEconomicStatus(leagueId);

                SquadNeedsResponse squadNeeds = getSquadNeeds(leagueId);

                LocalDate referenceDate = LocalDate.now()
                                .minusDays(ECONOMIC_CHANGE_DAYS);

                Map<Long, Long> value7DaysAgoByPlayer = playerPriceHistoryRepository
                                .findAllByLeagueIdOrderByPlayerAndPriceDate(leagueId)
                                .stream()
                                .filter(history -> !history.getPriceDate()
                                                .isAfter(referenceDate))
                                .collect(
                                                Collectors.toMap(
                                                                PlayerPriceHistory::getPlayerId,
                                                                PlayerPriceHistory::getMarketValue,
                                                                (previousValue, currentValue) -> currentValue));

                return marketListingRepository
                                .findAllByLeague_Id(leagueId)
                                .stream()
                                .filter(listing -> listing.getSeller() == null
                                                || !biwengerUserId.equals(
                                                                listing.getSeller()
                                                                                .getBiwengerManagerId()))
                                .map(listing -> toRecommendation(
                                                listing,
                                                economicStatus.maximumBid(),
                                                squadNeeds.needScoreByPosition(),
                                                value7DaysAgoByPlayer))
                                .sorted(
                                                Comparator.comparingInt(
                                                                MarketRecommendationResponse::score)
                                                                .reversed())
                                .toList();
        }

        private MarketRecommendationResponse toRecommendation(
                        MarketListing listing,
                        Long maximumBid,
                        Map<String, Integer> needScoreByPosition,
                        Map<Long, Long> value7DaysAgoByPlayer) {
                Player player = listing.getPlayer();

                Long marketValue = player.getMarketValue();

                Long value7DaysAgo = value7DaysAgoByPlayer.get(
                                player.getId());

                Long change7Days = calculateEconomicChange(
                                marketValue,
                                value7DaysAgo);

                Double changePercent7Days = calculateEconomicChangePercent(
                                marketValue,
                                value7DaysAgo);

                Double pointsPerMillion = calculatePointsPerMillion(
                                player.getPoints(),
                                marketValue);

                Long askingPrice = listing.getPrice();

                Long currentBid = listing.getType() == MarketListingType.AUCTION
                                ? listing.getLastBidAmount()
                                : null;

                Long maximumRecommendedBid = listing.getType() == MarketListingType.AUCTION
                                ? calculateMaximumRecommendedBid(
                                                player,
                                                maximumBid,
                                                changePercent7Days)
                                : null;

                long effectivePrice;

                if (listing.getType() == MarketListingType.AUCTION
                                && currentBid != null) {
                        effectivePrice = currentBid;
                } else {
                        effectivePrice = askingPrice;
                }

                long difference = marketValue - effectivePrice;

                double differencePercentage = marketValue == 0
                                ? 0
                                : ((double) difference / marketValue) * 100;

                boolean affordable;

                if (listing.getType() == MarketListingType.AUCTION) {
                        affordable = maximumBid != null
                                        && maximumRecommendedBid != null
                                        && effectivePrice <= maximumBid
                                        && effectivePrice <= maximumRecommendedBid;
                } else {
                        affordable = maximumBid != null
                                        && askingPrice <= maximumBid;
                }

                int squadNeedScore = calculatePlayerSquadNeedScore(
                                player,
                                needScoreByPosition);

                PlayerPerformanceSignals performance = playerPerformanceSignalService.analyze(player);

                int recentFormScore = calculateRecentFormScore(performance);

                int historicalPerformanceScore = calculateHistoricalPerformanceScore(performance);

                MarketScoreBreakdown scoreBreakdown = calculateScoreBreakdown(
                                player,
                                differencePercentage,
                                squadNeedScore,
                                recentFormScore,
                                performance.recentSampleSize(),
                                performance.historicalAveragePoints(),
                                performance.historicalSampleSize(),
                                historicalPerformanceScore,
                                changePercent7Days);

                int score = calculateScore(
                                scoreBreakdown,
                                affordable);

                boolean auctionBidCapApplied = listing.getType() == MarketListingType.AUCTION
                                && maximumRecommendedBid != null
                                && effectivePrice > maximumRecommendedBid
                                && scoreBreakdown.scoreBeforeCaps() > 25;

                boolean affordabilityCapApplied = !affordable
                                && !auctionBidCapApplied
                                && scoreBreakdown.scoreBeforeCaps() > 25;

                List<MarketRecommendationReason> reasons = calculateReasons(
                                player,
                                differencePercentage,
                                affordable,
                                squadNeedScore,
                                recentFormScore,
                                historicalPerformanceScore,
                                changePercent7Days);

                /*
                 * En una subasta que ya ha superado nuestro límite
                 * recomendado, nunca queremos recomendar comprar.
                 */
                if (listing.getType() == MarketListingType.AUCTION
                                && maximumRecommendedBid != null
                                && effectivePrice > maximumRecommendedBid) {

                        score = Math.min(
                                        score,
                                        25);
                }

                scoreBreakdown = new MarketScoreBreakdown(
                                scoreBreakdown.base(),
                                scoreBreakdown.price(),
                                scoreBreakdown.valueTrend(),
                                scoreBreakdown.squadNeed(),
                                scoreBreakdown.recentForm(),
                                scoreBreakdown.recentFormSampleSize(),
                                scoreBreakdown.historicalAveragePoints(),
                                scoreBreakdown.historicalSampleSize(),
                                scoreBreakdown.historicalPerformance(),
                                scoreBreakdown.status(),
                                scoreBreakdown.scoreBeforeCaps(),
                                affordabilityCapApplied,
                                auctionBidCapApplied);

                Manager seller = listing.getSeller();

                return new MarketRecommendationResponse(
                                player.getId(),
                                player.getBiwengerPlayerId(),
                                player.getName(),
                                player.getTeamName(),
                                player.getPositions(),
                                listing.getType(),
                                marketValue,
                                askingPrice,
                                currentBid,
                                maximumRecommendedBid,
                                difference,
                                round(differencePercentage),
                                player.getValueFluctuation(),
                                value7DaysAgo,
                                change7Days,
                                changePercent7Days == null
                                                ? null
                                                : round(changePercent7Days),
                                pointsPerMillion == null
                                                ? null
                                                : round(pointsPerMillion),
                                player.getPoints(),
                                player.getStatus(),
                                affordable,
                                score,
                                resolveRecommendation(score),
                                seller == null ? null : seller.getId(),
                                seller == null ? null : seller.getName(),
                                reasons,
                                scoreBreakdown);

        }

        private Long calculateEconomicChange(
                        Long currentValue,
                        Long previousValue) {

                if (currentValue == null || previousValue == null) {
                        return null;
                }

                return currentValue - previousValue;
        }

        private Double calculateEconomicChangePercent(
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

        private Double calculatePointsPerMillion(
                        Integer points,
                        Long marketValue) {

                if (points == null
                                || marketValue == null
                                || marketValue <= 0) {

                        return null;
                }

                return points
                                / (marketValue / 1_000_000.0);
        }

        private double resolveMarketTrendPercentage(
                        Player player,
                        Double changePercent7Days) {

                if (changePercent7Days != null) {
                        return changePercent7Days;
                }

                if (player.getMarketValue() == null
                                || player.getMarketValue() <= 0
                                || player.getValueFluctuation() == null) {

                        return 0;
                }

                return ((double) player.getValueFluctuation()
                                / player.getMarketValue())
                                * 100;
        }

        private List<MarketRecommendationReason> calculateReasons(
                        Player player,
                        double differencePercentage,
                        boolean affordable,
                        int squadNeedScore,
                        int recentFormScore,
                        int historicalPerformanceScore,
                        Double changePercent7Days) {

                List<MarketRecommendationReason> reasons = new java.util.ArrayList<>();

                if (differencePercentage >= 5) {
                        reasons.add(
                                        MarketRecommendationReason.PRICE_BELOW_MARKET);
                }

                if (differencePercentage <= -5) {
                        reasons.add(
                                        MarketRecommendationReason.PRICE_ABOVE_MARKET);
                }

                double marketTrendPercentage = resolveMarketTrendPercentage(
                                player,
                                changePercent7Days);

                double strongRiseThreshold = changePercent7Days != null
                                ? 5.0
                                : 3.0;

                if (marketTrendPercentage >= strongRiseThreshold) {
                        reasons.add(
                                        MarketRecommendationReason.VALUE_RISING_FAST);
                } else if (marketTrendPercentage > 0) {
                        reasons.add(
                                        MarketRecommendationReason.VALUE_RISING);
                } else if (marketTrendPercentage < 0) {
                        reasons.add(
                                        MarketRecommendationReason.VALUE_FALLING);
                }

                if (squadNeedScore >= 50) {
                        reasons.add(
                                        MarketRecommendationReason.SQUAD_POSITION_NEEDED);
                }

                if (recentFormScore >= 15) {
                        reasons.add(
                                        MarketRecommendationReason.EXCELLENT_RECENT_FORM);
                } else if (recentFormScore >= 5) {
                        reasons.add(
                                        MarketRecommendationReason.GOOD_RECENT_FORM);
                }

                if (historicalPerformanceScore >= 5) {
                        reasons.add(
                                        MarketRecommendationReason.STRONG_HISTORICAL_PERFORMANCE);
                }

                if (historicalPerformanceScore <= -5) {
                        reasons.add(
                                        MarketRecommendationReason.POOR_HISTORICAL_PERFORMANCE);
                }

                if (player.getStatus() == PlayerStatus.INJURED) {
                        reasons.add(
                                        MarketRecommendationReason.INJURED);
                }

                if (!affordable) {
                        reasons.add(
                                        MarketRecommendationReason.UNAFFORDABLE);
                }

                return List.copyOf(reasons);
        }

        private Long calculateMaximumRecommendedBid(
                        Player player,
                        Long userMaximumBid,
                        Double changePercent7Days) {
                if (player.getMarketValue() == null
                                || player.getMarketValue() <= 0) {
                        return 0L;
                }

                double multiplier = 1.0;

                double marketTrendPercentage = resolveMarketTrendPercentage(
                                player,
                                changePercent7Days);

                double bidTrendMultiplier = changePercent7Days != null
                                ? 0.75
                                : 2.5;

                if (marketTrendPercentage > 0) {

                        double premiumPercentage = Math.min(
                                        marketTrendPercentage * bidTrendMultiplier,
                                        12);

                        multiplier += premiumPercentage / 100;
                }

                if (marketTrendPercentage < 0) {

                        double discountPercentage = Math.min(
                                        Math.abs(marketTrendPercentage)
                                                        * bidTrendMultiplier,
                                        12);

                        multiplier -= discountPercentage / 100;
                }

                /*
                 * Un lesionado/en duda/no convocado/... requiere mayor margen de seguridad.
                 */
                multiplier -= calculateStatusBidPenalty(
                                player.getStatus());

                long recommended = Math.round(
                                player.getMarketValue()
                                                * multiplier);

                recommended = Math.max(
                                recommended,
                                0L);

                /*
                 * Aunque deportivamente pagaríamos más,
                 * nunca recomendamos una puja imposible
                 * para nuestra economía actual.
                 */
                if (userMaximumBid != null) {
                        recommended = Math.min(
                                        recommended,
                                        userMaximumBid);
                }

                return recommended;
        }

        private MarketScoreBreakdown calculateScoreBreakdown(
                        Player player,
                        double differencePercentage,
                        int squadNeedScore,
                        int recentFormScore,
                        int recentFormSampleSize,
                        double historicalAveragePoints,
                        int historicalSampleSize,
                        int historicalPerformanceScore,
                        Double changePercent7Days) {

                double baseScore = 50;

                double priceScore = clampDouble(
                                differencePercentage * 3,
                                -30,
                                30);

                double marketTrendPercentage = resolveMarketTrendPercentage(
                                player,
                                changePercent7Days);

                double trendMultiplier = changePercent7Days != null
                                ? 2.0
                                : 7.0;

                double valueTrendScore = Math.round(
                                clampDouble(
                                                marketTrendPercentage * trendMultiplier,
                                                -25,
                                                25));

                double squadNeedContribution = (int) Math.round(
                                squadNeedScore * 0.20);

                double statusPenalty = calculateStatusPenalty(
                                player.getStatus());

                double scoreBeforeCaps = baseScore
                                + priceScore
                                + valueTrendScore
                                + squadNeedContribution
                                + recentFormScore
                                + historicalPerformanceScore
                                + statusPenalty;

                return new MarketScoreBreakdown(
                                baseScore,
                                priceScore,
                                valueTrendScore,
                                squadNeedContribution,
                                recentFormScore,
                                recentFormSampleSize,
                                historicalAveragePoints,
                                historicalSampleSize,
                                historicalPerformanceScore,
                                statusPenalty,
                                scoreBeforeCaps,
                                false,
                                false);
        }

        private int calculateScore(
                        MarketScoreBreakdown breakdown,
                        boolean affordable) {

                double score = breakdown.scoreBeforeCaps();

                if (!affordable) {
                        score = Math.min(
                                        score,
                                        25);
                }

                return clamp(
                                (int) Math.round(score),
                                0,
                                100);
        }

        private int calculateStatusPenalty(
                        PlayerStatus status) {

                if (status == null) {
                        return 0;
                }

                return switch (status) {
                        case OK -> 0;
                        case DOUBT -> -10;
                        case INJURED -> -30;
                        case SANCTIONED -> -30;
                        case WARNED -> -5;
                        case DISCARDED -> -25;
                        case UNKNOWN -> 0;
                };
        }

        private boolean isInjuredStatus(
                        PlayerStatus status) {

                return status == PlayerStatus.INJURED;
        }

        private double calculateStatusBidPenalty(
                        PlayerStatus status) {

                if (status == null) {
                        return 0;
                }

                return switch (status) {
                        case OK -> 0;
                        case DOUBT -> 0.05;
                        case INJURED -> 0.15;
                        case SANCTIONED -> 0.15;
                        case WARNED -> 0.02;
                        case DISCARDED -> 0.12;
                        case UNKNOWN -> 0;
                };
        }

        private int calculatePlayerSquadNeedScore(
                        Player player,
                        Map<String, Integer> needScoreByPosition) {
                if (player.getPositions() == null
                                || player.getPositions().isEmpty()) {
                        return 0;
                }

                int highestNeed = 0;

                for (PlayerPosition position : player.getPositions()) {

                        if (position == PlayerPosition.E) {
                                continue;
                        }

                        int positionNeed = needScoreByPosition.getOrDefault(
                                        position.name(),
                                        0);

                        highestNeed = Math.max(
                                        highestNeed,
                                        positionNeed);
                }

                return highestNeed;
        }

        private int calculateRecentFormScore(
                        PlayerPerformanceSignals performance) {

                if (performance == null
                                || performance.recentSampleSize() < 2) {
                        return 0;
                }

                if (performance.allRecentMatchesExcellent()) {
                        return 15;
                }

                double average = performance.recentWeightedAverage();

                if (average >= 7) {
                        return 10;
                }

                if (average >= 5) {
                        return 5;
                }

                if (average >= 3) {
                        return 0;
                }

                if (average >= 1) {
                        return -5;
                }

                return -10;
        }

        private int calculateHistoricalPerformanceScore(
                        PlayerPerformanceSignals performance) {

                if (performance == null
                                || performance.historicalSampleSize() < 5) {
                        return 0;
                }

                double average = performance.historicalAveragePoints();

                if (average >= 6) {
                        return 10;
                }

                if (average >= 5) {
                        return 5;
                }

                if (average >= 3) {
                        return 0;
                }

                if (average >= 2) {
                        return -5;
                }

                return -10;
        }

        @Transactional(readOnly = true)
        public SquadNeedsResponse getSquadNeeds(Long leagueId) {
                if (!leagueRepository.existsById(leagueId)) {
                        throw new LeagueNotFoundException(leagueId);
                }

                List<Player> squad = playerRepository
                                .findAllByLeague_Id(leagueId)
                                .stream()
                                .filter(player -> player.getOwner() != null)
                                .filter(player -> biwengerUserId.equals(
                                                player.getOwner().getBiwengerManagerId()))
                                .toList();

                List<Player> squadPlayers = squad.stream()
                                .filter(player -> !player.getPositions().contains(
                                                PlayerPosition.E))
                                .toList();

                Map<String, Integer> playersByPosition = countPositions(squadPlayers);

                Map<String, Integer> startersByPosition = countPositions(
                                squadPlayers.stream()
                                                .filter(Player::isStarter)
                                                .toList());

                Map<String, Integer> injuredByPosition = countPositions(
                                squadPlayers.stream()
                                                .filter(player -> player.getStatus() == PlayerStatus.INJURED)
                                                .toList());

                Map<String, Double> effectiveAvailabilityByPosition = countEffectiveAvailabilityByPosition(
                                squadPlayers);

                Map<String, Integer> needScoreByPosition = calculateNeedScores(
                                effectiveAvailabilityByPosition, squadPlayers);

                Manager manager = squad.isEmpty()
                                ? null
                                : squad.get(0).getOwner();

                return new SquadNeedsResponse(
                                manager == null
                                                ? null
                                                : manager.getId(),
                                manager == null
                                                ? null
                                                : manager.getName(),
                                squadPlayers.size(),
                                playersByPosition,
                                startersByPosition,
                                injuredByPosition,
                                needScoreByPosition);
        }

        @Transactional(readOnly = true)
        public FormationRecommendationResponse getFormationRecommendation(
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
                                .filter(player -> !player.getPositions()
                                                .contains(PlayerPosition.E))
                                .toList();

                Map<Long, OpponentDifficulty> difficultyByTeamId = resolveMatchdayDifficulties(
                                leagueId,
                                squadPlayers);

                if (squadPlayers.isEmpty()) {

                        return new FormationRecommendationResponse(
                                        null,
                                        null,
                                        0,
                                        0,
                                        0,
                                        0);
                }

                String currentFormation = squadPlayers.get(0)
                                .getOwner()
                                .getCurrentFormation();

                if (currentFormation == null
                                || currentFormation.isBlank()) {

                        return new FormationRecommendationResponse(
                                        null,
                                        null,
                                        0,
                                        0,
                                        0,
                                        0);
                }

                Formation current = findFormation(currentFormation);

                if (current == null) {

                        return new FormationRecommendationResponse(
                                        currentFormation,
                                        null,
                                        0,
                                        0,
                                        0,
                                        0);
                }

                double currentScore = calculateFormationPerformanceScore(
                                squadPlayers,
                                current,
                                difficultyByTeamId);

                boolean currentFormationIsFeasible = currentScore != IMPOSSIBLE_FORMATION_SCORE;

                Formation bestFormation = current;

                double bestScore = currentFormationIsFeasible
                                ? currentScore
                                : IMPOSSIBLE_FORMATION_SCORE;

                for (Formation formation : VALID_FORMATIONS) {

                        if (formation.equals(current)) {
                                continue;
                        }

                        double score = calculateFormationPerformanceScore(
                                        squadPlayers,
                                        formation,
                                        difficultyByTeamId);

                        if (score > bestScore) {

                                bestScore = score;
                                bestFormation = formation;
                        }
                }

                boolean bestFormationIsFeasible = bestScore != IMPOSSIBLE_FORMATION_SCORE;

                double publicCurrentScore = currentFormationIsFeasible
                                ? currentScore
                                : 0;

                double publicBestScore = bestFormationIsFeasible
                                ? bestScore
                                : 0;

                double improvement = Math.max(
                                publicBestScore - publicCurrentScore,
                                0);

                /*
                 * Si la formación actual es válida y la mejora de otra
                 * formación es demasiado pequeña, mantenemos la actual.
                 *
                 * Evitamos recomendar cambios tácticos por diferencias
                 * prácticamente irrelevantes.
                 */
                if (currentFormationIsFeasible
                                && !bestFormation.equals(current)
                                && improvement < MIN_FORMATION_CHANGE_IMPROVEMENT) {

                        bestFormation = current;
                        publicBestScore = publicCurrentScore;
                        improvement = 0;
                }

                int confidence = calculateFormationRecommendationConfidence(
                                improvement);

                return new

                FormationRecommendationResponse(
                                currentFormation,
                                formationName(bestFormation),
                                round(publicCurrentScore),
                                round(publicBestScore),
                                round(improvement),
                                confidence);
        }

        @Transactional(readOnly = true)
        public RecommendedLineupResponse getRecommendedLineup(
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
                                .filter(player -> !player.getPositions()
                                                .contains(PlayerPosition.E))
                                .toList();

                if (squadPlayers.isEmpty()) {
                        return new RecommendedLineupResponse(
                                        null,
                                        null,
                                        0,
                                        0,
                                        0,
                                        0,
                                        List.of(),
                                        List.of());
                }

                Manager manager = squadPlayers.get(0)
                                .getOwner();

                String currentFormation = manager.getCurrentFormation();

                Map<Long, OpponentDifficulty> difficultyByTeamId = resolveMatchdayDifficulties(
                                leagueId,
                                squadPlayers);

                Map<Long, Boolean> modifiableByTeamId = matchdayChangeEligibilityService
                                .resolveModifiableByTeam(
                                                leagueId);

                Set<Long> lockedStarterIds = squadPlayers.stream()
                                .filter(Player::isStarter)
                                .filter(player -> !isPlayerModifiable(
                                                player,
                                                modifiableByTeamId))
                                .map(Player::getId)
                                .collect(java.util.stream.Collectors.toSet());

                List<Player> optimizationPlayers = squadPlayers.stream()
                                .filter(player -> player.isStarter()
                                                || isPlayerModifiable(
                                                                player,
                                                                modifiableByTeamId))
                                .toList();

                Formation currentFormationDefinition = findFormation(
                                currentFormation);

                double currentScore = currentFormationDefinition == null
                                ? 0
                                : calculateCurrentLineupScore(
                                                squadPlayers,
                                                currentFormationDefinition,
                                                difficultyByTeamId);

                FormationLineup bestLineup = currentFormationDefinition == null
                                ? null
                                : calculateBestFormationLineup(
                                                optimizationPlayers,
                                                currentFormationDefinition,
                                                difficultyByTeamId,
                                                lockedStarterIds);

                FormationLineup bestCurrentFormationLineup = bestLineup;

                if (bestLineup != null
                                && bestLineup.score() == IMPOSSIBLE_FORMATION_SCORE) {

                        bestLineup = null;
                }

                for (Formation formation : VALID_FORMATIONS) {

                        if (formation.equals(
                                        currentFormationDefinition)) {

                                continue;
                        }

                        FormationLineup candidate = calculateBestFormationLineup(
                                        optimizationPlayers,
                                        formation,
                                        difficultyByTeamId,
                                        lockedStarterIds);

                        if (candidate.score() == IMPOSSIBLE_FORMATION_SCORE) {

                                continue;
                        }

                        if (bestLineup == null
                                        || candidate.score() > bestLineup.score()
                                                        + 0.000001) {

                                bestLineup = candidate;
                        }
                }

                if (bestLineup == null) {

                        return new RecommendedLineupResponse(
                                        currentFormation,
                                        null,
                                        round(currentScore),
                                        0,
                                        0,
                                        0,
                                        List.of(),
                                        List.of());
                }

                double improvement = Math.max(
                                bestLineup.score()
                                                - currentScore,
                                0);

                /*
                 * Una formación distinta debe aportar una mejora mínima
                 * para justificar que la presentemos como recomendación.
                 *
                 * Seguimos permitiendo cambios de jugadores dentro de la
                 * formación actual aunque la mejora sea menor.
                 */
                if (currentFormationDefinition != null
                                && bestCurrentFormationLineup != null
                                && bestCurrentFormationLineup.score() != IMPOSSIBLE_FORMATION_SCORE
                                && !bestLineup.formation()
                                                .equals(currentFormationDefinition)) {

                        double formationChangeImprovement = bestLineup.score()
                                        - bestCurrentFormationLineup.score();

                        if (formationChangeImprovement < MIN_FORMATION_CHANGE_IMPROVEMENT) {

                                bestLineup = bestCurrentFormationLineup;

                                improvement = Math.max(
                                                bestLineup.score()
                                                                - currentScore,
                                                0);
                        }
                }

                int confidence = calculateFormationRecommendationConfidence(
                                improvement);

                List<RecommendedLineupPlayerResponse> recommendedStarters = bestLineup.assignments()
                                .stream()
                                .map(assignment -> new RecommendedLineupPlayerResponse(
                                                assignment.player().getId(),
                                                assignment.player().getName(),
                                                assignment.position().name(),
                                                round(assignment.rating())))
                                .toList();

                List<RecommendedLineupChangeResponse> changes = calculateRecommendedLineupChanges(
                                squadPlayers,
                                bestLineup);

                return new RecommendedLineupResponse(
                                currentFormation,
                                formationName(
                                                bestLineup.formation()),
                                round(currentScore),
                                round(bestLineup.score()),
                                round(improvement),
                                confidence,
                                recommendedStarters,
                                changes);
        }

        private Map<Long, OpponentDifficulty> resolveMatchdayDifficulties(
                        Long leagueId,
                        List<Player> players) {

                if (leagueId == null
                                || players == null
                                || players.isEmpty()) {

                        return Map.of();
                }

                List<Long> teamIds = players.stream()
                                .map(Player::getTeamId)
                                .filter(java.util.Objects::nonNull)
                                .distinct()
                                .toList();

                if (teamIds.isEmpty()) {
                        return Map.of();
                }

                return matchdayDifficultyService.resolveForTeams(
                                leagueId,
                                teamIds);
        }

        private double calculateCurrentLineupScore(
                        List<Player> squadPlayers,
                        Formation currentFormation,
                        Map<Long, OpponentDifficulty> difficultyByTeamId) {

                List<Player> currentStarters = squadPlayers.stream()
                                .filter(Player::isStarter)
                                .toList();

                FormationLineup currentLineup = calculateBestFormationLineup(
                                currentStarters,
                                currentFormation,
                                difficultyByTeamId);

                if (currentLineup.score() == IMPOSSIBLE_FORMATION_SCORE) {
                        return 0;
                }

                return currentLineup.score();
        }

        private boolean isPlayerModifiable(
                        Player player,
                        Map<Long, Boolean> modifiableByTeamId) {

                /*
                 * Si todavía no tenemos contexto de jornada persistido,
                 * mantenemos el comportamiento normal del recomendador.
                 */
                if (modifiableByTeamId == null
                                || modifiableByTeamId.isEmpty()) {

                        return true;
                }

                /*
                 * Si ya tenemos contexto pero desconocemos el equipo,
                 * no asumimos que el jugador pueda modificarse.
                 */
                if (player == null
                                || player.getTeamId() == null) {

                        return false;
                }

                return Boolean.TRUE.equals(
                                modifiableByTeamId.get(
                                                player.getTeamId()));
        }

        private List<RecommendedLineupChangeResponse> calculateRecommendedLineupChanges(
                        List<Player> squadPlayers,
                        FormationLineup recommendedLineup) {

                List<Player> currentStarters = squadPlayers.stream()
                                .filter(Player::isStarter)
                                .toList();

                List<Player> recommendedStarters = recommendedLineup.assignments()
                                .stream()
                                .map(FormationAssignment::player)
                                .toList();

                List<RecommendedLineupChangeResponse> changes = new ArrayList<>();

                for (Player currentStarter : currentStarters) {

                        boolean remainsStarter = recommendedStarters.stream()
                                        .anyMatch(player -> player.getId()
                                                        .equals(
                                                                        currentStarter.getId()));

                        if (!remainsStarter) {

                                changes.add(
                                                new RecommendedLineupChangeResponse(
                                                                "OUT",
                                                                currentStarter.getId(),
                                                                currentStarter.getName()));
                        }
                }

                for (Player recommendedStarter : recommendedStarters) {

                        boolean alreadyStarter = currentStarters.stream()
                                        .anyMatch(player -> player.getId()
                                                        .equals(
                                                                        recommendedStarter.getId()));

                        if (!alreadyStarter) {

                                changes.add(
                                                new RecommendedLineupChangeResponse(
                                                                "IN",
                                                                recommendedStarter.getId(),
                                                                recommendedStarter.getName()));
                        }
                }

                return List.copyOf(changes);
        }

        private double calculateAvailabilityWeight(
                        PlayerStatus status) {

                if (status == null) {
                        return 1.0;
                }

                return switch (status) {
                        case OK -> 1.0;
                        case WARNED -> 1.0;
                        case DOUBT -> 0.5;
                        case INJURED -> 0.0;
                        case SANCTIONED -> 0.0;
                        case DISCARDED -> 0.0;
                        case UNKNOWN -> 1.0;
                };
        }

        private Map<String, Double> countEffectiveAvailabilityByPosition(
                        List<Player> players) {

                Map<String, Double> result = createEmptyAvailabilityPositionMap();

                for (Player player : players) {

                        double availability = calculateAvailabilityWeight(
                                        player.getStatus());

                        for (PlayerPosition position : player.getPositions()) {

                                if (position == PlayerPosition.E) {
                                        continue;
                                }

                                result.compute(
                                                position.name(),
                                                (key, current) -> (current == null ? 0.0 : current)
                                                                + availability);
                        }
                }

                return result;
        }

        private Map<String, Double> createEmptyAvailabilityPositionMap() {
                Map<String, Double> positions = new LinkedHashMap<>();

                positions.put("PT", 0.0);
                positions.put("DF", 0.0);
                positions.put("MC", 0.0);
                positions.put("DL", 0.0);

                return positions;
        }

        private Formation findFormation(
                        String formationName) {

                return VALID_FORMATIONS
                                .stream()
                                .filter(formation -> formationName(formation)
                                                .equals(formationName))
                                .findFirst()
                                .orElse(null);
        }

        private String formationName(
                        Formation formation) {

                return formation.defenders()
                                + "-"
                                + formation.midfielders()
                                + "-"
                                + formation.forwards();
        }

        private List<PlayerPosition> buildRequiredPositions(
                        Formation formation) {

                List<PlayerPosition> requiredPositions = new java.util.ArrayList<>();

                requiredPositions.add(PlayerPosition.PT);

                for (int i = 0; i < formation.defenders(); i++) {
                        requiredPositions.add(PlayerPosition.DF);
                }

                for (int i = 0; i < formation.midfielders(); i++) {
                        requiredPositions.add(PlayerPosition.MC);
                }

                for (int i = 0; i < formation.forwards(); i++) {
                        requiredPositions.add(PlayerPosition.DL);
                }

                return requiredPositions;
        }

        private int calculateCoverageForRequiredPositions(
                        List<Player> players,
                        List<PlayerPosition> requiredPositions) {

                int[] playerAssignedToSlot = new int[requiredPositions.size()];

                java.util.Arrays.fill(
                                playerAssignedToSlot,
                                -1);

                int matches = 0;

                for (int playerIndex = 0; playerIndex < players.size(); playerIndex++) {

                        Player player = players.get(playerIndex);

                        if (calculateAvailabilityWeight(
                                        player.getStatus()) <= 0) {
                                continue;
                        }

                        boolean[] visitedSlots = new boolean[requiredPositions.size()];

                        if (tryAssignPlayerToSlot(
                                        playerIndex,
                                        players,
                                        requiredPositions,
                                        playerAssignedToSlot,
                                        visitedSlots)) {

                                matches++;
                        }
                }

                return matches;
        }

        private int calculateFormationCoverage(
                        List<Player> players,
                        Formation formation) {

                return calculateCoverageForRequiredPositions(
                                players,
                                buildRequiredPositions(formation));
        }

        private double calculateFormationPerformanceScore(
                        List<Player> players,
                        Formation formation,
                        Map<Long, OpponentDifficulty> difficultyByTeamId) {

                List<PlayerPosition> requiredPositions = buildRequiredPositions(formation);

                /*
                 * Si ni siquiera podemos cubrir los 11 puestos
                 * de la formación, no puede ser candidata.
                 */
                if (calculateFormationCoverage(
                                players,
                                formation) < requiredPositions.size()) {

                        return IMPOSSIBLE_FORMATION_SCORE;
                }

                Map<Long, Double> memo = new HashMap<>();

                return maximizeFormationPerformance(
                                players,
                                requiredPositions,
                                0,
                                0,
                                memo,
                                difficultyByTeamId,
                                Set.of());
        }

        private FormationLineup calculateBestFormationLineup(
                        List<Player> players,
                        Formation formation,
                        Map<Long, OpponentDifficulty> difficultyByTeamId) {

                return calculateBestFormationLineup(
                                players,
                                formation,
                                difficultyByTeamId,
                                Set.of());
        }

        private FormationLineup calculateBestFormationLineup(
                        List<Player> players,
                        Formation formation,
                        Map<Long, OpponentDifficulty> difficultyByTeamId,
                        Set<Long> requiredPlayerIds) {

                List<PlayerPosition> requiredPositions = buildRequiredPositions(
                                formation);

                if (calculateFormationCoverage(
                                players,
                                formation) < requiredPositions.size()) {

                        return new FormationLineup(
                                        formation,
                                        IMPOSSIBLE_FORMATION_SCORE,
                                        List.of());
                }

                Map<Long, Double> memo = new HashMap<>();

                double score = maximizeFormationPerformance(
                                players,
                                requiredPositions,
                                0,
                                0,
                                memo,
                                difficultyByTeamId,
                                requiredPlayerIds);

                if (score == IMPOSSIBLE_FORMATION_SCORE) {

                        return new FormationLineup(
                                        formation,
                                        IMPOSSIBLE_FORMATION_SCORE,
                                        List.of());
                }

                List<FormationAssignment> assignments = reconstructFormationAssignments(
                                players,
                                requiredPositions,
                                0,
                                0,
                                memo,
                                difficultyByTeamId,
                                requiredPlayerIds);

                return new FormationLineup(
                                formation,
                                score,
                                List.copyOf(assignments));
        }

        private double maximizeFormationPerformance(
                        List<Player> players,
                        List<PlayerPosition> requiredPositions,
                        int playerIndex,
                        int occupiedSlotsMask,
                        Map<Long, Double> memo,
                        Map<Long, OpponentDifficulty> difficultyByTeamId,
                        Set<Long> requiredPlayerIds) {

                int allSlotsMask = (1 << requiredPositions.size()) - 1;

                if (occupiedSlotsMask == allSlotsMask) {

                        boolean requiredPlayerStillPending = hasRequiredPlayerFromIndex(
                                        players,
                                        playerIndex,
                                        requiredPlayerIds);

                        return requiredPlayerStillPending
                                        ? IMPOSSIBLE_FORMATION_SCORE
                                        : 0;
                }

                if (playerIndex >= players.size()) {
                        return IMPOSSIBLE_FORMATION_SCORE;
                }

                long state = (((long) playerIndex) << 32)
                                | (occupiedSlotsMask
                                                & 0xffffffffL);

                Double cached = memo.get(state);

                if (cached != null) {
                        return cached;
                }

                Player player = players.get(playerIndex);

                boolean requiredPlayer = requiredPlayerIds != null
                                && requiredPlayerIds.contains(
                                                player.getId());

                /*
                 * Un jugador obligatorio no puede ser omitido.
                 */
                double best = requiredPlayer
                                ? IMPOSSIBLE_FORMATION_SCORE
                                : maximizeFormationPerformance(
                                                players,
                                                requiredPositions,
                                                playerIndex + 1,
                                                occupiedSlotsMask,
                                                memo,
                                                difficultyByTeamId,
                                                requiredPlayerIds);

                double availability = calculateAvailabilityWeight(
                                player.getStatus());

                if (availability > 0) {

                        for (int slotIndex = 0; slotIndex < requiredPositions.size(); slotIndex++) {

                                int slotBit = 1 << slotIndex;

                                if ((occupiedSlotsMask & slotBit) != 0) {
                                        continue;
                                }

                                PlayerPosition requiredPosition = requiredPositions.get(
                                                slotIndex);

                                if (!player.getPositions()
                                                .contains(requiredPosition)) {

                                        continue;
                                }

                                double remainingScore = maximizeFormationPerformance(
                                                players,
                                                requiredPositions,
                                                playerIndex + 1,
                                                occupiedSlotsMask
                                                                | slotBit,
                                                memo,
                                                difficultyByTeamId,
                                                requiredPlayerIds);

                                if (remainingScore <= IMPOSSIBLE_FORMATION_SCORE
                                                + 1) {

                                        continue;
                                }

                                double playerRating = calculateFormationPlayerRating(
                                                player,
                                                requiredPosition,
                                                difficultyByTeamId);

                                double candidateScore = playerRating
                                                + remainingScore;

                                best = Math.max(
                                                best,
                                                candidateScore);
                        }
                }

                memo.put(
                                state,
                                best);

                return best;
        }

        private boolean hasRequiredPlayerFromIndex(
                        List<Player> players,
                        int playerIndex,
                        Set<Long> requiredPlayerIds) {

                if (requiredPlayerIds == null
                                || requiredPlayerIds.isEmpty()) {

                        return false;
                }

                for (int index = playerIndex; index < players.size(); index++) {

                        if (requiredPlayerIds.contains(
                                        players.get(index).getId())) {

                                return true;
                        }
                }

                return false;
        }

        private List<FormationAssignment> reconstructFormationAssignments(
                        List<Player> players,
                        List<PlayerPosition> requiredPositions,
                        int playerIndex,
                        int occupiedSlotsMask,
                        Map<Long, Double> memo,
                        Map<Long, OpponentDifficulty> difficultyByTeamId,
                        Set<Long> requiredPlayerIds) {

                int allSlotsMask = (1 << requiredPositions.size()) - 1;

                if (occupiedSlotsMask == allSlotsMask
                                || playerIndex >= players.size()) {

                        return new ArrayList<>();
                }

                double bestScore = maximizeFormationPerformance(
                                players,
                                requiredPositions,
                                playerIndex,
                                occupiedSlotsMask,
                                memo,
                                difficultyByTeamId,
                                requiredPlayerIds);

                Player player = players.get(playerIndex);

                boolean requiredPlayer = requiredPlayerIds != null
                                && requiredPlayerIds.contains(
                                                player.getId());

                /*
                 * Solo intentamos omitir al jugador si no está bloqueado
                 * como titular obligatorio.
                 */
                if (!requiredPlayer) {

                        double skipScore = maximizeFormationPerformance(
                                        players,
                                        requiredPositions,
                                        playerIndex + 1,
                                        occupiedSlotsMask,
                                        memo,
                                        difficultyByTeamId,
                                        requiredPlayerIds);

                        if (scoresAreEqual(
                                        bestScore,
                                        skipScore)) {

                                return reconstructFormationAssignments(
                                                players,
                                                requiredPositions,
                                                playerIndex + 1,
                                                occupiedSlotsMask,
                                                memo,
                                                difficultyByTeamId,
                                                requiredPlayerIds);
                        }
                }

                double availability = calculateAvailabilityWeight(
                                player.getStatus());

                if (availability > 0) {

                        for (int slotIndex = 0; slotIndex < requiredPositions.size(); slotIndex++) {

                                int slotBit = 1 << slotIndex;

                                if ((occupiedSlotsMask & slotBit) != 0) {
                                        continue;
                                }

                                PlayerPosition requiredPosition = requiredPositions.get(
                                                slotIndex);

                                if (!player.getPositions()
                                                .contains(requiredPosition)) {

                                        continue;
                                }

                                double remainingScore = maximizeFormationPerformance(
                                                players,
                                                requiredPositions,
                                                playerIndex + 1,
                                                occupiedSlotsMask
                                                                | slotBit,
                                                memo,
                                                difficultyByTeamId,
                                                requiredPlayerIds);

                                if (remainingScore <= IMPOSSIBLE_FORMATION_SCORE
                                                + 1) {

                                        continue;
                                }

                                double playerRating = calculateFormationPlayerRating(
                                                player,
                                                requiredPosition,
                                                difficultyByTeamId);

                                double candidateScore = playerRating
                                                + remainingScore;

                                if (!scoresAreEqual(
                                                bestScore,
                                                candidateScore)) {

                                        continue;
                                }

                                List<FormationAssignment> assignments = new ArrayList<>();

                                assignments.add(
                                                new FormationAssignment(
                                                                player,
                                                                requiredPosition,
                                                                playerRating));

                                assignments.addAll(
                                                reconstructFormationAssignments(
                                                                players,
                                                                requiredPositions,
                                                                playerIndex + 1,
                                                                occupiedSlotsMask
                                                                                | slotBit,
                                                                memo,
                                                                difficultyByTeamId,
                                                                requiredPlayerIds));

                                return assignments;
                        }
                }

                return new ArrayList<>();
        }

        private boolean scoresAreEqual(
                        double first,
                        double second) {

                return Math.abs(first - second) < 0.000001;
        }

        private double calculateFormationPlayerRating(
                        Player player,
                        PlayerPosition position,
                        Map<Long, OpponentDifficulty> difficultyByTeamId) {

                PlayerPerformanceSignals performance = playerPerformanceSignalService
                                .analyze(player);

                boolean hasRecentData = performance.recentSampleSize() >= 2;

                boolean hasHistoricalData = performance.historicalSampleSize() >= 5;

                double rating;

                if (hasRecentData
                                && hasHistoricalData) {

                        rating = performance.recentWeightedAverage()
                                        * 0.65
                                        + performance.historicalAveragePoints()
                                                        * 0.35;

                } else if (hasRecentData) {

                        rating = performance.recentWeightedAverage();

                } else if (hasHistoricalData) {

                        rating = performance.historicalAveragePoints();

                } else {

                        /*
                         * No inventamos rendimiento cuando todavía
                         * no disponemos de muestra suficiente.
                         */
                        rating = 0;
                }

                double availabilityAdjustedRating = rating
                                * calculateAvailabilityWeight(
                                                player.getStatus());

                if (availabilityAdjustedRating == 0) {
                        return 0;
                }

                if (difficultyByTeamId == null
                                || difficultyByTeamId.isEmpty()
                                || player.getTeamId() == null) {

                        return availabilityAdjustedRating;
                }

                OpponentDifficulty difficulty = difficultyByTeamId.get(
                                player.getTeamId());

                if (difficulty == null) {
                        return availabilityAdjustedRating;
                }

                double positionDifficulty = switch (position) {
                        case PT, DF ->
                                difficulty.attackingStrength() * 0.70
                                                + difficulty.overallDifficulty() * 0.30;

                        case MC ->
                                difficulty.overallDifficulty();

                        case DL ->
                                difficulty.defensiveStrength() * 0.70
                                                + difficulty.overallDifficulty() * 0.30;

                        default ->
                                difficulty.overallDifficulty();
                };

                double difficultyMultiplier = 1
                                + ((50
                                                - positionDifficulty)
                                                / 50.0)
                                                * MATCHDAY_DIFFICULTY_MAX_ADJUSTMENT;

                return availabilityAdjustedRating
                                * difficultyMultiplier;
        }

        private int calculateFormationRecommendationConfidence(
                        double improvement) {

                if (improvement <= 0) {
                        return 0;
                }

                int confidence = 55
                                + Math.min(
                                                (int) Math.round(
                                                                improvement * 5),
                                                35);

                return Math.min(
                                confidence,
                                90);
        }

        private boolean tryAssignPlayerToSlot(
                        int playerIndex,
                        List<Player> players,
                        List<PlayerPosition> requiredPositions,
                        int[] playerAssignedToSlot,
                        boolean[] visitedSlots) {

                Player player = players.get(playerIndex);

                for (int slotIndex = 0; slotIndex < requiredPositions.size(); slotIndex++) {

                        if (visitedSlots[slotIndex]) {
                                continue;
                        }

                        PlayerPosition requiredPosition = requiredPositions.get(slotIndex);

                        if (!player.getPositions().contains(
                                        requiredPosition)) {
                                continue;
                        }

                        visitedSlots[slotIndex] = true;

                        int currentlyAssignedPlayer = playerAssignedToSlot[slotIndex];

                        if (currentlyAssignedPlayer == -1
                                        || tryAssignPlayerToSlot(
                                                        currentlyAssignedPlayer,
                                                        players,
                                                        requiredPositions,
                                                        playerAssignedToSlot,
                                                        visitedSlots)) {

                                playerAssignedToSlot[slotIndex] = playerIndex;

                                return true;
                        }
                }

                return false;
        }

        private int calculateFormationCoverageWithExtraPosition(
                        List<Player> players,
                        Formation formation,
                        PlayerPosition extraPosition) {

                int currentCoverage = calculateFormationCoverage(
                                players,
                                formation);

                if (currentCoverage >= 11) {
                        return 11;
                }

                if (canExtraPositionImproveCoverage(
                                players,
                                formation,
                                extraPosition)) {

                        return Math.min(
                                        currentCoverage + 1,
                                        11);
                }

                return currentCoverage;
        }

        private boolean canExtraPositionImproveCoverage(
                        List<Player> players,
                        Formation formation,
                        PlayerPosition extraPosition) {

                List<PlayerPosition> requiredPositions = buildRequiredPositions(formation);

                int slotToRemove = -1;

                for (int i = 0; i < requiredPositions.size(); i++) {

                        if (requiredPositions.get(i) == extraPosition) {

                                slotToRemove = i;
                                break;
                        }
                }

                if (slotToRemove == -1) {
                        return false;
                }

                int currentCoverage = calculateFormationCoverage(
                                players,
                                formation);

                requiredPositions.remove(slotToRemove);

                int coverageWithoutThatSlot = calculateCoverageForRequiredPositions(
                                players,
                                requiredPositions);

                return coverageWithoutThatSlot >= currentCoverage;
        }

        private int calculateFormationNeedScore(
                        List<Player> squadPlayers,
                        PlayerPosition position) {

                int usefulImprovements = 0;
                int totalMissingSlots = 0;

                for (Formation formation : VALID_FORMATIONS) {

                        int currentCoverage = calculateFormationCoverage(
                                        squadPlayers,
                                        formation);

                        int missingSlots = 11 - currentCoverage;

                        if (missingSlots <= 0) {
                                continue;
                        }

                        totalMissingSlots += missingSlots;

                        int reinforcedCoverage = calculateFormationCoverageWithExtraPosition(
                                        squadPlayers,
                                        formation,
                                        position);

                        usefulImprovements += Math.max(
                                        reinforcedCoverage
                                                        - currentCoverage,
                                        0);
                }

                if (totalMissingSlots == 0) {
                        return 0;
                }

                double usefulness = (double) usefulImprovements
                                / totalMissingSlots;

                return (int) Math.round(
                                usefulness * 25);
        }

        private Map<String, Integer> calculateNeedScores(
                        Map<String, Double> effectiveAvailabilityByPosition, List<Player> squadPlayers) {

                Map<String, Integer> targetDepth = new LinkedHashMap<>();

                targetDepth.put("PT", 2);
                targetDepth.put("DF", 5);
                targetDepth.put("MC", 5);
                targetDepth.put("DL", 4);

                Map<String, Integer> needScores = new LinkedHashMap<>();

                for (Map.Entry<String, Integer> entry : targetDepth.entrySet()) {

                        String position = entry.getKey();
                        int target = entry.getValue();

                        double available = effectiveAvailabilityByPosition
                                        .getOrDefault(
                                                        position,
                                                        0.0);

                        double missing = Math.max(
                                        target - available,
                                        0.0);

                        /*
                         * Cada jugador efectivo que falta respecto
                         * a la profundidad recomendada aporta
                         * 25 puntos de necesidad.
                         *
                         * Un jugador DOUBT cuenta como 0.5,
                         * por lo que genera aproximadamente
                         * 12-13 puntos de necesidad.
                         */
                        int depthScore = (int) Math.round(
                                        missing * 25);

                        PlayerPosition playerPosition = PlayerPosition.valueOf(
                                        position);

                        int formationScore = calculateFormationNeedScore(
                                        squadPlayers,
                                        playerPosition);

                        int score = depthScore
                                        + formationScore;

                        score = Math.min(
                                        score,
                                        100);

                        needScores.put(
                                        position,
                                        score);
                }

                return needScores;
        }

        private Map<String, Integer> countPositions(
                        List<Player> players) {
                Map<String, Integer> result = createEmptyPositionMap();

                for (Player player : players) {
                        for (PlayerPosition position : player.getPositions()) {

                                /*
                                 * E = entrenador.
                                 * No lo contamos para detectar necesidades
                                 * de plantilla de jugadores.
                                 */
                                if (position == PlayerPosition.E) {
                                        continue;
                                }

                                result.compute(
                                                position.name(),
                                                (key, current) -> current == null
                                                                ? 1
                                                                : current + 1);
                        }
                }

                return result;
        }

        private Map<String, Integer> createEmptyPositionMap() {
                Map<String, Integer> positions = new LinkedHashMap<>();

                positions.put("PT", 0);
                positions.put("DF", 0);
                positions.put("MC", 0);
                positions.put("DL", 0);

                return positions;
        }

        private RecommendationType resolveRecommendation(
                        int score) {
                if (score >= 80) {
                        return RecommendationType.STRONG_BUY;
                }

                if (score >= 60) {
                        return RecommendationType.BUY;
                }

                if (score >= 40) {
                        return RecommendationType.WATCH;
                }

                return RecommendationType.AVOID;
        }

        private int clamp(
                        int value,
                        int min,
                        int max) {
                return Math.max(
                                min,
                                Math.min(max, value));
        }

        private double round(double value) {
                return Math.round(value * 100.0) / 100.0;
        }
}