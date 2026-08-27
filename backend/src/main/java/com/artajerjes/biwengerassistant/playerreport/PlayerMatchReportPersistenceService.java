package com.artajerjes.biwengerassistant.playerreport;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerReport;
import com.artajerjes.biwengerassistant.player.Player;

@Service
public class PlayerMatchReportPersistenceService {

    private final PlayerMatchReportRepository playerMatchReportRepository;

    public PlayerMatchReportPersistenceService(
            PlayerMatchReportRepository playerMatchReportRepository) {

        this.playerMatchReportRepository = playerMatchReportRepository;
    }

    @Transactional
    public int persistReport(
            Player player,
            BiwengerPlayerReport report,
            Integer leaguePoints) {

        if (player == null
                || report == null
                || report.match() == null
                || report.match().id() == null) {

            return 0;
        }

        boolean participated = report.points() != null
                || report.rawStats() != null;

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
                .findByPlayerIdAndBiwengerMatchId(
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

        return 1;
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
}