package com.artajerjes.biwengerassistant.offer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.player.Player;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "offers")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "biwenger_offer_id", nullable = false, unique = true)
    private Long biwengerOfferId;

    @Column(nullable = false)
    private Long amount;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String type;

    @ManyToOne
    @JoinColumn(name = "from_manager_id")
    private Manager fromManager;

    @ManyToOne
    @JoinColumn(name = "to_manager_id")
    private Manager toManager;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "offer_requested_players", joinColumns = @JoinColumn(name = "offer_id"), inverseJoinColumns = @JoinColumn(name = "player_id"))
    private List<Player> requestedPlayers = new ArrayList<>();

    @ManyToOne(optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    protected Offer() {
    }

    public Offer(
            Long biwengerOfferId,
            Long amount,
            String status,
            String type,
            Manager fromManager,
            Manager toManager,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            List<Player> requestedPlayers,
            League league) {
        this.biwengerOfferId = biwengerOfferId;
        this.amount = amount;
        this.status = status;
        this.type = type;
        this.fromManager = fromManager;
        this.toManager = toManager;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.requestedPlayers = new ArrayList<>(requestedPlayers);
        this.league = league;
    }

    public void update(
            Long amount,
            String status,
            String type,
            Manager fromManager,
            Manager toManager,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            List<Player> requestedPlayers) {
        this.amount = amount;
        this.status = status;
        this.type = type;
        this.fromManager = fromManager;
        this.toManager = toManager;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.requestedPlayers.clear();
        this.requestedPlayers.addAll(requestedPlayers);
    }

    public Long getId() {
        return id;
    }

    public Long getBiwengerOfferId() {
        return biwengerOfferId;
    }

    public Long getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getType() {
        return type;
    }

    public Manager getFromManager() {
        return fromManager;
    }

    public Manager getToManager() {
        return toManager;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public List<Player> getRequestedPlayers() {
        return requestedPlayers;
    }

    public League getLeague() {
        return league;
    }
}