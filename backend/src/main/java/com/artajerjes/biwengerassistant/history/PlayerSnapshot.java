package com.artajerjes.biwengerassistant.history;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "player_snapshots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_snapshot_player_date", columnNames = {
                "player_id",
                "snapshot_date"
        })
})
public class PlayerSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    @Column(nullable = false)
    private int points;

    @Column(name = "market_value", nullable = false)
    private Long marketValue;

    @Column(name = "value_fluctuation", nullable = false)
    private Long valueFluctuation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlayerStatus status;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "purchase_price")
    private Long purchasePrice;

    protected PlayerSnapshot() {
    }

    public PlayerSnapshot(
            Player player,
            LocalDate snapshotDate,
            LocalDateTime capturedAt) {

        this.playerId = player.getId();
        this.leagueId = player.getLeague().getId();
        this.snapshotDate = snapshotDate;

        updateFrom(
                player,
                capturedAt);
    }

    public void updateFrom(
            Player player,
            LocalDateTime capturedAt) {

        this.capturedAt = capturedAt;
        this.points = player.getPoints();
        this.marketValue = player.getMarketValue();
        this.valueFluctuation = player.getValueFluctuation();
        this.status = player.getStatus();
        this.teamId = player.getTeamId();

        this.ownerId = player.getOwner() == null
                ? null
                : player.getOwner().getId();

        this.purchasePrice = player.getPurchasePrice();
    }

    public Long getId() {
        return id;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public Long getLeagueId() {
        return leagueId;
    }

    public LocalDate getSnapshotDate() {
        return snapshotDate;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }

    public int getPoints() {
        return points;
    }

    public Long getMarketValue() {
        return marketValue;
    }

    public Long getValueFluctuation() {
        return valueFluctuation;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public Long getTeamId() {
        return teamId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public Long getPurchasePrice() {
        return purchasePrice;
    }
}