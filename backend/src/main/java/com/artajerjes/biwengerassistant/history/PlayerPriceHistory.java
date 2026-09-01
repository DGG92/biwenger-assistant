package com.artajerjes.biwengerassistant.history;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
@Table(name = "player_price_history", uniqueConstraints = {
        @UniqueConstraint(name = "uk_player_price_history_player_date", columnNames = {
                "player_id",
                "price_date"
        })
})
public class PlayerPriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "market_value", nullable = false)
    private Long marketValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PlayerPriceSource source;

    @Column(name = "captured_at", nullable = false)
    private LocalDateTime capturedAt;

    protected PlayerPriceHistory() {
    }

    public PlayerPriceHistory(
            Long playerId,
            Long leagueId,
            LocalDate priceDate,
            Long marketValue,
            PlayerPriceSource source,
            LocalDateTime capturedAt) {

        this.playerId = playerId;
        this.leagueId = leagueId;
        this.priceDate = priceDate;

        update(
                marketValue,
                source,
                capturedAt);
    }

    public void update(
            Long marketValue,
            PlayerPriceSource source,
            LocalDateTime capturedAt) {

        this.marketValue = marketValue;
        this.source = source;
        this.capturedAt = capturedAt;
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

    public LocalDate getPriceDate() {
        return priceDate;
    }

    public Long getMarketValue() {
        return marketValue;
    }

    public PlayerPriceSource getSource() {
        return source;
    }

    public LocalDateTime getCapturedAt() {
        return capturedAt;
    }
}