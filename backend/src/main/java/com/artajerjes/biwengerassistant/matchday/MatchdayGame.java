package com.artajerjes.biwengerassistant.matchday;

import com.artajerjes.biwengerassistant.league.League;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "matchday_games", uniqueConstraints = {
        @UniqueConstraint(name = "uk_matchday_game_league_game", columnNames = {
                "league_id",
                "biwenger_game_id"
        })
})
public class MatchdayGame {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(name = "biwenger_round_id", nullable = false)
    private Long biwengerRoundId;

    @Column(name = "biwenger_game_id", nullable = false)
    private Long biwengerGameId;

    @Column(name = "round_part")
    private Integer roundPart;

    @Column(name = "game_date")
    private Long gameDate;

    @Column(name = "status")
    private String status;

    @Column(name = "home_team_id")
    private Long homeTeamId;

    @Column(name = "home_team_name")
    private String homeTeamName;

    @Column(name = "away_team_id")
    private Long awayTeamId;

    @Column(name = "away_team_name")
    private String awayTeamName;

    protected MatchdayGame() {
    }

    public MatchdayGame(
            League league,
            Long biwengerRoundId,
            Long biwengerGameId,
            Integer roundPart,
            Long gameDate,
            String status,
            Long homeTeamId,
            String homeTeamName,
            Long awayTeamId,
            String awayTeamName) {

        this.league = league;
        this.biwengerRoundId = biwengerRoundId;
        this.biwengerGameId = biwengerGameId;
        this.roundPart = roundPart;
        this.gameDate = gameDate;
        this.status = status;
        this.homeTeamId = homeTeamId;
        this.homeTeamName = homeTeamName;
        this.awayTeamId = awayTeamId;
        this.awayTeamName = awayTeamName;
    }

    public void update(
            Long biwengerRoundId,
            Integer roundPart,
            Long gameDate,
            String status,
            Long homeTeamId,
            String homeTeamName,
            Long awayTeamId,
            String awayTeamName) {

        this.biwengerRoundId = biwengerRoundId;
        this.roundPart = roundPart;
        this.gameDate = gameDate;
        this.status = status;
        this.homeTeamId = homeTeamId;
        this.homeTeamName = homeTeamName;
        this.awayTeamId = awayTeamId;
        this.awayTeamName = awayTeamName;
    }

    public Long getId() {
        return id;
    }

    public League getLeague() {
        return league;
    }

    public Long getBiwengerRoundId() {
        return biwengerRoundId;
    }

    public Long getBiwengerGameId() {
        return biwengerGameId;
    }

    public Integer getRoundPart() {
        return roundPart;
    }

    public Long getGameDate() {
        return gameDate;
    }

    public String getStatus() {
        return status;
    }

    public Long getHomeTeamId() {
        return homeTeamId;
    }

    public String getHomeTeamName() {
        return homeTeamName;
    }

    public Long getAwayTeamId() {
        return awayTeamId;
    }

    public String getAwayTeamName() {
        return awayTeamName;
    }
}