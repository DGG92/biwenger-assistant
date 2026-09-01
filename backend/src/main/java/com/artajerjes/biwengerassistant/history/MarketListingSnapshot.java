package com.artajerjes.biwengerassistant.history;

import java.time.LocalDateTime;

import com.artajerjes.biwengerassistant.market.MarketListing;
import com.artajerjes.biwengerassistant.market.MarketListingType;

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
@Table(name = "market_listing_snapshots", uniqueConstraints = {
        @UniqueConstraint(name = "uk_market_listing_snapshot_appearance", columnNames = {
                "league_id",
                "player_id",
                "type",
                "published_at"
        })
})
public class MarketListingSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private MarketListingType type;

    @Column(name = "seller_id")
    private Long sellerId;

    @Column(name = "asking_price", nullable = false)
    private Long askingPrice;

    @Column(name = "player_market_value", nullable = false)
    private Long playerMarketValue;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean extended;

    @Column(name = "last_bid_amount")
    private Long lastBidAmount;

    @Column(name = "last_bid_status")
    private String lastBidStatus;

    @Column(name = "last_bid_manager_id")
    private Long lastBidManagerId;

    @Column(name = "first_captured_at", nullable = false)
    private LocalDateTime firstCapturedAt;

    @Column(name = "last_captured_at", nullable = false)
    private LocalDateTime lastCapturedAt;

    protected MarketListingSnapshot() {
    }

    public MarketListingSnapshot(
            MarketListing listing,
            LocalDateTime capturedAt) {

        this.leagueId = listing.getLeague().getId();
        this.playerId = listing.getPlayer().getId();
        this.type = listing.getType();
        this.publishedAt = listing.getPublishedAt();
        this.firstCapturedAt = capturedAt;
        this.playerMarketValue = listing.getPlayer()
                .getMarketValue();

        updateFrom(
                listing,
                capturedAt);
    }

    public void updateFrom(
            MarketListing listing,
            LocalDateTime capturedAt) {

        this.sellerId = listing.getSeller() == null
                ? null
                : listing.getSeller().getId();

        this.askingPrice = listing.getPrice();

        this.expiresAt = listing.getExpiresAt();
        this.extended = listing.isExtended();
        this.lastBidAmount = listing.getLastBidAmount();
        this.lastBidStatus = listing.getLastBidStatus();

        this.lastBidManagerId = listing.getLastBidManager() == null
                ? null
                : listing.getLastBidManager().getId();

        this.lastCapturedAt = capturedAt;
    }

    public Long getId() {
        return id;
    }

    public Long getLeagueId() {
        return leagueId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public MarketListingType getType() {
        return type;
    }

    public Long getSellerId() {
        return sellerId;
    }

    public Long getAskingPrice() {
        return askingPrice;
    }

    public Long getPlayerMarketValue() {
        return playerMarketValue;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isExtended() {
        return extended;
    }

    public Long getLastBidAmount() {
        return lastBidAmount;
    }

    public String getLastBidStatus() {
        return lastBidStatus;
    }

    public Long getLastBidManagerId() {
        return lastBidManagerId;
    }

    public LocalDateTime getFirstCapturedAt() {
        return firstCapturedAt;
    }

    public LocalDateTime getLastCapturedAt() {
        return lastCapturedAt;
    }
}