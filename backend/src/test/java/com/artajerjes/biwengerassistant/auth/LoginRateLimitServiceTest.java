package com.artajerjes.biwengerassistant.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LoginRateLimitServiceTest {

    private static final String IP = "203.0.113.10";

    @Test
    void shouldBlockAfterFiveFailures() {

        LoginRateLimitService service = new LoginRateLimitService(
                Clock.fixed(
                        Instant.parse("2026-09-24T12:00:00Z"),
                        ZoneOffset.UTC));

        for (int i = 0; i < 4; i++) {
            service.registerFailure(IP);
        }

        assertThat(service.isBlocked(IP)).isFalse();

        service.registerFailure(IP);

        assertThat(service.isBlocked(IP)).isTrue();
        assertThat(service.retryAfterSeconds(IP))
                .isEqualTo(900);
    }

    @Test
    void shouldClearFailuresAfterSuccessfulLogin() {

        LoginRateLimitService service = new LoginRateLimitService(
                Clock.fixed(
                        Instant.parse("2026-09-24T12:00:00Z"),
                        ZoneOffset.UTC));

        for (int i = 0; i < 4; i++) {
            service.registerFailure(IP);
        }

        service.registerSuccess(IP);

        for (int i = 0; i < 4; i++) {
            service.registerFailure(IP);
        }

        assertThat(service.isBlocked(IP)).isFalse();
    }

    @Test
    void shouldTreatDifferentIpsIndependently() {

        LoginRateLimitService service = new LoginRateLimitService(
                Clock.fixed(
                        Instant.parse("2026-09-24T12:00:00Z"),
                        ZoneOffset.UTC));

        for (int i = 0; i < 5; i++) {
            service.registerFailure("203.0.113.10");
        }

        assertThat(service.isBlocked("203.0.113.10"))
                .isTrue();

        assertThat(service.isBlocked("203.0.113.11"))
                .isFalse();
    }
}