package com.artajerjes.biwengerassistant.manager;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.auth.CurrentAssistantUserService;
import com.artajerjes.biwengerassistant.manager.dto.SquadProfitabilityPlayerResponse;
import com.artajerjes.biwengerassistant.manager.dto.SquadProfitabilityResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@Service
public class SquadProfitabilityService {

        private final PlayerRepository playerRepository;
        private final PlayerMatchReportRepository playerMatchReportRepository;
        private final CurrentAssistantUserService currentAssistantUserService;

        public SquadProfitabilityService(
                        PlayerRepository playerRepository,
                        PlayerMatchReportRepository playerMatchReportRepository,
                        CurrentAssistantUserService currentAssistantUserService) {

                this.playerRepository = playerRepository;
                this.playerMatchReportRepository = playerMatchReportRepository;
                this.currentAssistantUserService = currentAssistantUserService;
        }

        @Transactional(readOnly = true)
        public SquadProfitabilityResponse getSquadProfitability(
                        Long leagueId,
                        Long managerId) {

                Manager manager = currentAssistantUserService.getCurrentManager();

                if (manager.getLeague() == null
                                || !leagueId.equals(
                                                manager.getLeague().getId())) {
                        throw new AccessDeniedException(
                                        "Authenticated manager does not belong to league "
                                                        + leagueId);
                }

                if (!manager.getId().equals(managerId)) {
                        throw new AccessDeniedException(
                                        "Cannot access profitability for another manager");
                }

                Long authenticatedManagerId = manager.getId();

                List<Player> players = playerRepository
                                .findAllByOwner_IdAndLeague_Id(
                                                authenticatedManagerId,
                                                leagueId)
                                .stream()
                                .filter(player -> !player.getPositions().contains(
                                                PlayerPosition.E))
                                .toList();

                Map<Long, Integer> currentSeasonPointsByPlayer = new HashMap<>();

                for (Player player : players) {
                        currentSeasonPointsByPlayer.put(
                                        player.getId(),
                                        calculateCurrentSeasonPoints(player.getId()));
                }

                List<Player> playersWithPurchasePrice = players.stream()
                                .filter(player -> player.getPurchasePrice() != null)
                                .toList();

                long currentSquadValue = players.stream()
                                .map(Player::getMarketValue)
                                .filter(value -> value != null)
                                .mapToLong(Long::longValue)
                                .sum();

                long analyzedSquadValue = playersWithPurchasePrice.stream()
                                .map(Player::getMarketValue)
                                .filter(value -> value != null)
                                .mapToLong(Long::longValue)
                                .sum();

                long totalInvestment = playersWithPurchasePrice.stream()
                                .map(Player::getPurchasePrice)
                                .mapToLong(Long::longValue)
                                .sum();

                long unrealizedProfit = analyzedSquadValue
                                - totalInvestment;

                Double unrealizedProfitPercent = calculatePercentage(
                                totalInvestment,
                                analyzedSquadValue);

                int profitablePlayers = (int) playersWithPurchasePrice.stream()
                                .filter(player -> player.getProfitability() != null
                                                && player.getProfitability() > 0)
                                .count();

                int losingPlayers = (int) playersWithPurchasePrice.stream()
                                .filter(player -> player.getProfitability() != null
                                                && player.getProfitability() < 0)
                                .count();

                int breakEvenPlayers = (int) playersWithPurchasePrice.stream()
                                .filter(player -> player.getProfitability() != null
                                                && player.getProfitability() == 0)
                                .count();

                SquadProfitabilityPlayerResponse bestInvestment = playersWithPurchasePrice.stream()
                                .filter(player -> player.getProfitability() != null)
                                .max(Comparator.comparingLong(
                                                Player::getProfitability))
                                .map(player -> toPlayerResponse(
                                                player,
                                                currentSeasonPointsByPlayer.get(player.getId())))
                                .orElse(null);

                SquadProfitabilityPlayerResponse worstInvestment = playersWithPurchasePrice.stream()
                                .filter(player -> player.getProfitability() != null)
                                .min(Comparator.comparingLong(
                                                Player::getProfitability))
                                .map(player -> toPlayerResponse(
                                                player,
                                                currentSeasonPointsByPlayer.get(player.getId())))
                                .orElse(null);

                SquadProfitabilityPlayerResponse mostEfficientPlayer = players.stream()
                                .filter(player -> player.getMarketValue() != null
                                                && player.getMarketValue() > 0)
                                .max(Comparator.comparingDouble(
                                                player -> calculatePointsPerMillion(
                                                                currentSeasonPointsByPlayer.get(player.getId()),
                                                                player.getMarketValue())))
                                .map(player -> toPlayerResponse(
                                                player,
                                                currentSeasonPointsByPlayer.get(player.getId())))
                                .orElse(null);

                return new SquadProfitabilityResponse(
                                manager.getId(),
                                manager.getName(),
                                players.size(),
                                playersWithPurchasePrice.size(),
                                currentSquadValue,
                                analyzedSquadValue,
                                totalInvestment,
                                unrealizedProfit,
                                unrealizedProfitPercent,
                                profitablePlayers,
                                losingPlayers,
                                breakEvenPlayers,
                                bestInvestment,
                                worstInvestment,
                                mostEfficientPlayer);
        }

        private SquadProfitabilityPlayerResponse toPlayerResponse(
                        Player player,
                        Integer points) {

                Long purchasePrice = player.getPurchasePrice();
                Long currentValue = player.getMarketValue();
                Long unrealizedProfit = player.getProfitability();

                Double unrealizedProfitPercent = calculatePercentage(
                                purchasePrice,
                                currentValue);

                Double pointsPerMillion = currentValue == null
                                || currentValue <= 0
                                                ? null
                                                : calculatePointsPerMillion(
                                                                points,
                                                                currentValue);

                return new SquadProfitabilityPlayerResponse(
                                player.getId(),
                                player.getName(),
                                currentValue,
                                purchasePrice,
                                unrealizedProfit,
                                unrealizedProfitPercent,
                                points,
                                pointsPerMillion);
        }

        private Double calculatePercentage(
                        Long initialValue,
                        Long currentValue) {

                if (initialValue == null
                                || currentValue == null
                                || initialValue == 0) {
                        return null;
                }

                return ((double) (currentValue - initialValue)
                                / initialValue)
                                * 100.0;
        }

        private double calculatePointsPerMillion(
                        Integer points,
                        Long marketValue) {

                if (points == null
                                || marketValue == null
                                || marketValue <= 0) {
                        return 0.0;
                }

                return points
                                / (marketValue / 1_000_000.0);
        }

        private int calculateCurrentSeasonPoints(
                        Long playerId) {

                List<PlayerMatchReport> reports = playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                playerId);

                String season = reports.stream()
                                .map(PlayerMatchReport::getSeason)
                                .filter(reportSeason -> reportSeason != null
                                                && !reportSeason.isBlank())
                                .findFirst()
                                .orElse(null);

                if (season == null) {
                        return 0;
                }

                return reports.stream()
                                .filter(report -> season.equals(
                                                report.getSeason()))
                                .mapToInt(PlayerMatchReport::getPoints)
                                .sum();
        }
}