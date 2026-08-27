package com.artajerjes.biwengerassistant.playerreport;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.beans.factory.annotation.Value;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerLeagueApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerReport;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.playerreport.dto.PlayerReportSyncResponse;

@Service
public class PlayerMatchReportService {

        private static final int CUSTOM_SCORE_ID = 100;

        private final BiwengerClient biwengerClient;
        private final PlayerRepository playerRepository;
        private final CustomScoreEvaluator customScoreEvaluator;
        private final PlayerMatchReportPersistenceService playerMatchReportPersistenceService;

        @Value("${biwenger.reports-sync.batch-size:25}")
        private int reportsSyncBatchSize;

        public PlayerMatchReportService(
                        BiwengerClient biwengerClient,
                        PlayerRepository playerRepository,
                        CustomScoreEvaluator customScoreEvaluator,
                        PlayerMatchReportPersistenceService playerMatchReportPersistenceService) {

                this.biwengerClient = biwengerClient;
                this.playerRepository = playerRepository;
                this.customScoreEvaluator = customScoreEvaluator;
                this.playerMatchReportPersistenceService = playerMatchReportPersistenceService;
        }

        @Transactional
        public int syncPlayerReports(Player player) {

                LeagueScoreConfig scoreConfig = loadLeagueScoreConfig();

                return syncPlayerReports(
                                player,
                                scoreConfig);
        }

        private int syncPlayerReports(
                        Player player,
                        LeagueScoreConfig scoreConfig) {

                if (player == null
                                || player.getSlug() == null
                                || player.getSlug().isBlank()) {

                        return 0;
                }

                BiwengerPlayerDetailResponse response = biwengerClient.getPlayerDetail(
                                player.getSlug());

                if (response == null
                                || response.data() == null
                                || response.data().reports() == null) {

                        return 0;
                }

                int processed = 0;

                for (BiwengerPlayerReport report : response.data().reports()) {

                        if (report == null
                                        || report.match() == null
                                        || report.match().id() == null
                                        || !"finished".equalsIgnoreCase(
                                                        report.match().status())) {

                                continue;
                        }

                        /*
                         * En las respuestas reales de Biwenger:
                         *
                         * - un jugador que ha participado dispone de points
                         * y/o rawStats;
                         * - un jugador que no ha participado puede tener
                         * únicamente status.
                         */
                        boolean participated = report.points() != null
                                        || report.rawStats() != null;

                        Integer leaguePoints = null;

                        if (participated) {

                                leaguePoints = resolveLeaguePoints(
                                                scoreConfig.scoreId(),
                                                scoreConfig.customScore(),
                                                report);

                                /*
                                 * Si Biwenger devuelve un informe incompleto no
                                 * sobrescribimos la información existente con una
                                 * puntuación que no podemos calcular con seguridad.
                                 */
                                if (leaguePoints == null) {
                                        continue;
                                }
                        }

                        processed += playerMatchReportPersistenceService
                                        .persistReport(
                                                        player,
                                                        report,
                                                        leaguePoints);
                }

                return processed;
        }

        public PlayerReportSyncResponse syncLeagueReports(
                        Long leagueId) {

                /*
                 * La configuración se consulta una sola vez
                 * para toda la sincronización.
                 */
                LeagueScoreConfig scoreConfig = loadLeagueScoreConfig();

                List<Player> players = playerRepository.findAllByLeague_Id(
                                leagueId);

                players = players.stream()
                                .sorted(
                                                Comparator
                                                                .comparing(
                                                                                Player::getReportsLastSyncAttemptAt,
                                                                                Comparator.nullsFirst(
                                                                                                Comparator.naturalOrder()))
                                                                .thenComparing(
                                                                                Player::getId))
                                .toList();

                List<Player> eligiblePlayers = players.stream()
                                .filter(player -> player.getSlug() != null
                                                && !player.getSlug().isBlank())
                                .toList();

                int playersEligible = eligiblePlayers.size();

                List<Player> playersToProcess = eligiblePlayers.stream()
                                .limit(reportsSyncBatchSize)
                                .toList();

                int playersAttempted = 0;
                int playersCompleted = 0;
                int reportsProcessed = 0;

                Long lastCompletedPlayerId = null;
                Long rateLimitedPlayerId = null;

                boolean completed = true;
                String stopReason = null;

                for (Player player : playersToProcess) {

                        playersAttempted++;

                        LocalDateTime attemptTime = LocalDateTime.now();

                        player.markReportsSyncAttempt(
                                        attemptTime);

                        playerRepository.save(player);

                        try {

                                reportsProcessed += syncPlayerReports(
                                                player,
                                                scoreConfig);

                                player.markReportsSyncSuccess(
                                                LocalDateTime.now());

                                playerRepository.save(player);

                                playersCompleted++;
                                lastCompletedPlayerId = player.getId();

                        } catch (HttpClientErrorException.TooManyRequests exception) {

                                completed = false;
                                stopReason = "RATE_LIMIT";
                                rateLimitedPlayerId = player.getId();

                                /*
                                 * Paramos inmediatamente.
                                 *
                                 * Los reports de jugadores anteriores
                                 * ya están persistidos mediante
                                 * transacciones independientes.
                                 */
                                break;
                        }
                }

                return new PlayerReportSyncResponse(
                                players.size(),
                                playersEligible,
                                playersAttempted,
                                playersCompleted,
                                reportsProcessed,
                                completed,
                                stopReason,
                                lastCompletedPlayerId,
                                rateLimitedPlayerId);
        }

        Integer resolveLeaguePoints(
                        Integer scoreId,
                        String customScore,
                        BiwengerPlayerReport report) {

                if (scoreId == null
                                || report == null) {

                        return null;
                }

                /*
                 * scoreID 100 = sistema personalizado de Biwenger.
                 *
                 * No existe points["100"]. El resultado se obtiene
                 * evaluando settings.customScore contra rawStats.
                 */
                if (scoreId == CUSTOM_SCORE_ID) {

                        if (customScore == null
                                        || customScore.isBlank()
                                        || report.rawStats() == null) {

                                return null;
                        }

                        return customScoreEvaluator.evaluate(
                                        customScore,
                                        report.rawStats());
                }

                /*
                 * Para sistemas estándar Biwenger ya devuelve directamente
                 * la puntuación dentro de:
                 *
                 * points["1"]
                 * points["2"]
                 * points["3"]
                 * ...
                 *
                 * Así esta lógica también queda preparada para otras ligas.
                 */
                if (report.points() == null) {
                        return null;
                }

                return report.points().get(
                                String.valueOf(scoreId));
        }

        private LeagueScoreConfig loadLeagueScoreConfig() {

                BiwengerLeagueApiResponse response = biwengerClient.getLeague();

                if (response == null
                                || response.data() == null
                                || response.data().scoreID() == null) {

                        throw new IllegalStateException(
                                        "Could not resolve Biwenger league scoring configuration");
                }

                Integer scoreId = response.data().scoreID();

                String customScore = response.data().settings() == null
                                ? null
                                : response.data()
                                                .settings()
                                                .customScore();

                if (scoreId == CUSTOM_SCORE_ID
                                && (customScore == null
                                                || customScore.isBlank())) {

                        throw new IllegalStateException(
                                        "Biwenger league uses custom scoring but customScore is missing");
                }

                return new LeagueScoreConfig(
                                scoreId,
                                customScore);
        }

        private record LeagueScoreConfig(
                        Integer scoreId,
                        String customScore) {
        }
}