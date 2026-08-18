package com.artajerjes.biwengerassistant.playerreport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerLeagueApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerReport;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@Service
public class PlayerMatchReportService {

        private static final int CUSTOM_SCORE_ID = 100;

        private final BiwengerClient biwengerClient;
        private final PlayerMatchReportRepository playerMatchReportRepository;
        private final PlayerRepository playerRepository;
        private final CustomScoreEvaluator customScoreEvaluator;

        public PlayerMatchReportService(
                        BiwengerClient biwengerClient,
                        PlayerMatchReportRepository playerMatchReportRepository,
                        PlayerRepository playerRepository,
                        CustomScoreEvaluator customScoreEvaluator) {

                this.biwengerClient = biwengerClient;
                this.playerMatchReportRepository = playerMatchReportRepository;
                this.playerRepository = playerRepository;
                this.customScoreEvaluator = customScoreEvaluator;
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

                        String absenceStatus = participated
                                        || report.status() == null
                                                        ? null
                                                        : report.status().status();

                        LocalDateTime matchDate = report.match().date() == null
                                        ? null
                                        : LocalDateTime.ofInstant(
                                                        Instant.ofEpochSecond(
                                                                        report.match().date()),
                                                        ZoneId.systemDefault());

                        String season = resolveSeason(
                                        matchDate);

                        Long roundId = report.match().round() == null
                                        ? null
                                        : report.match().round().id();

                        String roundName = report.match().round() == null
                                        ? null
                                        : report.match().round().name();

                        String roundShort = report.match().round() == null
                                        ? null
                                        : report.match().round().shortName();

                        PlayerMatchReport entity = playerMatchReportRepository
                                        .findByPlayer_IdAndBiwengerMatchId(
                                                        player.getId(),
                                                        report.match().id())
                                        .orElse(null);

                        if (entity == null) {

                                entity = new PlayerMatchReport(
                                                player,
                                                report.match().id(),
                                                roundId,
                                                roundName,
                                                roundShort,
                                                matchDate,
                                                season,
                                                participated,
                                                absenceStatus,
                                                leaguePoints);

                        } else {

                                entity.update(
                                                roundId,
                                                roundName,
                                                roundShort,
                                                matchDate,
                                                season,
                                                participated,
                                                absenceStatus,
                                                leaguePoints);
                        }

                        playerMatchReportRepository.save(
                                        entity);

                        processed++;
                }

                return processed;
        }

        @Transactional
        public int syncLeagueReports(
                        Long leagueId) {

                /*
                 * La configuración se consulta UNA sola vez para toda
                 * la sincronización.
                 *
                 * Así todos los jugadores utilizan exactamente la misma
                 * versión del algoritmo y evitamos una petición de liga
                 * por jugador.
                 */
                LeagueScoreConfig scoreConfig = loadLeagueScoreConfig();

                List<Player> players = playerRepository.findAllByLeague_Id(
                                leagueId);

                int processed = 0;

                for (Player player : players) {

                        if (player.getSlug() == null
                                        || player.getSlug().isBlank()) {

                                continue;
                        }

                        processed += syncPlayerReports(
                                        player,
                                        scoreConfig);
                }

                return processed;
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

        private String resolveSeason(
                        LocalDateTime matchDate) {

                if (matchDate == null) {
                        return null;
                }

                int year = matchDate.getYear();
                int month = matchDate.getMonthValue();

                if (month >= 7) {
                        return year + "-" + (year + 1);
                }

                return (year - 1) + "-" + year;
        }

        private record LeagueScoreConfig(
                        Integer scoreId,
                        String customScore) {
        }
}