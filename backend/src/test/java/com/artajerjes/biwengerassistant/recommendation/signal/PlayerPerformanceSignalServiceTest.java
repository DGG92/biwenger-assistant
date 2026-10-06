package com.artajerjes.biwengerassistant.recommendation.signal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                2,
                                result.recentObservedMatches());

                assertTrue(
                                result.recentSignalAvailable());

                assertTrue(
                                result.hasAnyPerformanceEvidence());

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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                1,
                                result.recentObservedMatches());

                assertFalse(
                                result.recentSignalAvailable());

                assertTrue(
                                result.hasAnyPerformanceEvidence());

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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                                previousSeason(),
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
                                                previousSeason(),
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
                                                currentSeason(),
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
                                                currentSeason(),
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
                                                previousSeason(),
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
                                                previousSeason(),
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
                                                currentSeason(),
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
                                                currentSeason(),
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

                assertEquals(
                                2,
                                result.recentObservedMatches());

                assertTrue(
                                result.recentSignalAvailable());

                assertFalse(
                                result.historicalSignalAvailable());

                assertTrue(
                                result.hasAnyPerformanceEvidence());
        }

        @Test
        void shouldExposeNoPerformanceEvidenceWhenPlayerHasNoReports() {
                mockRecent(List.of());
                mockHistorical(List.of());

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertEquals(
                                0,
                                result.recentObservedMatches());

                assertEquals(
                                0,
                                result.recentSampleSize());

                assertEquals(
                                0,
                                result.historicalSampleSize());

                assertFalse(
                                result.recentSignalAvailable());

                assertFalse(
                                result.historicalSignalAvailable());

                assertFalse(
                                result.hasAnyPerformanceEvidence());
        }

        @Test
        void shouldExposeHistoricalSignalOnlyWhenMinimumSampleIsReached() {
                List<PlayerMatchReport> historicalReports = List.of(
                                report(
                                                54005L,
                                                5405L,
                                                "Jornada 5",
                                                "J5",
                                                2026,
                                                9,
                                                13,
                                                currentSeason(),
                                                true,
                                                10),
                                report(
                                                54004L,
                                                5404L,
                                                "Jornada 4",
                                                "J4",
                                                2026,
                                                9,
                                                6,
                                                currentSeason(),
                                                true,
                                                8),
                                report(
                                                54003L,
                                                5403L,
                                                "Jornada 3",
                                                "J3",
                                                2026,
                                                8,
                                                30,
                                                currentSeason(),
                                                true,
                                                6),
                                report(
                                                54002L,
                                                5402L,
                                                "Jornada 2",
                                                "J2",
                                                2026,
                                                8,
                                                23,
                                                currentSeason(),
                                                true,
                                                4),
                                report(
                                                54001L,
                                                5401L,
                                                "Jornada 1",
                                                "J1",
                                                2026,
                                                8,
                                                16,
                                                currentSeason(),
                                                true,
                                                2));

                mockRecent(List.of());
                mockHistorical(historicalReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertEquals(
                                0,
                                result.recentObservedMatches());

                assertEquals(
                                0,
                                result.recentSampleSize());

                assertFalse(
                                result.recentSignalAvailable());

                assertEquals(
                                5,
                                result.historicalSampleSize());

                assertEquals(
                                6.0,
                                result.historicalAveragePoints(),
                                0.000001);

                assertTrue(
                                result.historicalSignalAvailable());

                assertTrue(
                                result.hasAnyPerformanceEvidence());
        }

        @Test
        void shouldKeepAdvancedSignalsUnavailableWithInsufficientHistoricalSample() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                55002L,
                                                5502L,
                                                "Jornada 2",
                                                "J2",
                                                2026,
                                                8,
                                                23,
                                                currentSeason(),
                                                true,
                                                8),
                                report(
                                                55001L,
                                                5501L,
                                                "Jornada 1",
                                                "J1",
                                                2026,
                                                8,
                                                16,
                                                currentSeason(),
                                                true,
                                                6));

                mockRecent(recentReports);
                mockHistorical(recentReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertTrue(result.recentSignalAvailable());
                assertFalse(result.historicalSignalAvailable());

                assertFalse(result.recentFormDeltaAvailable());
                assertFalse(result.historicalConsistencyAvailable());

                assertNull(result.recentFormDelta());
                assertNull(result.historicalConsistency());
        }

        @Test
        void shouldExposePerfectConsistencyForStableHistoricalPerformance() {
                List<PlayerMatchReport> historicalReports = historicalReports(
                                6,
                                6,
                                6,
                                6,
                                6);

                mockRecent(List.of());
                mockHistorical(historicalReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertTrue(result.historicalSignalAvailable());
                assertTrue(result.historicalConsistencyAvailable());

                assertEquals(
                                1.0,
                                result.historicalConsistency(),
                                0.000001);
        }

        @Test
        void shouldExposeZeroConsistencyForExtremelyVolatileHistoricalPerformance() {
                List<PlayerMatchReport> historicalReports = historicalReports(
                                0,
                                12,
                                0,
                                12,
                                0,
                                12);

                mockRecent(List.of());
                mockHistorical(historicalReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertTrue(result.historicalConsistencyAvailable());

                assertEquals(
                                0.0,
                                result.historicalConsistency(),
                                0.000001);
        }

        @Test
        void shouldNormalizeIntermediateHistoricalConsistency() {
                List<PlayerMatchReport> historicalReports = historicalReports(
                                2,
                                4,
                                6,
                                8,
                                10);

                mockRecent(List.of());
                mockHistorical(historicalReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                /*
                 * Media = 6.
                 *
                 * Desviación poblacional:
                 * sqrt((16 + 4 + 0 + 4 + 16) / 5)
                 * = sqrt(8)
                 * = 2.828427...
                 *
                 * Normalización 14.C:
                 * 1 - ((2.828427 - 1) / (6 - 1))
                 * = 0.634314...
                 */
                assertEquals(
                                0.634314575,
                                result.historicalConsistency(),
                                0.000001);
        }

        @Test
        void shouldExposePositiveRecentFormDeltaWhenPlayerIsImproving() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                56002L,
                                                5602L,
                                                "Jornada 7",
                                                "J7",
                                                2026,
                                                9,
                                                27,
                                                currentSeason(),
                                                true,
                                                10),
                                report(
                                                56001L,
                                                5601L,
                                                "Jornada 6",
                                                "J6",
                                                2026,
                                                9,
                                                20,
                                                currentSeason(),
                                                true,
                                                8));

                List<PlayerMatchReport> historicalReports = historicalReports(
                                4,
                                4,
                                4,
                                4,
                                4);

                mockRecent(recentReports);
                mockHistorical(historicalReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertTrue(result.recentFormDeltaAvailable());

                /*
                 * Forma reciente ponderada:
                 * (10 * 2 + 8 * 1) / 3 = 9.333333...
                 *
                 * Histórico = 4.
                 */
                assertEquals(
                                5.333333333333333,
                                result.recentFormDelta(),
                                0.000001);
        }

        @Test
        void shouldExposeNegativeRecentFormDeltaWhenPlayerIsDeclining() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                57002L,
                                                5702L,
                                                "Jornada 7",
                                                "J7",
                                                2026,
                                                9,
                                                27,
                                                currentSeason(),
                                                true,
                                                2),
                                report(
                                                57001L,
                                                5701L,
                                                "Jornada 6",
                                                "J6",
                                                2026,
                                                9,
                                                20,
                                                currentSeason(),
                                                true,
                                                4));

                List<PlayerMatchReport> historicalReports = historicalReports(
                                8,
                                8,
                                8,
                                8,
                                8);

                mockRecent(recentReports);
                mockHistorical(historicalReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertTrue(result.recentFormDeltaAvailable());

                assertEquals(
                                -5.333333333333333,
                                result.recentFormDelta(),
                                0.000001);
        }

        @Test
        void shouldExposeNeutralRecentFormDeltaWhenRecentFormMatchesBaseline() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                58002L,
                                                5802L,
                                                "Jornada 7",
                                                "J7",
                                                2026,
                                                9,
                                                27,
                                                currentSeason(),
                                                true,
                                                6),
                                report(
                                                58001L,
                                                5801L,
                                                "Jornada 6",
                                                "J6",
                                                2026,
                                                9,
                                                20,
                                                currentSeason(),
                                                true,
                                                6));

                List<PlayerMatchReport> historicalReports = historicalReports(
                                6,
                                6,
                                6,
                                6,
                                6);

                mockRecent(recentReports);
                mockHistorical(historicalReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertTrue(result.recentFormDeltaAvailable());

                assertEquals(
                                0.0,
                                result.recentFormDelta(),
                                0.000001);
        }

        @Test
        void shouldKeepRecentFormDeltaUnavailableWithoutReliableRecentSignal() {
                List<PlayerMatchReport> recentReports = List.of(
                                report(
                                                59001L,
                                                5901L,
                                                "Jornada 7",
                                                "J7",
                                                2026,
                                                9,
                                                27,
                                                currentSeason(),
                                                true,
                                                10));

                List<PlayerMatchReport> historicalReports = historicalReports(
                                4,
                                5,
                                6,
                                7,
                                8);

                mockRecent(recentReports);
                mockHistorical(historicalReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(player);

                assertFalse(result.recentSignalAvailable());
                assertTrue(result.historicalSignalAvailable());

                assertFalse(result.recentFormDeltaAvailable());
                assertNull(result.recentFormDelta());

                assertTrue(result.historicalConsistencyAvailable());
        }

        @Test
        void shouldKeepAdvancedHistoricalSignalsUnavailableForCoach() {
                League league = new League(
                                "Liga",
                                "league-coach");

                Player coach = new Player(
                                "99999",
                                "Entrenador",
                                List.of(PlayerPosition.E),
                                "Club",
                                1_000_000L,
                                league);

                ReflectionTestUtils.setField(
                                coach,
                                "id",
                                PLAYER_ID);

                List<PlayerMatchReport> recentReports = List.of(
                                reportForPlayer(
                                                coach,
                                                60002L,
                                                6002L,
                                                2026,
                                                9,
                                                27,
                                                currentSeason(),
                                                3),
                                reportForPlayer(
                                                coach,
                                                60001L,
                                                6001L,
                                                2026,
                                                9,
                                                20,
                                                currentSeason(),
                                                3));

                mockRecent(recentReports);

                PlayerPerformanceSignals result = playerPerformanceSignalService
                                .analyze(coach);

                assertEquals(
                                0,
                                result.historicalSampleSize());

                assertFalse(result.historicalSignalAvailable());
                assertFalse(result.historicalConsistencyAvailable());
                assertFalse(result.recentFormDeltaAvailable());

                assertNull(result.historicalConsistency());
                assertNull(result.recentFormDelta());
        }

        @Test
        void shouldKeepLegacyConstructorsCompatibleWithAdvancedSignalsUnavailable() {
                PlayerPerformanceSignals motor20 = new PlayerPerformanceSignals(
                                7.0,
                                3,
                                false,
                                6.0,
                                8);

                assertEquals(
                                3,
                                motor20.recentObservedMatches());

                assertNull(motor20.recentFormDelta());
                assertNull(motor20.historicalConsistency());

                PlayerPerformanceSignals sparseData = new PlayerPerformanceSignals(
                                7.0,
                                3,
                                false,
                                6.0,
                                8,
                                4);

                assertEquals(
                                4,
                                sparseData.recentObservedMatches());

                assertNull(sparseData.recentFormDelta());
                assertNull(sparseData.historicalConsistency());
        }

        private List<PlayerMatchReport> historicalReports(
                        Integer... points) {

                java.util.ArrayList<PlayerMatchReport> reports = new java.util.ArrayList<>();

                for (int index = 0; index < points.length; index++) {

                        reports.add(
                                        report(
                                                        70000L + index,
                                                        7000L + index,
                                                        "Jornada " + (index + 1),
                                                        "J" + (index + 1),
                                                        2026,
                                                        8,
                                                        1 + index,
                                                        currentSeason(),
                                                        true,
                                                        points[index]));
                }

                return List.copyOf(reports);
        }

        private PlayerMatchReport reportForPlayer(
                        Player reportPlayer,
                        Long matchId,
                        Long roundId,
                        int year,
                        int month,
                        int day,
                        String season,
                        Integer points) {

                return new PlayerMatchReport(
                                reportPlayer,
                                matchId,
                                roundId,
                                "Jornada",
                                "J",
                                LocalDateTime.of(
                                                year,
                                                month,
                                                day,
                                                20,
                                                0),
                                season,
                                true,
                                null,
                                points);
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

        private String currentSeason() {
                LocalDate today = LocalDate.now();
                int year = today.getYear();

                return today.getMonthValue() >= 7
                                ? year + "-" + (year + 1)
                                : (year - 1) + "-" + year;
        }

        private String previousSeason() {
                LocalDate today = LocalDate.now();
                int year = today.getYear();

                return today.getMonthValue() >= 7
                                ? (year - 1) + "-" + year
                                : (year - 2) + "-" + (year - 1);
        }
}
