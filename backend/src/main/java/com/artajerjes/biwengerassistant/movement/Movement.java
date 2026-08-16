package com.artajerjes.biwengerassistant.movement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.player.Player;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "movements")
public class Movement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MovementType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @ManyToOne
    @JoinColumn(name = "from_manager_id")
    private Manager fromManager;

    @ManyToOne
    @JoinColumn(name = "to_manager_id")
    private Manager toManager;

    @Column(nullable = false)
    private Long amount;

    @Column(name = "rounds")
    private Integer rounds;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    @ManyToOne(optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @OneToMany(mappedBy = "movement", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<MovementBid> bids = new ArrayList<>();

    @Column(name = "external_key", nullable = false, unique = true)
    private String externalKey;

    protected Movement() {
    }

    public Movement(
            String externalKey,
            MovementType type,
            Player player,
            Manager fromManager,
            Manager toManager,
            Long amount,
            Integer rounds,
            LocalDateTime occurredAt,
            League league) {
        this.externalKey = externalKey;
        this.type = type;
        this.player = player;
        this.fromManager = fromManager;
        this.toManager = toManager;
        this.amount = amount;
        this.rounds = rounds;
        this.occurredAt = occurredAt;
        this.league = league;
    }

    public void addBid(MovementBid bid) {
        bids.add(bid);
        bid.setMovement(this);
    }

    public Long getId() {
        return id;
    }

    public MovementType getType() {
        return type;
    }

    public Player getPlayer() {
        return player;
    }

    public Manager getFromManager() {
        return fromManager;
    }

    public Manager getToManager() {
        return toManager;
    }

    public Long getAmount() {
        return amount;
    }

    public Integer getRounds() {
        return rounds;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public League getLeague() {
        return league;
    }

    public List<MovementBid> getBids() {
        return bids;
    }

    public String getExternalKey() {
        return externalKey;
    }

}