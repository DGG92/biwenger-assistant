package com.artajerjes.biwengerassistant.biwenger;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.sync.PlayerDetailSyncResponse;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryService;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportService;
import com.artajerjes.biwengerassistant.playerreport.PlayerReportScoreConfig;

@Service
public class PlayerDetailSyncService {

        private final BiwengerClient biwengerClient;
        private final PlayerRepository playerRepository;
        private final PlayerPriceHistoryRepository playerPriceHistoryRepository;
        private final PlayerPriceHistoryService playerPriceHistoryService;
        private final PlayerMatchReportService playerMatchReportService;

        @Value("${biwenger.player-detail-sync.batch-size:25}")
        private int batchSize;

        public PlayerDetailSyncService(
                        BiwengerClient biwengerClient,
                        PlayerRepository playerRepository,
                        PlayerPriceHistoryRepository playerPriceHistoryRepository,
                        PlayerPriceHistoryService playerPriceHistoryService,
                        PlayerMatchReportService playerMatchReportService) {

                this.biwengerClient = biwengerClient;
                this.playerRepository = playerRepository;
                this.playerPriceHistoryRepository = playerPriceHistoryRepository;
                this.playerPriceHistoryService = playerPriceHistoryService;
                this.playerMatchReportService = playerMatchReportService;
        }

        public PlayerDetailSyncResponse syncLeaguePlayerDetails(
                        Long leagueId) {

                List<Player> players = playerRepository
                                .findAllByLeague_Id(
                                                leagueId);

                Set<Long> playerIdsWithPriceHistory = new HashSet<>(
                                playerPriceHistoryRepository
                                                .findPlayerIdsWithHistoryByLeagueId(
                                                                leagueId));

                /*
                 * Mientras exista backfill de precios pendiente,
                 * esos jugadores tienen prioridad absoluta.
                 *
                 * Cuando todos tengan histórico, la ordenación por
                 * reportsLastSyncSuccessAt convierte este proceso
                 * en una cola circular de actualización.
                 */
                List<Player> eligiblePlayers = players.stream()
                                .filter(
                                                player -> player.getSlug() != null
                                                                && !player.getSlug().isBlank())
                                .sorted(
                                                Comparator
                                                                .comparing(
                                                                                (Player player) -> playerIdsWithPriceHistory
                                                                                                .contains(player.getId()))
                                                                .thenComparing(
                                                                                Player::getReportsLastSyncSuccessAt,
                                                                                Comparator.nullsFirst(
                                                                                                Comparator.naturalOrder()))
                                                                .thenComparing(
                                                                                Player::getId))
                                .toList();

                List<Player> playersToProcess = eligiblePlayers.stream()
                                .limit(batchSize)
                                .toList();

                int playersAttempted = 0;
                int playersCompleted = 0;
                int pricesProcessed = 0;
                int reportsProcessed = 0;

                Long lastCompletedPlayerId = null;
                Long rateLimitedPlayerId = null;
                Long retryAfterSeconds = null;

                boolean completed = true;
                String stopReason = null;

                PlayerReportScoreConfig scoreConfig = null;

                if (!playersToProcess.isEmpty()) {
                        try {
                                scoreConfig = playerMatchReportService.loadLeagueScoreConfig();
                        } catch (HttpClientErrorException.TooManyRequests exception) {
                                return new PlayerDetailSyncResponse(
                                                players.size(),
                                                eligiblePlayers.size(),
                                                0,
                                                0,
                                                0,
                                                0,
                                                false,
                                                "RATE_LIMIT",
                                                null,
                                                null,
                                                BiwengerRateLimitUtils.extractRetryAfterSeconds(
                                                                exception));
                        }
                }

                for (Player player : playersToProcess) {

                        playersAttempted++;

                        /*
                         * Marcamos el intento antes de hacer la llamada.
                         *
                         * NO marcamos success hasta que la llamada HTTP
                         * y las dos persistencias hayan terminado.
                         */
                        player.markReportsSyncAttempt(
                                        LocalDateTime.now());

                        playerRepository.save(
                                        player);

                        try {

                                /*
                                 * ÚNICA llamada HTTP de detalle para este jugador.
                                 */
                                BiwengerPlayerDetailResponse response = biwengerClient
                                                .getPlayerDetail(
                                                                player.getSlug());

                                /*
                                 * La misma respuesta alimenta ambos subsistemas.
                                 */
                                pricesProcessed += playerPriceHistoryService
                                                .syncPlayerPriceHistory(
                                                                player,
                                                                response);

                                reportsProcessed += playerMatchReportService
                                                .syncPlayerReports(
                                                                player,
                                                                response,
                                                                scoreConfig);

                                /*
                                 * Solo ahora consideramos terminado al jugador.
                                 */
                                player.markReportsSyncSuccess(
                                                LocalDateTime.now());

                                playerRepository.save(
                                                player);

                                playersCompleted++;
                                lastCompletedPlayerId = player.getId();

                        } catch (HttpClientErrorException.TooManyRequests exception) {

                                completed = false;
                                stopReason = "RATE_LIMIT";
                                rateLimitedPlayerId = player.getId();

                                retryAfterSeconds = BiwengerRateLimitUtils.extractRetryAfterSeconds(
                                                exception);

                                /*
                                 * Muy importante:
                                 *
                                 * - este jugador NO recibe success;
                                 * - detenemos inmediatamente la tanda;
                                 * - al ordenar por reportsLastSyncSuccessAt,
                                 * seguirá estando entre los más antiguos;
                                 * - si además no tenía histórico de precios,
                                 * seguirá teniendo prioridad absoluta.
                                 *
                                 * Por tanto, no se salta.
                                 */
                                break;
                        }
                }

                return new PlayerDetailSyncResponse(
                                players.size(),
                                eligiblePlayers.size(),
                                playersAttempted,
                                playersCompleted,
                                pricesProcessed,
                                reportsProcessed,
                                completed,
                                stopReason,
                                lastCompletedPlayerId,
                                rateLimitedPlayerId,
                                retryAfterSeconds);
        }
}