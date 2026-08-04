package com.artajerjes.biwengerassistant.player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.artajerjes.biwengerassistant.league.League;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "players",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_player_biwenger_id_league",
                        columnNames = {
                                "biwenger_player_id",
                                "league_id"
                        }
                )
        }
)
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
        name = "biwenger_player_id",
        nullable = false,
        length = 255
    )
    private String biwengerPlayerId;

    @Column(nullable = false, length = 100)
    private String name;

    @ElementCollection
    @CollectionTable(
            name = "player_positions",
            joinColumns = @JoinColumn(name = "player_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "position", nullable = false)
    @OrderColumn(name = "position_order")
    private List<PlayerPosition> positions = new ArrayList<>();

    @Column(nullable = false)
    private int points;

    @Column(name = "team_name", length = 100)
    private String teamName;

    @Column(name = "market_value", nullable = false)
    private Long marketValue;

    @Column(nullable = false)
    private boolean injured;

    @Column(nullable = false)
    private boolean captain;

    @Column(nullable = false)
    private boolean ram;

    @Column(name = "value_fluctuation", nullable = false)
    private Long valueFluctuation;

    @Column(name = "blocked_clause", nullable = false)
    private boolean blockedClause;

    @Column(name = "clause_value")
    private Long clauseValue;

    @Column(name = "owner_name", length = 100)
    private String ownerName;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Player() {
    }

    public Player(
            String biwengerPlayerId,
            String name,
            List<PlayerPosition> positions,
            String teamName,
            Long marketValue,
            League league
    ) {
        this.biwengerPlayerId = biwengerPlayerId;
        this.name = name;
        this.positions = new ArrayList<>(positions);
        this.teamName = teamName;
        this.marketValue = marketValue;
        this.league = league;

        this.points = 0;
        this.injured = false;
        this.captain = false;
        this.ram = false;
        this.valueFluctuation = 0L;
        this.blockedClause = false;
        this.clauseValue = null;
        this.ownerName = null;
        this.signedAt = null;
    }

    public void update(
            String biwengerPlayerId,
            String name,
            List<PlayerPosition> positions,
            Integer points,
            String teamName,
            Long marketValue,
            Boolean injured,
            Boolean captain,
            Boolean ram,
            Long valueFluctuation,
            Boolean blockedClause,
            Long clauseValue,
            String ownerName,
            LocalDateTime signedAt
    ) {
        this.biwengerPlayerId = biwengerPlayerId;
        this.name = name;
        this.positions = new ArrayList<>(positions);
        this.points = points;
        this.teamName = teamName;
        this.marketValue = marketValue;
        this.injured = injured;
        this.captain = captain;
        this.ram = ram;
        this.valueFluctuation = valueFluctuation;
        this.blockedClause = blockedClause;
        this.clauseValue = clauseValue;
        this.ownerName = ownerName;
        this.signedAt = signedAt;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getBiwengerPlayerId() {
        return biwengerPlayerId;
    }

    public String getName() {
        return name;
    }

    public List<PlayerPosition> getPositions() {
        return List.copyOf(positions);
    }

    public int getPoints() {
        return points;
    }

    public String getTeamName() {
        return teamName;
    }

    public Long getMarketValue() {
        return marketValue;
    }

    public boolean isInjured() {
        return injured;
    }

    public boolean isCaptain() {
        return captain;
    }

    public boolean isRam() {
        return ram;
    }

    public Long getValueFluctuation() {
        return valueFluctuation;
    }

    public boolean isBlockedClause() {
        return blockedClause;
    }

    public Long getClauseValue() {
        return clauseValue;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public boolean isFreePlayer() {
        return ownerName == null;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public League getLeague() {
        return league;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}