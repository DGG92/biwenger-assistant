package com.artajerjes.biwengerassistant.playerreport;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PlayerMatchReportServiceTest {

    private final PlayerMatchReportService service = new PlayerMatchReportService(null, null, null);

    @Test
    void calculateLeaguePointsShouldRoundPositiveHalfAwayFromZero() {
        assertEquals(
                7,
                service.calculateLeaguePoints(7, 6));
    }

    @Test
    void calculateLeaguePointsShouldRoundNegativeHalfAwayFromZero() {
        assertEquals(
                -4,
                service.calculateLeaguePoints(-4, -3));
    }

    @Test
    void calculateLeaguePointsShouldKeepExactIntegerAverage() {
        assertEquals(
                6,
                service.calculateLeaguePoints(5, 7));
    }
}