package com.artajerjes.biwengerassistant.playerreport;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerReport;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@Service
public class PlayerMatchReportService {

        private final BiwengerClient biwengerClient;
        private final PlayerMatchReportRepository playerMatchReportRepository;
        private final PlayerRepository playerRepository;

        public PlayerMatchReportService(
                        BiwengerClient biwengerClient,
                        PlayerMatchReportRepository playerMatchReportRepository,
                        PlayerRepository playerRepository) {
                this.biwengerClient = biwengerClient;
                this.playerMatchReportRepository = playerMatchReportRepository;
                this.playerRepository = playerRepository;
        }

        @Transactional
        public int syncPlayerReports(Player player) {
                if (player.getSlug() == null || player.getSlug().isBlank()) {
                        return 0;
                }

                BiwengerPlayerDetailResponse response = biwengerClient.getPlayerDetail(player.getSlug());

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
                                        || !"finished".equalsIgnoreCase(report.match().status())) {
                                continue;
                        }

                        boolean participated = report.points() != null;

                        Integer leaguePoints = null;

                        if (participated) {
                                Integer score2 = report.points().get("2");
                                Integer score3 = report.points().get("3");

                                if (score2 == null || score3 == null) {
                                        continue;
                                }

                                leaguePoints = calculateLeaguePoints(
                                                score2,
                                                score3);
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

                        String season = resolveSeason(matchDate);

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

                        playerMatchReportRepository.save(entity);
                        processed++;
                }

                return processed;
        }

        @Transactional
        public int syncLeagueReports(Long leagueId) {
                List<Player> players = playerRepository.findAllByLeague_Id(leagueId);

                int processed = 0;

                for (Player player : players) {
                        if (player.getSlug() == null
                                        || player.getSlug().isBlank()) {
                                continue;
                        }

                        processed += syncPlayerReports(player);
                }

                return processed;
        }

        int calculateLeaguePoints(
                        Integer score2,
                        Integer score3) {

                BigDecimal average = BigDecimal
                                .valueOf(score2)
                                .add(BigDecimal.valueOf(score3))
                                .divide(
                                                BigDecimal.valueOf(2),
                                                1,
                                                RoundingMode.HALF_UP);

                return average
                                .setScale(
                                                0,
                                                RoundingMode.HALF_UP)
                                .intValue();
        }

        private String resolveSeason(LocalDateTime matchDate) {
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
}