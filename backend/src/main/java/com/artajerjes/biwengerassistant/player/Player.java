package com.artajerjes.biwengerassistant.player;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;

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
@Table(name = "players", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_biwenger_id_league", columnNames = {
                "biwenger_player_id",
                "league_id"
        })
})
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "biwenger_player_id", nullable = false, length = 255)
    private String biwengerPlayerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 150)
    private String slug;

    @ElementCollection
    @CollectionTable(name = "player_positions", joinColumns = @JoinColumn(name = "player_id"))
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PlayerStatus status;

    @Column(nullable = false)
    private boolean captain;

    @Column(nullable = false)
    private boolean ram;

    @Column(nullable = false)
    private boolean coach;

    @Column(nullable = false)
    private boolean starter;

    @Column(nullable = false)
    private boolean reserve;

    @Enumerated(EnumType.STRING)
    @Column(name = "lineup_position")
    private PlayerPosition lineupPosition;

    @Enumerated(EnumType.STRING)
    @Column(name = "bench_position")
    private PlayerPosition benchPosition;

    @Column(name = "value_fluctuation", nullable = false)
    private Long valueFluctuation;

    @Column(name = "clause_value")
    private Long clauseValue;

    @Column(name = "clause_locked_until")
    private LocalDateTime clauseLockedUntil;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Manager owner;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "purchase_price")
    private Long purchasePrice;

    protected Player() {
    }

    public Player(
            String biwengerPlayerId,
            String name,
            List<PlayerPosition> positions,
            String teamName,
            Long marketValue,
            League league) {
        this.biwengerPlayerId = biwengerPlayerId;
        this.name = name;
        this.positions = new ArrayList<>(positions);
        this.teamName = teamName;
        this.marketValue = marketValue;
        this.league = league;

        this.points = 0;
        this.injured = false;
        this.status = PlayerStatus.OK;
        this.captain = false;
        this.ram = false;
        this.coach = false;
        this.starter = false;
        this.reserve = false;
        this.lineupPosition = null;
        this.benchPosition = null;
        this.valueFluctuation = 0L;
        this.clauseValue = null;
        this.owner = null;
        this.signedAt = null;
        this.clauseLockedUntil = null;
        this.createdAt = null;
        this.purchasePrice = null;
    }

    public void update(
            String biwengerPlayerId,
            String name,
            List<PlayerPosition> positions,
            Integer points,
            String teamName,
            Long marketValue,
            PlayerStatus status,
            Boolean captain,
            Boolean ram,
            Long valueFluctuation,
            LocalDateTime clauseLockedUntil,
            Long clauseValue,
            Manager owner,
            LocalDateTime signedAt) {
        this.biwengerPlayerId = biwengerPlayerId;
        this.name = name;
        this.positions = new ArrayList<>(positions);
        this.points = points;
        this.teamName = teamName;
        this.marketValue = marketValue;
        this.status = status;
        this.injured = status == PlayerStatus.INJURED;
        this.captain = captain;
        this.ram = ram;
        this.valueFluctuation = valueFluctuation;
        this.clauseLockedUntil = clauseLockedUntil;
        this.clauseValue = clauseValue;
        this.owner = owner;
        this.signedAt = signedAt;
    }

    public void updateCompetitionData(
            String name,
            String slug,
            List<PlayerPosition> positions,
            Integer points,
            String teamName,
            Long marketValue,
            PlayerStatus status,
            Long valueFluctuation) {

        this.name = name;
        this.slug = slug;
        this.positions = new ArrayList<>(positions);
        this.points = points;
        this.teamName = teamName;
        this.marketValue = marketValue;
        this.status = status;
        this.injured = status == PlayerStatus.INJURED;
        this.valueFluctuation = valueFluctuation;
    }

    public void updateOwnership(
            Manager owner,
            LocalDateTime signedAt,
            Long purchasePrice,
            Long clauseValue,
            LocalDateTime clauseLockedUntil) {
        this.owner = owner;
        this.signedAt = signedAt;
        this.purchasePrice = purchasePrice;
        this.clauseValue = clauseValue;
        this.clauseLockedUntil = clauseLockedUntil;
    }

    public void clearOwnership() {
        this.owner = null;
        this.signedAt = null;
        this.purchasePrice = null;
        this.clauseValue = null;
        this.clauseLockedUntil = null;
    }

    public void updateLineupRoles(
            boolean captain,
            boolean ram,
            boolean coach,
            boolean starter,
            boolean reserve,
            PlayerPosition lineupPosition,
            PlayerPosition benchPosition) {
        this.captain = captain;
        this.ram = ram;
        this.coach = coach;
        this.starter = starter;
        this.reserve = reserve;
        this.lineupPosition = lineupPosition;
        this.benchPosition = benchPosition;
    }

    public void clearLineupRoles() {
        this.captain = false;
        this.ram = false;
        this.starter = false;
        this.reserve = false;
        this.lineupPosition = null;
        this.benchPosition = null;
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

    public String getSlug() {
        return slug;
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

    public PlayerStatus getStatus() {
        return status;
    }

    public boolean isCaptain() {
        return captain;
    }

    public boolean isRam() {
        return ram;
    }

    public boolean isCoach() {
        return coach;
    }

    public boolean isStarter() {
        return starter;
    }

    public boolean isReserve() {
        return reserve;
    }

    public PlayerPosition getLineupPosition() {
        return lineupPosition;
    }

    public PlayerPosition getBenchPosition() {
        return benchPosition;
    }

    public Long getValueFluctuation() {
        return valueFluctuation;
    }

    public LocalDateTime getClauseLockedUntil() {
        return clauseLockedUntil;
    }

    public Long getClauseValue() {
        return clauseValue;
    }

    public boolean isBlockedClause() {
        return clauseLockedUntil != null
                && clauseLockedUntil.isAfter(LocalDateTime.now());
    }

    public Manager getOwner() {
        return owner;
    }

    public boolean isFreePlayer() {
        return owner == null;
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

    public Long getPurchasePrice() {
        return purchasePrice;
    }

    public Long getProfitability() {
        if (purchasePrice == null || marketValue == null) {
            return null;
        }

        return marketValue - purchasePrice;
    }
}