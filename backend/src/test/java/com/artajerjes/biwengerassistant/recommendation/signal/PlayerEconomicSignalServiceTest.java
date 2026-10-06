package com.artajerjes.biwengerassistant.recommendation.signal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mockito;

import com.artajerjes.biwengerassistant.history.PlayerPriceHistory;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.history.PlayerPriceSource;

class PlayerEconomicSignalServiceTest {

    private static final Long LEAGUE_ID = 1L;
    private static final Long PLAYER_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(
            2026,
            10,
            6);

    private final PlayerPriceHistoryRepository repository = Mockito.mock(
            PlayerPriceHistoryRepository.class);

    private final PlayerEconomicSignalService service = new PlayerEconomicSignalService(
            repository);

    @Test
    void shouldDetectSustainedPositiveMomentumWithNeutralAcceleration() {

        PlayerEconomicSignals signals = analyze(
                1_000_000L,
                1_010_000L,
                1_020_000L,
                1_030_000L,
                1_040_300L,
                1_050_600L,
                1_060_900L,
                1_071_200L);

        assertTrue(signals.valueTrendAvailable());
        assertTrue(signals.momentumAvailable());
        assertTrue(signals.accelerationAvailable());

        assertTrue(
                signals.marketMomentumPercentPerDay() > 0);

        assertEquals(
                0.0,
                signals.valueAccelerationPercentPerDaySquared(),
                0.02);

        assertEquals(
                1.0,
                signals.trendConsistency(),
                0.0001);
    }

    @Test
    void shouldDetectPositiveAccelerationWhenRiseIsGainingStrength() {

        PlayerEconomicSignals signals = service.analyzeHistory(
                List.of(
                        price(TODAY.minusDays(6), 1_000_000L),
                        price(TODAY.minusDays(3), 1_010_000L),
                        price(TODAY, 1_060_000L)));

        assertTrue(
                signals.marketMomentumPercentPerDay() > 0);

        assertTrue(
                signals.valueAccelerationPercentPerDaySquared() > 0);
    }

    @Test
    void shouldDetectDecelerationWhilePriceIsStillRising() {

        PlayerEconomicSignals signals = service.analyzeHistory(
                List.of(
                        price(TODAY.minusDays(6), 1_000_000L),
                        price(TODAY.minusDays(3), 1_050_000L),
                        price(TODAY, 1_060_000L)));

        assertTrue(
                signals.marketMomentumPercentPerDay() > 0);

        assertTrue(
                signals.valueAccelerationPercentPerDaySquared() < 0);
    }

    @Test
    void shouldDetectSustainedNegativeMomentum() {

        PlayerEconomicSignals signals = service.analyzeHistory(
                List.of(
                        price(TODAY.minusDays(6), 1_060_000L),
                        price(TODAY.minusDays(3), 1_030_000L),
                        price(TODAY, 1_000_000L)));

        assertTrue(
                signals.marketMomentumPercentPerDay() < 0);
    }

    @Test
    void shouldDetectAcceleratingFall() {

        PlayerEconomicSignals signals = service.analyzeHistory(
                List.of(
                        price(TODAY.minusDays(6), 1_060_000L),
                        price(TODAY.minusDays(3), 1_050_000L),
                        price(TODAY, 1_000_000L)));

        assertTrue(
                signals.marketMomentumPercentPerDay() < 0);

        assertTrue(
                signals.valueAccelerationPercentPerDaySquared() < 0);
    }

    @Test
    void shouldDetectFallLosingStrength() {

        PlayerEconomicSignals signals = service.analyzeHistory(
                List.of(
                        price(TODAY.minusDays(6), 1_060_000L),
                        price(TODAY.minusDays(3), 1_010_000L),
                        price(TODAY, 1_000_000L)));

        assertTrue(
                signals.marketMomentumPercentPerDay() < 0);

        assertTrue(
                signals.valueAccelerationPercentPerDaySquared() > 0);
    }

    @Test
    void shouldTreatStableHistoryAsNeutralSignal() {

        PlayerEconomicSignals signals = analyze(
                1_000_000L,
                1_000_000L,
                1_000_000L,
                1_000_000L,
                1_000_000L,
                1_000_000L,
                1_000_000L,
                1_000_000L);

        assertEquals(
                0.0,
                signals.valueTrend7DaysPercent(),
                0.0001);

        assertEquals(
                0.0,
                signals.marketMomentumPercentPerDay(),
                0.0001);

        assertEquals(
                0.0,
                signals.valueAccelerationPercentPerDaySquared(),
                0.0001);

        assertEquals(
                1.0,
                signals.trendConsistency(),
                0.0001);
    }

