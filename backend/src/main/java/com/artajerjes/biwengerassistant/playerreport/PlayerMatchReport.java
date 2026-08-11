package com.artajerjes.biwengerassistant.playerreport;

import java.time.LocalDateTime;

import com.artajerjes.biwengerassistant.player.Player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "player_match_reports", uniqueConstraints = @UniqueConstraint(name = "uk_player_match_report_player_match", columnNames = {
        "player_id",
        "biwenger_match_id"
}))
public class PlayerMatchReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Column(name = "biwenger_match_id", nullable = false)
    private Long biwengerMatchId;

    @Column(name = "biwenger_round_id")
    private Long biwengerRoundId;

    @Column(name = "round_name", length = 100)
    private String roundName;

    @Column(name = "round_short", length = 20)
    private String roundShort;

    @Column(name = "match_date")
    private LocalDateTime matchDate;

    @Column(name = "season", length = 20)
    private String season;

    @Column(name = "participated", nullable = false)
    private boolean participated;

    @Column(name = "absence_status", length = 50)
    private String absenceStatus;

    @Column
    private Integer points;

    protected PlayerMatchReport() {
    }

    public PlayerMatchReport(
            Player player,
            Long biwengerMatchId,
            Long biwengerRoundId,
            String roundName,
            String roundShort,
            LocalDateTime matchDate,
            String season,
            boolean participated,
            String absenceStatus,
            Integer points) {

        this.player = player;
        this.biwengerMatchId = biwengerMatchId;
        this.biwengerRoundId = biwengerRoundId;
        this.roundName = roundName;
        this.roundShort = roundShort;
        this.matchDate = matchDate;
        this.season = season;
        this.participated = participated;
        this.absenceStatus = absenceStatus;
        this.points = points;
    }

    public Long getId() {
        return id;
    }

    public Player getPlayer() {
        return player;
    }

    public Long getBiwengerMatchId() {
        return biwengerMatchId;
    }

    public Long getBiwengerRoundId() {
        return biwengerRoundId;
    }

    public String getRoundName() {
        return roundName;
    }

    public String getRoundShort() {
        return roundShort;
    }

    public LocalDateTime getMatchDate() {
        return matchDate;
    }

    public String getSeason() {
        return season;
    }

    public boolean isParticipated() {
        return participated;
    }

    public String getAbsenceStatus() {
        return absenceStatus;
    }

    public Integer getPoints() {
        return points;
    }

    public void update(
            Long biwengerRoundId,
            String roundName,
            String roundShort,
            LocalDateTime matchDate,
            String season,
            boolean participated,
            String absenceStatus,
            Integer points) {

        this.biwengerRoundId = biwengerRoundId;
        this.roundName = roundName;
        this.roundShort = roundShort;
        this.matchDate = matchDate;
        this.season = season;
        this.participated = participated;
        this.absenceStatus = absenceStatus;
        this.points = points;
    }
}