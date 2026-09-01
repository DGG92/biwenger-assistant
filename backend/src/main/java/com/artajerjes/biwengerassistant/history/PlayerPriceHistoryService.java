package com.artajerjes.biwengerassistant.history;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailResponse;
import com.artajerjes.biwengerassistant.history.dto.PlayerPriceHistorySyncResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerNotFoundException;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@Service
public class PlayerPriceHistoryService {

    private final PlayerRepository playerRepository;
    private final PlayerPriceHistoryRepository playerPriceHistoryRepository;
    private final BiwengerClient biwengerClient;
    @Value("${biwenger.prices-sync.batch-size:25}")
    private int pricesSyncBatchSize;

    public PlayerPriceHistoryService(
            PlayerRepository playerRepository,
            PlayerPriceHistoryRepository playerPriceHistoryRepository,
            BiwengerClient biwengerClient) {

        this.playerRepository = playerRepository;
        this.playerPriceHistoryRepository = playerPriceHistoryRepository;
        this.biwengerClient = biwengerClient;
    }

    @Transactional
    public int syncPlayerPriceHistory(
            Long leagueId,
            Long playerId) {

        Player player = playerRepository
                .findByIdAndLeague_Id(
                        playerId,
                        leagueId)
                .orElseThrow(
                        () -> new PlayerNotFoundException(
                                playerId,
                                leagueId));

        if (player.getSlug() == null
                || player.getSlug().isBlank()) {

            return 0;
        }

        BiwengerPlayerDetailResponse response = biwengerClient
                .getPlayerDetail(
                        player.getSlug());

        return syncPlayerPriceHistory(
                player,
                response);
    }

    @Transactional
    public int syncPlayerPriceHistory(
            Player player,
            BiwengerPlayerDetailResponse response) {

        if (player == null
                || response == null
                || response.data() == null
                || response.data().prices() == null) {

            return 0;
        }

        LocalDateTime capturedAt = LocalDateTime.now();

        int processed = 0;

        for (List<Long> rawPrice : response.data().prices()) {

            if (rawPrice == null
                    || rawPrice.size() < 2
                    || rawPrice.get(0) == null
                    || rawPrice.get(1) == null) {

                continue;
            }

            LocalDate priceDate = parsePriceDate(
                    rawPrice.get(0));

            Long marketValue = rawPrice.get(1);

            PlayerPriceHistory entity = playerPriceHistoryRepository
                    .findByPlayerIdAndPriceDate(
                            player.getId(),
                            priceDate)
                    .orElse(null);

            if (entity == null) {

                entity = new PlayerPriceHistory(
                        player.getId(),
                        player.getLeague().getId(),
                        priceDate,
                        marketValue,
                        PlayerPriceSource.BIWENGER_DETAIL,
                        capturedAt);

            } else {

                entity.update(
                        marketValue,
                        PlayerPriceSource.BIWENGER_DETAIL,
                        capturedAt);
            }

            playerPriceHistoryRepository.save(
                    entity);

            processed++;
        }

        return processed;
    }

    public PlayerPriceHistorySyncResponse syncLeaguePriceHistory(
            Long leagueId) {

        List<Player> players = playerRepository
                .findAllByLeague_Id(
                        leagueId);

        Set<Long> playerIdsWithHistory = new HashSet<>(
                playerPriceHistoryRepository
                        .findPlayerIdsWithHistoryByLeagueId(
                                leagueId));

        List<Player> eligiblePlayers = players.stream()
                .filter(
                        player -> player.getSlug() != null
                                && !player.getSlug().isBlank())
                .filter(
                        player -> !playerIdsWithHistory.contains(
                                player.getId()))
                .sorted(
                        Comparator.comparing(
                                Player::getId))
                .toList();

        int playersEligible = eligiblePlayers.size();

        List<Player> playersToProcess = eligiblePlayers.stream()
                .limit(pricesSyncBatchSize)
                .toList();

        int playersAttempted = 0;
        int playersCompleted = 0;
        int pricesProcessed = 0;

        Long lastCompletedPlayerId = null;
        Long rateLimitedPlayerId = null;

        boolean completed = true;
        String stopReason = null;

        for (Player player : playersToProcess) {

            playersAttempted++;

            try {

                pricesProcessed += syncPlayerPriceHistory(
                        leagueId,
                        player.getId());

                playersCompleted++;
                lastCompletedPlayerId = player.getId();

            } catch (HttpClientErrorException.TooManyRequests exception) {

                completed = false;
                stopReason = "RATE_LIMIT";
                rateLimitedPlayerId = player.getId();

                break;
            }
        }

        return new PlayerPriceHistorySyncResponse(
                players.size(),
                playersEligible,
                playersAttempted,
                playersCompleted,
                pricesProcessed,
                completed,
                stopReason,
                lastCompletedPlayerId,
                rateLimitedPlayerId);
    }

    LocalDate parsePriceDate(Long rawDate) {

        if (rawDate == null) {
            throw new IllegalArgumentException(
                    "Biwenger price date cannot be null");
        }

        String value = String.format(
                "%06d",
                rawDate);

        int year = 2000
                + Integer.parseInt(
                        value.substring(
                                0,
                                2));

        int month = Integer.parseInt(
                value.substring(
                        2,
                        4));

        int day = Integer.parseInt(
                value.substring(
                        4,
                        6));

        return LocalDate.of(
                year,
                month,
                day);
    }
}