    @Test
    void shouldKeepInsufficientHistoryUnavailableInsteadOfInventingZero() {

        PlayerEconomicSignals signals = service.analyzeHistory(
                List.of(
                        price(TODAY.minusDays(1), 1_000_000L),
                        price(TODAY, 1_020_000L)));

        assertTrue(signals.hasAnyEconomicEvidence());

        assertFalse(signals.valueTrendAvailable());
        assertFalse(signals.momentumAvailable());
        assertFalse(signals.accelerationAvailable());
        assertFalse(signals.consistencyAvailable());

        assertNull(signals.valueTrend7DaysPercent());
        assertNull(signals.marketMomentumPercentPerDay());
        assertNull(signals.valueAccelerationPercentPerDaySquared());
        assertNull(signals.trendConsistency());
    }

    @Test
    void shouldNormalizeMomentumUsingActualElapsedDays() {

        PlayerEconomicSignals signals = service.analyzeHistory(
                List.of(
                        price(TODAY.minusDays(6), 1_000_000L),
                        price(TODAY.minusDays(4), 1_020_000L),
                        price(TODAY, 1_060_000L)));

        /*
         * El ancla reciente buscada es D-3.
         * Se acepta D-4 por el margen máximo de un día.
         *
         * 1.020.000 -> 1.060.000 = 3,921568... %
         * durante 4 días reales.
         */
        assertEquals(
                0.980392,
                signals.marketMomentumPercentPerDay(),
                0.00001);
    }

    @Test
    void shouldRejectStaleAnchorInsteadOfStretchingOldData() {

        PlayerEconomicSignals signals = service.analyzeHistory(
                List.of(
                        price(TODAY.minusDays(5), 1_000_000L),
                        price(TODAY, 1_100_000L)));

        assertFalse(signals.momentumAvailable());
        assertFalse(signals.accelerationAvailable());
    }

    @Test
    void shouldGiveLowerConsistencyToVolatileSeriesThanSmoothSeries() {

        PlayerEconomicSignals smooth = analyze(
                1_000_000L,
                1_010_000L,
                1_020_000L,
                1_030_000L,
                1_040_000L,
                1_050_000L,
                1_060_000L,
                1_070_000L);

        PlayerEconomicSignals volatileSignals = analyze(
                1_000_000L,
                1_080_000L,
                990_000L,
                1_100_000L,
                1_010_000L,
                1_120_000L,
                1_030_000L,
                1_070_000L);

        assertTrue(
                smooth.trendConsistency()
                        > volatileSignals.trendConsistency());

        assertEquals(
                1.0,
                smooth.trendConsistency(),
                0.0001);
    }

    @Test
    void shouldAnalyzeWholeLeagueWithSingleRepositoryQuery() {

        LocalDate fromDate = LocalDate.now()
                .minusDays(8);

        List<PlayerPriceHistory> history = List.of(
                priceForPlayer(
                        10L,
                        fromDate.plusDays(1),
                        1_000_000L),
                priceForPlayer(
                        10L,
                        fromDate.plusDays(4),
                        1_030_000L),
                priceForPlayer(
                        10L,
                        fromDate.plusDays(7),
                        1_060_000L),
                priceForPlayer(
                        20L,
                        fromDate.plusDays(1),
                        2_000_000L),
                priceForPlayer(
                        20L,
                        fromDate.plusDays(4),
                        1_950_000L),
                priceForPlayer(
                        20L,
                        fromDate.plusDays(7),
                        1_900_000L));

        Mockito.when(
                        repository.findRecentPricesByLeagueId(
                                eq(LEAGUE_ID),
                                eq(fromDate)))
                .thenReturn(history);

        Map<Long, PlayerEconomicSignals> result = service.analyzeLeague(
                LEAGUE_ID);

        assertEquals(2, result.size());

        assertTrue(
                result.get(10L)
                        .marketMomentumPercentPerDay() > 0);

        assertTrue(
                result.get(20L)
                        .marketMomentumPercentPerDay() < 0);

        Mockito.verify(
                        repository,
                        Mockito.times(1))
                .findRecentPricesByLeagueId(
                        LEAGUE_ID,
                        fromDate);
    }

    private PlayerEconomicSignals analyze(
            Long... values) {

        java.util.ArrayList<PlayerPriceHistory> history = new java.util.ArrayList<>();

        for (int index = 0; index < values.length; index++) {

            int daysAgo = values.length - 1 - index;

            history.add(
                    price(
                            TODAY.minusDays(daysAgo),
                            values[index]));
        }

        return service.analyzeHistory(history);
    }

    private PlayerPriceHistory price(
            LocalDate date,
            long value) {

        return priceForPlayer(
                PLAYER_ID,
                date,
                value);
    }

    private PlayerPriceHistory priceForPlayer(
            Long playerId,
            LocalDate date,
            long value) {

        return new PlayerPriceHistory(
                playerId,
                LEAGUE_ID,
                date,
                value,
                PlayerPriceSource.BIWENGER_DETAIL,
                LocalDateTime.of(
                        date,
                        java.time.LocalTime.NOON));
    }
}
