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
@Table(name = "matchday_contexts", uniqueConstraints = @UniqueConstraint(name = "uk_matchday_context_league_round", columnNames = {
        "league_id",
        "biwenger_round_id"
}))
public class MatchdayContext {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(name = "biwenger_round_id", nullable = false)
    private Long biwengerRoundId;

    @Column(name = "split_round")
    private String splitRound;

    @Column(name = "lineup_show")
    private String lineupShow;

    @Column(name = "lineup_round_changes")
    private Integer lineupRoundChanges;

    @Column(name = "lineup_round_changes_in")
    private String lineupRoundChangesIn;

    @Column(name = "lineup_round_change_strategy")
    private Boolean lineupRoundChangeStrategy;

    protected MatchdayContext() {
    }

    public MatchdayContext(
            League league,
            Long biwengerRoundId,
            String splitRound,
            String lineupShow,
            Integer lineupRoundChanges,
            String lineupRoundChangesIn,
            Boolean lineupRoundChangeStrategy) {

        this.league = league;
        this.biwengerRoundId = biwengerRoundId;
        this.splitRound = splitRound;
        this.lineupShow = lineupShow;
        this.lineupRoundChanges = lineupRoundChanges;
        this.lineupRoundChangesIn = lineupRoundChangesIn;
        this.lineupRoundChangeStrategy = lineupRoundChangeStrategy;
    }

    public void update(
            String splitRound,
            String lineupShow,
            Integer lineupRoundChanges,
            String lineupRoundChangesIn,
            Boolean lineupRoundChangeStrategy) {

        this.splitRound = splitRound;
        this.lineupShow = lineupShow;
        this.lineupRoundChanges = lineupRoundChanges;
        this.lineupRoundChangesIn = lineupRoundChangesIn;
        this.lineupRoundChangeStrategy = lineupRoundChangeStrategy;
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

    public String getSplitRound() {
        return splitRound;
    }

    public String getLineupShow() {
        return lineupShow;
    }

    public Integer getLineupRoundChanges() {
        return lineupRoundChanges;
    }

    public String getLineupRoundChangesIn() {
        return lineupRoundChangesIn;
    }

    public Boolean getLineupRoundChangeStrategy() {
        return lineupRoundChangeStrategy;
    }
}