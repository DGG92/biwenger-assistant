package com.artajerjes.biwengerassistant.sync;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncStateServiceTest {

    private SyncStateRepository syncStateRepository;
    private SyncStateService service;

    @BeforeEach
    void setUp() {

        syncStateRepository = mock(SyncStateRepository.class);

        service = new SyncStateService(
                syncStateRepository,
                3600L);
    }

    @Test
    void registerRateLimitShouldUseRetryAfterWhenProvided() {

        when(syncStateRepository.findByLeagueIdAndSyncType(
                1L,
                SyncType.PLAYER_DETAILS))
                .thenReturn(Optional.empty());

        when(syncStateRepository.save(any(SyncState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();

        SyncState result = service.registerRateLimit(
                1L,
                SyncType.PLAYER_DETAILS,
                502L,
                120L);

        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result);

        assertSame(
                SyncType.PLAYER_DETAILS,
                result.getSyncType());

        assertTrue(
                result.getLastRateLimitAt()
                        .isAfter(before.minusSeconds(1)));

        assertTrue(
                result.getLastRateLimitAt()
                        .isBefore(after.plusSeconds(1)));

        assertTrue(
                result.getCooldownUntil()
                        .isAfter(before.plusSeconds(119)));

        assertTrue(
                result.getCooldownUntil()
                        .isBefore(after.plusSeconds(121)));

        assertEquals(
                120L,
                result.getRetryAfterSeconds());

        assertEquals(
                502L,
                result.getRateLimitedPlayerId());

        verify(syncStateRepository)
                .save(result);
    }

    @Test
    void registerRateLimitShouldUseFallbackWhenRetryAfterIsMissing() {

        when(syncStateRepository.findByLeagueIdAndSyncType(
                1L,
                SyncType.PLAYER_DETAILS))
                .thenReturn(Optional.empty());

        when(syncStateRepository.save(any(SyncState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();

        SyncState result = service.registerRateLimit(
                1L,
                SyncType.PLAYER_DETAILS,
                502L,
                null);

        LocalDateTime after = LocalDateTime.now();

        assertNull(
                result.getRetryAfterSeconds());

        assertTrue(
                result.getCooldownUntil()
                        .isAfter(before.plusSeconds(3599)));

        assertTrue(
                result.getCooldownUntil()
                        .isBefore(after.plusSeconds(3601)));
    }

    @Test
    void registerRateLimitShouldReuseExistingState() {

        SyncState existing = new SyncState(
                1L,
                SyncType.PLAYER_DETAILS);

        when(syncStateRepository.findByLeagueIdAndSyncType(
                1L,
                SyncType.PLAYER_DETAILS))
                .thenReturn(Optional.of(existing));

        when(syncStateRepository.save(existing))
                .thenReturn(existing);

        SyncState result = service.registerRateLimit(
                1L,
                SyncType.PLAYER_DETAILS,
                502L,
                300L);

        assertSame(
                existing,
                result);

        verify(syncStateRepository)
                .save(existing);
    }

    @Test
    void isInCooldownShouldReturnTrueForFutureCooldown() {

        SyncState state = new SyncState(
                1L,
                SyncType.PLAYER_DETAILS);

        state.registerRateLimit(
                LocalDateTime.now(),
                502L,
                120L,
                LocalDateTime.now()
                        .plusMinutes(10));

        when(syncStateRepository.findByLeagueIdAndSyncType(
                1L,
                SyncType.PLAYER_DETAILS))
                .thenReturn(Optional.of(state));

        assertTrue(
                service.isInCooldown(
                        1L,
                        SyncType.PLAYER_DETAILS));
    }

    @Test
    void isInCooldownShouldReturnFalseForExpiredCooldown() {

        SyncState state = new SyncState(
                1L,
                SyncType.PLAYER_DETAILS);

        state.registerRateLimit(
                LocalDateTime.now()
                        .minusHours(2),
                502L,
                null,
                LocalDateTime.now()
                        .minusHours(1));

        when(syncStateRepository.findByLeagueIdAndSyncType(
                1L,
                SyncType.PLAYER_DETAILS))
                .thenReturn(Optional.of(state));

        assertFalse(
                service.isInCooldown(
                        1L,
                        SyncType.PLAYER_DETAILS));
    }

    @Test
    void isInCooldownShouldReturnFalseWhenStateDoesNotExist() {

        when(syncStateRepository.findByLeagueIdAndSyncType(
                1L,
                SyncType.PLAYER_DETAILS))
                .thenReturn(Optional.empty());

        assertFalse(
                service.isInCooldown(
                        1L,
                        SyncType.PLAYER_DETAILS));
    }
}