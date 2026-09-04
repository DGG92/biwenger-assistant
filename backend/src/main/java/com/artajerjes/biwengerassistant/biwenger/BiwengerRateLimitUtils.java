package com.artajerjes.biwengerassistant.biwenger;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.web.client.HttpClientErrorException;

public final class BiwengerRateLimitUtils {

    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private BiwengerRateLimitUtils() {
    }

    public static Long extractRetryAfterSeconds(
            HttpClientErrorException.TooManyRequests exception) {

        String retryAfter = exception
                .getResponseHeaders()
                .getFirst(RETRY_AFTER_HEADER);

        if (retryAfter == null || retryAfter.isBlank()) {
            return null;
        }

        try {
            return Long.parseLong(retryAfter.trim());
        } catch (NumberFormatException ignored) {
            // Puede venir como fecha HTTP en lugar de segundos.
        }

        try {
            ZonedDateTime retryAt = ZonedDateTime.parse(
                    retryAfter,
                    DateTimeFormatter.RFC_1123_DATE_TIME);

            long seconds = Duration.between(
                    ZonedDateTime.now(retryAt.getZone()),
                    retryAt)
                    .getSeconds();

            return Math.max(seconds, 0L);

        } catch (Exception ignored) {
            return null;
        }
    }
}