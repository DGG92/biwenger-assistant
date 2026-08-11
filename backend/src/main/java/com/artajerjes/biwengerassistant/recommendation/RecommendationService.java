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
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;

@Service
public class RecommendationService {

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

                int score = calculateScore(
                                player,
                                differencePercentage,
                                affordable,
                                squadNeedScore);

                /*
                 * En una subasta que ya ha superado nuestro límite
                 * recomendado, nunca queremos recomendar comprar.
                 */
                if (listing.getType() == MarketListingType.AUCTION
                                && maximumRecommendedBid != null
                                && effectivePrice > maximumRecommendedBid) {
                        score = Math.min(score, 25);
                }

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
                                player.isInjured(),
                                affordable,
                                score,
                                resolveRecommendation(score));
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
                 * Un lesionado requiere mayor margen de seguridad.
                 */
                if (player.isInjured()) {
                        multiplier -= 0.15;
                }

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

        private int calculateScore(
                        Player player,
                        double differencePercentage,
                        boolean affordable,
                        int squadNeedScore) {
                double score = 50;

                double priceScore = differencePercentage * 3;

                score += clampDouble(
                                priceScore,
                                -30,
                                30);

                if (player.getMarketValue() != null
                                && player.getMarketValue() > 0
                                && player.getValueFluctuation() != null) {
                        double fluctuationPercentage = ((double) player.getValueFluctuation()
                                        / player.getMarketValue())
                                        * 100;

                        double fluctuationScore = fluctuationPercentage * 7;

                        score += clampDouble(
                                        fluctuationScore,
                                        -25,
                                        25);
                }

                /*
                 * Necesidad de plantilla.
                 *
                 * needScore va de 0 a 100.
                 * Lo convertimos en un bonus máximo de +20.
                 */
                score += squadNeedScore * 0.20;

                score += calculateRecentFormScore(player);

                if (player.isInjured()) {
                        score -= 30;
                }

                if (!affordable) {
                        score = Math.min(score, 25);
                }

                return clamp(
                                (int) Math.round(score),
                                0,
                                100);
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

        private int calculateRecentFormScore(Player player) {
                List<PlayerMatchReport> recentReports = playerMatchReportRepository
                                .findTop2ByPlayer_IdOrderByMatchDateDesc(
                                                player.getId());

                if (recentReports == null
                                || recentReports.size() < 2) {
                        return 0;
                }

                PlayerMatchReport latest = recentReports.get(0);

                PlayerMatchReport previous = recentReports.get(1);

                /*
                 * Necesitamos conocer la temporada de ambos partidos.
                 */
                if (latest.getSeason() == null
                                || previous.getSeason() == null) {
                        return 0;
                }

                /*
                 * Solo aceptamos partidos de la temporada actualmente en curso.
                 *
                 * Esto evita:
                 * - usar J38 + J37 de la temporada pasada antes de empezar esta;
                 * - mezclar J1 de esta temporada con J38 de la anterior.
                 */
                String currentSeason = getCurrentSeason();

                if (!currentSeason.equals(latest.getSeason())
                                || !currentSeason.equals(previous.getSeason())) {
                        return 0;
                }

                /*
                 * Una jornada sin participar rompe completamente la racha.
                 *
                 * Da igual que fuera por lesión, sanción, no convocatoria,
                 * suplencia sin minutos, etc.
                 */
                if (!latest.isParticipated()
                                || !previous.isParticipated()) {
                        return 0;
                }

                /*
                 * Si participó necesitamos puntuación.
                 *
                 * 0 es una puntuación válida.
                 * null significa que no tenemos una puntuación válida.
                 */
                if (latest.getPoints() == null
                                || previous.getPoints() == null) {
                        return 0;
                }

                int firstPoints = latest.getPoints();
                int secondPoints = previous.getPoints();

                double average = (firstPoints + secondPoints) / 2.0;

                if (firstPoints >= 8
                                && secondPoints >= 8) {
                        return 15;
                }

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
                                                .filter(Player::isInjured)
                                                .toList());

                Map<String, Integer> needScoreByPosition = calculateNeedScores(
                                playersByPosition,
                                injuredByPosition);

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

        private Map<String, Integer> calculateNeedScores(
                        Map<String, Integer> playersByPosition,
                        Map<String, Integer> injuredByPosition) {
                Map<String, Integer> targetDepth = new LinkedHashMap<>();

                targetDepth.put("PT", 2);
                targetDepth.put("DF", 5);
                targetDepth.put("MC", 5);
                targetDepth.put("DL", 4);

                Map<String, Integer> needScores = new LinkedHashMap<>();

                for (Map.Entry<String, Integer> entry : targetDepth.entrySet()) {

                        String position = entry.getKey();
                        int target = entry.getValue();

                        int current = playersByPosition.getOrDefault(
                                        position,
                                        0);

                        int injured = injuredByPosition.getOrDefault(
                                        position,
                                        0);

                        /*
                         * Cada jugador que falta respecto al objetivo
                         * aporta 25 puntos de necesidad.
                         */
                        int missing = Math.max(
                                        target - current,
                                        0);

                        int score = missing * 25;

                        /*
                         * Cada lesionado aumenta temporalmente
                         * la necesidad en 10 puntos.
                         */
                        score += injured * 10;

                        /*
                         * 0 = posición muy cubierta
                         * 100 = necesidad máxima
                         */
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

        private String getCurrentSeason() {
                LocalDate today = LocalDate.now();

                int year = today.getYear();

                if (today.getMonthValue() >= 7) {
                        return year + "-" + (year + 1);
                }

                return (year - 1) + "-" + year;
        }
}