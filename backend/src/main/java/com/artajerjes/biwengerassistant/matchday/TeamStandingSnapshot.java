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
@Table(name = "team_standing_snapshots", uniqueConstraints = @UniqueConstraint(name = "uk_team_standing_snapshot_league_round_team", columnNames = {
        "league_id",
        "biwenger_round_id",
        "biwenger_team_id"
}))
public class TeamStandingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(name = "biwenger_round_id", nullable = false)
    private Long biwengerRoundId;

    @Column(name = "biwenger_team_id", nullable = false)
    private Long biwengerTeamId;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(name = "position")
    private Integer position;

    @Column(name = "points")
    private Integer points;

    @Column(name = "won")
    private Integer won;

    @Column(name = "lost")
    private Integer lost;

    @Column(name = "tied")
    private Integer tied;

    @Column(name = "scored")
    private Integer scored;

    @Column(name = "against")
    private Integer against;

    protected TeamStandingSnapshot() {
    }

    public TeamStandingSnapshot(
            League league,
            Long biwengerRoundId,
            Long biwengerTeamId,
            String teamName,
            Integer position,
            Integer points,
            Integer won,
            Integer lost,
            Integer tied,
            Integer scored,
            Integer against) {

        this.league = league;
        this.biwengerRoundId = biwengerRoundId;
        this.biwengerTeamId = biwengerTeamId;
        this.teamName = teamName;
        this.position = position;
        this.points = points;
        this.won = won;
        this.lost = lost;
        this.tied = tied;
        this.scored = scored;
        this.against = against;
    }

    public void update(
            String teamName,
            Integer position,
            Integer points,
            Integer won,
            Integer lost,
            Integer tied,
            Integer scored,
            Integer against) {

        this.teamName = teamName;
        this.position = position;
        this.points = points;
        this.won = won;
        this.lost = lost;
        this.tied = tied;
        this.scored = scored;
        this.against = against;
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

    public Long getBiwengerTeamId() {
        return biwengerTeamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public Integer getPosition() {
        return position;
    }

    public Integer getPoints() {
        return points;
    }

    public Integer getWon() {
        return won;
    }

    public Integer getLost() {
        return lost;
    }

    public Integer getTied() {
        return tied;
    }

    public Integer getScored() {
        return scored;
    }

    public Integer getAgainst() {
        return against;
    }
}