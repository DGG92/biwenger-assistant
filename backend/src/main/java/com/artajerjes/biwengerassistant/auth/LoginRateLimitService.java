package com.artajerjes.biwengerassistant.auth;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LoginRateLimitService {

    static final int MAX_FAILED_ATTEMPTS = 5;
    static final Duration ATTEMPT_WINDOW = Duration.ofMinutes(5);
    static final Duration BLOCK_DURATION = Duration.ofMinutes(15);

    private final Map<String, AttemptState> attempts = new ConcurrentHashMap<>();

    private final Clock clock;

    public LoginRateLimitService() {
        this(Clock.systemUTC());
    }

    LoginRateLimitService(Clock clock) {
        this.clock = clock;
    }

    public boolean isBlocked(String clientIp) {
        String key = normalize(clientIp);
        AttemptState state = attempts.get(key);

        if (state == null) {
            return false;
        }

        Instant now = clock.instant();

        synchronized (state) {
            if (state.blockedUntil != null) {
                if (now.isBefore(state.blockedUntil)) {
                    return true;
                }

                attempts.remove(key, state);
                return false;
            }

            if (isWindowExpired(state, now)) {
                attempts.remove(key, state);
            }

            return false;
        }
    }

    public void registerFailure(String clientIp) {
        String key = normalize(clientIp);
        Instant now = clock.instant();

        attempts.compute(key, (ignored, current) -> {
            AttemptState state = current;

            if (state == null
                    || state.blockedUntil != null
                    || isWindowExpired(state, now)) {

                state = new AttemptState(now);
            }

            state.failedAttempts++;

            if (state.failedAttempts >= MAX_FAILED_ATTEMPTS) {
                state.blockedUntil = now.plus(BLOCK_DURATION);
            }

            return state;
        });
    }

    public void registerSuccess(String clientIp) {
        attempts.remove(normalize(clientIp));
    }

    public long retryAfterSeconds(String clientIp) {
        AttemptState state = attempts.get(normalize(clientIp));

        if (state == null || state.blockedUntil == null) {
            return 0;
        }

        long seconds = Duration.between(
                clock.instant(),
                state.blockedUntil)
                .getSeconds();

        return Math.max(0, seconds);
    }

    private boolean isWindowExpired(
            AttemptState state,
            Instant now) {

        return !now.isBefore(
                state.windowStartedAt.plus(ATTEMPT_WINDOW));
    }

    private String normalize(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return "unknown";
        }

        return clientIp.trim();
    }

    private static final class AttemptState {

        private final Instant windowStartedAt;
        private int failedAttempts;
        private Instant blockedUntil;

        private AttemptState(Instant windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }
    }
}