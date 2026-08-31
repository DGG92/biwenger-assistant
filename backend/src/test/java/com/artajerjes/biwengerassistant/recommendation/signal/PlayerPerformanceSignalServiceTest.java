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

    @Mock
    private PlayerMatchReportRepository playerMatchReportRepository;

    private PlayerPerformanceSignalService playerPerformanceSignalService;

    @BeforeEach
    void setUp() {

        playerPerformanceSignalService = new PlayerPerformanceSignalService(
                playerMatchReportRepository);
    }

    @Test
    void shouldUseChronologicalRecentMatchesEvenWhenRoundNumbersAreOutOfOrder() {

        League league = new League(
                "Liga",
                "league-1");

        Player player = new Player(
                "42370",
                "Facundo Bernal",
                List.of(PlayerPosition.MC),
                "Getafe",
                1_000_000L,
                league);

        ReflectionTestUtils.setField(
                player,
                "id",
                182L);

        List<PlayerMatchReport> reports = List.of(
                new PlayerMatchReport(
                        player,
                        50732L,
                        4901L,
                        "Jornada 3",
                        "J3",
                        LocalDateTime.of(
                                2026,
                                8,
                                29,
                                17,
                                0),
                        "2026-2027",
                        true,
                        null,
                        5),
                new PlayerMatchReport(
                        player,
                        50716L,
                        4937L,
                        "Jornada 1 (aplazada)",
                        "J1",
                        LocalDateTime.of(
                                2026,
                                8,
                                25,
                                21,
                                0),
                        "2026-2027",
                        true,
                        null,
                        7),
                new PlayerMatchReport(
                        player,
                        50721L,
                        4900L,
                        "Jornada 2",
                        "J2",
                        LocalDateTime.of(
                                2026,
                                8,
                                21,
                                21,
                                0),
                        "2026-2027",
                        true,
                        null,
                        5));

        when(
                playerMatchReportRepository
                        .findTop5ByPlayer_IdOrderByMatchDateDesc(
                                182L))
                .thenReturn(reports);

        when(
                playerMatchReportRepository
                        .findTop10ByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                182L))
                .thenReturn(reports);

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
}