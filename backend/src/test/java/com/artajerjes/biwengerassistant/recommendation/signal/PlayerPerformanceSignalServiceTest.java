package com.artajerjes.biwengerassistant.recommendation.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@ExtendWith(MockitoExtension.class)
class PlayerPerformanceSignalServiceTest {

        private static final Long PLAYER_ID = 182L;

        @Mock
        private PlayerMatchReportRepository playerMatchReportRepository;

        private PlayerPerformanceSignalService playerPerformanceSignalService;
        private Player player;

        @BeforeEach
        void setUp() {
                playerPerformanceSignalService = new PlayerPerformanceSignalService(
                                playerMatchReportRepository);

                League league = new League(
                                "Liga",
                                "league-1");

                player = new Player(
                                "42370",
                                "Facundo Bernal",
                                List.of(PlayerPosition.MC),
                                "Getafe",
                                1_000_000L,
                                league);

                ReflectionTestUtils.setField(
                                player,
                                "id",
                                PLAYER_ID);
        }

        @Test
        void shouldUseChronologicalRecentMatchesEvenWhenRoundNumbersAreOutOfOrder() {
                List<PlayerMatchReport> reports = List.of(
                                report(
                                                50732L,
                                                4901L,
                                                "Jornada 3",
                                                "J3",
                                                2026,
                                                8,
                                                29,
                                                "2026-2027",
                                                true,
                                                5),
                                report(
                                                50716L,
                                                4937L,
                                                "Jornada 1 (aplazada)",
                                                "J1",
                                                2026,
                                                8,
                                                25,
                                                "2026-2027",
                                                true,
                                                7),
                                report(
                                                50721L,
                                                4900L,
                                                "Jornada 2",
                                                "J2",
                                                2026,
                                                8,
                                                21,
                                                "2026-2027",
                                                true,
                                                5));

                mockRecent(reports);
                mockHistorical(reports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertEquals(
                                3,
                                result.recentSampleSize());

                assertEquals(
                                5.666666666666667,
                                result.recentWeightedAverage(),
                                0.000001);

                assertEquals(
                                3,
                                result.historicalSampleSize());

                assertEquals(
                                5.666666666666667,
                                result.historicalAveragePoints(),
                                0.000001);
        }

        @Test
        void shouldBreakRecentStreakWhenPlayerDoesNotParticipate() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                51120L,
                                                5120L,
                                                "Jornada 12",
                                                "J12",
                                                2026,
                                                11,
                                                8,
                                                "2026-2027",
                                                true,
                                                8),
                                report(
                                                51110L,
                                                5110L,
                                                "Jornada 11",
                                                "J11",
                                                2026,
                                                11,
                                                1,
                                                "2026-2027",
                                                true,
                                                6),
                                report(
                                                51060L,
                                                5060L,
                                                "Jornada 6",
                                                "J6",
                                                2026,
                                                9,
                                                27,
                                                "2026-2027",
                                                false,
                                                null),
                                report(
                                                51050L,
                                                5050L,
                                                "Jornada 5",
                                                "J5",
                                                2026,
                                                9,
                                                20,
                                                "2026-2027",
                                                true,
                                                10));

                mockRecent(recentReports);

                mockHistorical(List.of(
                                recentReports.get(0),
                                recentReports.get(1),
                                recentReports.get(3)));

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertEquals(
                                2,
                                result.recentSampleSize());

                assertEquals(
                                7.333333333333333,
                                result.recentWeightedAverage(),
                                0.000001);

                assertEquals(
                                3,
                                result.historicalSampleSize());

                assertEquals(
                                8.0,
                                result.historicalAveragePoints(),
                                0.000001);
        }

        @Test
        void shouldKeepSingleMatchAfterAbsenceAsInsufficientRecentSample() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                51110L,
                                                5110L,
                                                "Jornada 11",
                                                "J11",
                                                2026,
                                                11,
                                                1,
                                                "2026-2027",
                                                true,
                                                9),
                                report(
                                                51060L,
                                                5060L,
                                                "Jornada 6",
                                                "J6",
                                                2026,
                                                9,
                                                27,
                                                "2026-2027",
                                                false,
                                                null),
                                report(
                                                51050L,
                                                5050L,
                                                "Jornada 5",
                                                "J5",
                                                2026,
                                                9,
                                                20,
                                                "2026-2027",
                                                true,
                                                7));

                mockRecent(recentReports);

                mockHistorical(List.of(
                                recentReports.get(0),
                                recentReports.get(2)));

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertEquals(
                                0,
                                result.recentSampleSize());

                assertEquals(
                                0.0,
                                result.recentWeightedAverage(),
                                0.000001);

                assertEquals(
                                2,
                                result.historicalSampleSize());

                assertEquals(
                                8.0,
                                result.historicalAveragePoints(),
                                0.000001);
        }

        @Test
        void shouldNotCarryRecentStreakAcrossSeasonBoundary() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                52020L,
                                                5202L,
                                                "Jornada 2",
                                                "J2",
                                                2026,
                                                8,
                                                23,
                                                "2026-2027",
                                                true,
                                                8),
                                report(
                                                52010L,
                                                5201L,
                                                "Jornada 1",
                                                "J1",
                                                2026,
                                                8,
                                                16,
                                                "2026-2027",
                                                true,
                                                6),
                                report(
                                                51938L,
                                                5198L,
                                                "Jornada 38",
                                                "J38",
                                                2026,
                                                5,
                                                24,
                                                "2025-2026",
                                                true,
                                                10),
                                report(
                                                51937L,
                                                5197L,
                                                "Jornada 37",
                                                "J37",
                                                2026,
                                                5,
                                                17,
                                                "2025-2026",
                                                true,
                                                9));

                mockRecent(recentReports);
                mockHistorical(recentReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertEquals(
                                2,
                                result.recentSampleSize());

                assertEquals(
                                7.333333333333333,
                                result.recentWeightedAverage(),
                                0.000001);
        }

        @Test
        void shouldAllowHistoricalPerformanceToCrossSeasonBoundary() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                52020L,
                                                5202L,
                                                "Jornada 2",
                                                "J2",
                                                2026,
                                                8,
                                                23,
                                                "2026-2027",
                                                true,
                                                8),
                                report(
                                                52010L,
                                                5201L,
                                                "Jornada 1",
                                                "J1",
                                                2026,
                                                8,
                                                16,
                                                "2026-2027",
                                                true,
                                                6),
                                report(
                                                51938L,
                                                5198L,
                                                "Jornada 38",
                                                "J38",
                                                2026,
                                                5,
                                                24,
                                                "2025-2026",
                                                true,
                                                10),
                                report(
                                                51937L,
                                                5197L,
                                                "Jornada 37",
                                                "J37",
                                                2026,
                                                5,
                                                17,
                                                "2025-2026",
                                                true,
                                                4));

                mockRecent(recentReports);
                mockHistorical(recentReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertEquals(
                                2,
                                result.recentSampleSize());

                assertEquals(
                                4,
                                result.historicalSampleSize());

                assertEquals(
                                7.0,
                                result.historicalAveragePoints(),
                                0.000001);
        }

        @Test
        void shouldAcceptWinterSigningWithoutEarlierReports() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                53018L,
                                                5318L,
                                                "Jornada 18",
                                                "J18",
                                                2027,
                                                1,
                                                17,
                                                "2026-2027",
                                                true,
                                                8),
                                report(
                                                53017L,
                                                5317L,
                                                "Jornada 17",
                                                "J17",
                                                2027,
                                                1,
                                                10,
                                                "2026-2027",
                                                true,
                                                6));

                mockRecent(recentReports);
                mockHistorical(recentReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertEquals(
                                2,
                                result.recentSampleSize());

                assertEquals(
                                7.333333333333333,
                                result.recentWeightedAverage(),
                                0.000001);

                assertEquals(
                                2,
                                result.historicalSampleSize());

                assertEquals(
                                7.0,
                                result.historicalAveragePoints(),
                                0.000001);
        }

        private void mockRecent(List<PlayerMatchReport> reports) {
                when(
                                playerMatchReportRepository
                                                .findTop5ByPlayer_IdOrderByMatchDateDesc(
                                                                PLAYER_ID))
                                .thenReturn(reports);
        }

        private void mockHistorical(List<PlayerMatchReport> reports) {
                when(
                                playerMatchReportRepository
                                                .findTop10ByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                                PLAYER_ID))
                                .thenReturn(reports);
        }

        private PlayerMatchReport report(
                        Long matchId,
                        Long roundId,
                        String roundName,
                        String roundShort,
                        int year,
                        int month,
                        int day,
                        String season,
                        boolean participated,
                        Integer points) {

                return new PlayerMatchReport(
                                player,
                                matchId,
                                roundId,
                                roundName,
                                roundShort,
                                LocalDateTime.of(
                                                year,
                                                month,
                                                day,
                                                20,
                                                0),
                                season,
                                participated,
                                null,
                                points);
        }
}