package com.artajerjes.biwengerassistant.sync;

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
@Table(name = "sync_states", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sync_states_league_type", columnNames = {
                "league_id",
                "sync_type"
        })
})
public class SyncState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sync_type", nullable = false, length = 50)
    private SyncType syncType;

    @Column(name = "last_rate_limit_at")
    private LocalDateTime lastRateLimitAt;

    @Column(name = "rate_limited_player_id")
    private Long rateLimitedPlayerId;

    @Column(name = "retry_after_seconds")
    private Long retryAfterSeconds;

    @Column(name = "cooldown_until")
    private LocalDateTime cooldownUntil;

    protected SyncState() {
    }

    public SyncState(
            Long leagueId,
            SyncType syncType) {

        this.leagueId = leagueId;
        this.syncType = syncType;
    }

    public void registerRateLimit(
            LocalDateTime detectedAt,
            Long rateLimitedPlayerId,
            Long retryAfterSeconds,
            LocalDateTime cooldownUntil) {

        this.lastRateLimitAt = detectedAt;
        this.rateLimitedPlayerId = rateLimitedPlayerId;
        this.retryAfterSeconds = retryAfterSeconds;
        this.cooldownUntil = cooldownUntil;
    }

    public Long getId() {
        return id;
    }

    public Long getLeagueId() {
        return leagueId;
    }

    public SyncType getSyncType() {
        return syncType;
    }

    public LocalDateTime getLastRateLimitAt() {
        return lastRateLimitAt;
    }

    public Long getRateLimitedPlayerId() {
        return rateLimitedPlayerId;
    }

    public Long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public LocalDateTime getCooldownUntil() {
        return cooldownUntil;
    }
}