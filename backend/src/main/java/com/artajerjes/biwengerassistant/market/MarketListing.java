package com.artajerjes.biwengerassistant.market;

import java.time.LocalDateTime;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.player.Player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "market_listings")
public class MarketListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MarketListingType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private Manager seller;

    @Column(nullable = false)
    private Long price;

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

    @ManyToOne
    @JoinColumn(name = "last_bid_manager_id")
    private Manager lastBidManager;

    @ManyToOne(optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    protected MarketListing() {
    }

    public MarketListing(
            MarketListingType type,
            Player player,
            Manager seller,
            Long price,
            LocalDateTime publishedAt,
            LocalDateTime expiresAt,
            boolean extended,
            Long lastBidAmount,
            String lastBidStatus,
            Manager lastBidManager,
            League league) {
        this.type = type;
        this.player = player;
        this.seller = seller;
        this.price = price;
        this.publishedAt = publishedAt;
        this.expiresAt = expiresAt;
        this.extended = extended;
        this.lastBidAmount = lastBidAmount;
        this.lastBidStatus = lastBidStatus;
        this.lastBidManager = lastBidManager;
        this.league = league;
    }

    public Long getId() {
        return id;
    }

    public MarketListingType getType() {
        return type;
    }

    public Player getPlayer() {
        return player;
    }

    public Manager getSeller() {
        return seller;
    }

    public Long getPrice() {
        return price;
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

    public Manager getLastBidManager() {
        return lastBidManager;
    }

    public League getLeague() {
        return league;
    }
}