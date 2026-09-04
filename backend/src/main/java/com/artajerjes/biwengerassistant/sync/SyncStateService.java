package com.artajerjes.biwengerassistant.sync;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncStateService {

    private final SyncStateRepository syncStateRepository;
    private final long playerDetailsRateLimitFallbackSeconds;

    public SyncStateService(
            SyncStateRepository syncStateRepository,
            @Value("${biwenger.player-detail-sync.rate-limit-fallback-seconds:3600}") long playerDetailsRateLimitFallbackSeconds) {

        this.syncStateRepository = syncStateRepository;
        this.playerDetailsRateLimitFallbackSeconds = playerDetailsRateLimitFallbackSeconds;
    }

    @Transactional(readOnly = true)
    public boolean isInCooldown(
            Long leagueId,
            SyncType syncType) {

        LocalDateTime now = LocalDateTime.now();

        return syncStateRepository
                .findByLeagueIdAndSyncType(
                        leagueId,
                        syncType)
                .map(SyncState::getCooldownUntil)
                .map(cooldownUntil -> cooldownUntil.isAfter(now))
                .orElse(false);
    }

    @Transactional
    public SyncState registerRateLimit(
            Long leagueId,
            SyncType syncType,
            Long rateLimitedPlayerId,
            Long retryAfterSeconds) {

        LocalDateTime detectedAt = LocalDateTime.now();

        long effectiveRetryAfterSeconds = retryAfterSeconds != null
                ? Math.max(retryAfterSeconds, 0L)
                : playerDetailsRateLimitFallbackSeconds;

        LocalDateTime cooldownUntil = detectedAt.plusSeconds(
                effectiveRetryAfterSeconds);

        SyncState syncState = syncStateRepository
                .findByLeagueIdAndSyncType(
                        leagueId,
                        syncType)
                .orElseGet(() -> new SyncState(
                        leagueId,
                        syncType));

        syncState.registerRateLimit(
                detectedAt,
                rateLimitedPlayerId,
                retryAfterSeconds,
                cooldownUntil);

        return syncStateRepository.save(
                syncState);
    }

    @Transactional(readOnly = true)
    public SyncState findState(
            Long leagueId,
            SyncType syncType) {

        return syncStateRepository
                .findByLeagueIdAndSyncType(
                        leagueId,
                        syncType)
                .orElse(null);
    }
}