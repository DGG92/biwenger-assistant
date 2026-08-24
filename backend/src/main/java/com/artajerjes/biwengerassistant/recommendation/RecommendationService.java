package com.artajerjes.biwengerassistant.recommendation;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.market.MarketListing;
import com.artajerjes.biwengerassistant.market.MarketListingRepository;
import com.artajerjes.biwengerassistant.market.MarketListingType;
import com.artajerjes.biwengerassistant.offer.OfferService;
import com.artajerjes.biwengerassistant.offer.dto.EconomicStatusResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationReason;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketScoreBreakdown;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;

@Service
public class RecommendationService {

        private record Formation(
                        int defenders,
                        int midfielders,
                        int forwards) {
        }

        private record RecentFormAnalysis(
                        int score,
                        int sampleSize) {
        }

        private record HistoricalPerformanceAnalysis(
                        double averagePoints,
                        int sampleSize) {
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
        private final PlayerMatchReportRepository playerMatchReportRepository;

        @Value("${biwenger.user-id}")
        private Long biwengerUserId;

        public RecommendationService(
                        LeagueRepository leagueRepository,
                        MarketListingRepository marketListingRepository,
                        OfferService offerService,
                        PlayerRepository playerRepository,
                        PlayerMatchReportRepository playerMatchReportRepository) {
                this.leagueRepository = leagueRepository;
                this.marketListingRepository = marketListingRepository;
                this.offerService = offerService;
                this.playerRepository = playerRepository;
                this.playerMatchReportRepository = playerMatchReportRepository;
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

                return marketListingRepository
                                .findAllByLeague_Id(leagueId)
                                .stream()
                                .map(listing -> toRecommendation(
                                                listing,
                                                economicStatus.maximumBid(),
                                                squadNeeds.needScoreByPosition()))
                                .sorted(
                                                Comparator.comparingInt(
                                                                MarketRecommendationResponse::score).reversed())
                                .toList();
        }

        private MarketRecommendationResponse toRecommendation(
                        MarketListing listing,
                        Long maximumBid,
                        Map<String, Integer> needScoreByPosition) {
                Player player = listing.getPlayer();

                Long marketValue = player.getMarketValue();
                Long askingPrice = listing.getPrice();

                Long currentBid = listing.getType() == MarketListingType.AUCTION
                                ? listing.getLastBidAmount()
                                : null;

                Long maximumRecommendedBid = listing.getType() == MarketListingType.AUCTION
                                ? calculateMaximumRecommendedBid(
                                                player,
                                                maximumBid)
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

                RecentFormAnalysis recentForm = calculateRecentForm(player);

                int recentFormScore = recentForm.score();

                HistoricalPerformanceAnalysis historicalPerformance = calculateHistoricalPerformance(player);

                int historicalPerformanceScore = calculateHistoricalPerformanceScore(
                                historicalPerformance);

                MarketScoreBreakdown scoreBreakdown = calculateScoreBreakdown(
                                player,
                                differencePercentage,
                                squadNeedScore,
                                recentFormScore,
                                recentForm.sampleSize(),
                                historicalPerformance.averagePoints(),
                                historicalPerformance.sampleSize(),
                                historicalPerformanceScore);

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
                                historicalPerformanceScore);

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

        private List<MarketRecommendationReason> calculateReasons(
                        Player player,
                        double differencePercentage,
                        boolean affordable,
                        int squadNeedScore,
                        int recentFormScore,
                        int historicalPerformanceScore) {

                List<MarketRecommendationReason> reasons = new java.util.ArrayList<>();

                if (differencePercentage >= 5) {
                        reasons.add(
                                        MarketRecommendationReason.PRICE_BELOW_MARKET);
                }

                if (differencePercentage <= -5) {
                        reasons.add(
                                        MarketRecommendationReason.PRICE_ABOVE_MARKET);
                }

                if (player.getMarketValue() != null
                                && player.getMarketValue() > 0
                                && player.getValueFluctuation() != null) {

                        double fluctuationPercentage = ((double) player.getValueFluctuation()
                                        / player.getMarketValue())
                                        * 100;

                        if (fluctuationPercentage >= 3) {
                                reasons.add(
                                                MarketRecommendationReason.VALUE_RISING_FAST);
                        } else if (fluctuationPercentage > 0) {
                                reasons.add(
                                                MarketRecommendationReason.VALUE_RISING);
                        } else if (fluctuationPercentage < 0) {
                                reasons.add(
                                                MarketRecommendationReason.VALUE_FALLING);
                        }
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
                        Long userMaximumBid) {
                if (player.getMarketValue() == null
                                || player.getMarketValue() <= 0) {
                        return 0L;
                }

                double multiplier = 1.0;

                /*
                 * Tendencia positiva:
                 * permitimos pagar algo por encima del valor actual,
                 * pero con un máximo del 10 % adicional.
                 */
                if (player.getValueFluctuation() != null
                                && player.getValueFluctuation() > 0) {
                        double fluctuationPercentage = ((double) player.getValueFluctuation()
                                        / player.getMarketValue())
                                        * 100;

                        double premiumPercentage = Math.min(
                                        fluctuationPercentage * 2.5,
                                        12);

                        multiplier += premiumPercentage / 100;
                }

                /*
                 * Si está perdiendo valor no pagamos prima:
                 * reducimos el límite hasta un máximo del 10 %.
                 */
                if (player.getValueFluctuation() != null
                                && player.getValueFluctuation() < 0) {
                        double fluctuationPercentage = Math.abs(
                                        ((double) player.getValueFluctuation()
                                                        / player.getMarketValue())
                                                        * 100);

                        double discountPercentage = Math.min(
                                        fluctuationPercentage * 2.5,
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
                        int historicalPerformanceScore) {

                double baseScore = 50;

                double priceScore = clampDouble(
                                differencePercentage * 3,
                                -30,
                                30);

                double valueTrendScore = 0;

                if (player.getMarketValue() != null
                                && player.getMarketValue() > 0
                                && player.getValueFluctuation() != null) {

                        double fluctuationPercentage = ((double) player.getValueFluctuation()
                                        / player.getMarketValue())
                                        * 100;

                        valueTrendScore = (int) Math.round(
                                        clampDouble(
                                                        fluctuationPercentage * 7,
                                                        -25,
                                                        25));
                }

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

        private RecentFormAnalysis calculateRecentForm(Player player) {
                List<PlayerMatchReport> recentReports = playerMatchReportRepository
                                .findTop5ByPlayer_IdOrderByMatchDateDesc(
                                                player.getId());

                if (recentReports == null
                                || recentReports.size() < 2) {
                        return new RecentFormAnalysis(
                                        0,
                                        0);
                }

                List<PlayerMatchReport> streak = buildCurrentConsecutiveStreak(
                                recentReports);

                if (streak.size() < 2) {
                        return new RecentFormAnalysis(
                                        0,
                                        0);
                }

                double weightedPoints = 0;
                int totalWeight = 0;

                for (int i = 0; i < streak.size(); i++) {
                        PlayerMatchReport report = streak.get(i);

                        /*
                         * Los reports vienen ordenados del más reciente
                         * al más antiguo.
                         *
                         * Para una racha de 5:
                         * J5 -> peso 5
                         * J4 -> peso 4
                         * J3 -> peso 3
                         * J2 -> peso 2
                         * J1 -> peso 1
                         */
                        int weight = streak.size() - i;

                        weightedPoints += report.getPoints() * weight;
                        totalWeight += weight;
                }

                double average = totalWeight == 0
                                ? 0
                                : weightedPoints / totalWeight;

                boolean allExcellent = streak.stream()
                                .allMatch(report -> report.getPoints() >= 8);

                int sampleSize = streak.size();

                if (allExcellent) {
                        return new RecentFormAnalysis(
                                        15,
                                        sampleSize);
                }

                if (average >= 7) {
                        return new RecentFormAnalysis(
                                        10,
                                        sampleSize);
                }

                if (average >= 5) {
                        return new RecentFormAnalysis(
                                        5,
                                        sampleSize);
                }

                if (average >= 3) {
                        return new RecentFormAnalysis(
                                        0,
                                        sampleSize);
                }

                if (average >= 1) {
                        return new RecentFormAnalysis(
                                        -5,
                                        sampleSize);
                }

                return new RecentFormAnalysis(
                                -10,
                                sampleSize);
        }

        private HistoricalPerformanceAnalysis calculateHistoricalPerformance(
                        Player player) {

                if (player.getPositions() != null
                                && player.getPositions().contains(PlayerPosition.E)) {
                        return new HistoricalPerformanceAnalysis(
                                        0,
                                        0);
                }

                List<PlayerMatchReport> reports = playerMatchReportRepository
                                .findTop10ByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                player.getId());

                if (reports == null
                                || reports.isEmpty()) {
                        return new HistoricalPerformanceAnalysis(
                                        0,
                                        0);
                }

                double averagePoints = reports.stream()
                                .map(PlayerMatchReport::getPoints)
                                .mapToInt(Integer::intValue)
                                .average()
                                .orElse(0);

                return new HistoricalPerformanceAnalysis(
                                averagePoints,
                                reports.size());
        }

        private int calculateHistoricalPerformanceScore(
                        HistoricalPerformanceAnalysis historicalPerformance) {

                if (historicalPerformance == null
                                || historicalPerformance.sampleSize() < 5) {
                        return 0;
                }

                double average = historicalPerformance.averagePoints();

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

        private Integer extractRoundNumber(String roundShort) {
                if (roundShort == null) {
                        return null;
                }

                String normalized = roundShort
                                .trim()
                                .toUpperCase();

                if (!normalized.matches("J\\d+")) {
                        return null;
                }

                return Integer.parseInt(
                                normalized.substring(1));
        }

        private List<PlayerMatchReport> buildCurrentConsecutiveStreak(
                        List<PlayerMatchReport> recentReports) {

                if (recentReports == null
                                || recentReports.isEmpty()) {
                        return List.of();
                }

                String currentSeason = getCurrentSeason();

                List<PlayerMatchReport> streak = new java.util.ArrayList<>();

                Integer expectedRoundNumber = null;

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

                        Integer roundNumber = extractRoundNumber(
                                        report.getRoundShort());

                        if (roundNumber == null) {
                                break;
                        }

                        if (expectedRoundNumber != null
                                        && roundNumber != expectedRoundNumber) {
                                break;
                        }

                        streak.add(report);

                        expectedRoundNumber = roundNumber - 1;
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