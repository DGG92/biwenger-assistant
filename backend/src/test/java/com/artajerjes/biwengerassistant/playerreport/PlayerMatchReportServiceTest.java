package com.artajerjes.biwengerassistant.playerreport;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerReport;

class PlayerMatchReportServiceTest {

    private PlayerMatchReportService service;

    @BeforeEach
    void setUp() {

        service = new PlayerMatchReportService(
                null,
                null,
                null,
                new CustomScoreEvaluator());
    }

    @Test
    void resolveLeaguePointsShouldUseCustomScoreForScoreId100() {

        String customScore = """
                (score2 * 0.5) + (score3 * 0.5)
                +savedPenalties * 3
                +penaltyMissed * -1
                +ownGoals * -1
                +if((pos1 or pos2), assists * 2)
                +if((pos3 or pos4), assists * 1)
                +if(pos1 and minutesPlayed > 70, 1)
                +if(pos2 and minutesPlayed > 70, 1)
                +if(pos3 and minutesPlayed > 70, 1)
                +if(pos4 and minutesPlayed > 70, 1)
                +if(homeScore == 0 and away and pos2, 1)
                +if(homeScore == 0 and away and pos1, 2)
                +if(goals > 0 and minutesPlayed < 31, 2)
                """;

        BiwengerPlayerReport report = new BiwengerPlayerReport(
                false,
                null,
                Map.of(
                        "1", 6,
                        "2", 3,
                        "3", 0),
                Map.of(
                        "pos1", true,
                        "score2", 3,
                        "score3", 0,
                        "away", true,
                        "homeScore", 3,
                        "awayScore", 0,
                        "minutesPlayed", 90),
                null);

        assertEquals(
                3,
                service.resolveLeaguePoints(
                        100,
                        customScore,
                        report));
    }

    @Test
    void resolveLeaguePointsShouldUseBiwengerPointsForStandardScore() {

        BiwengerPlayerReport report = new BiwengerPlayerReport(
                false,
                null,
                Map.of(
                        "1", 6,
                        "2", 3,
                        "3", 0,
                        "7", 2),
                Map.of(),
                null);

        assertEquals(
                3,
                service.resolveLeaguePoints(
                        2,
                        null,
                        report));

        assertEquals(
                6,
                service.resolveLeaguePoints(
                        1,
                        null,
                        report));

        assertEquals(
                2,
                service.resolveLeaguePoints(
                        7,
                        null,
                        report));
    }

    @Test
    void resolveLeaguePointsShouldReturnNullWhenCustomRawStatsAreMissing() {

        BiwengerPlayerReport report = new BiwengerPlayerReport(
                false,
                null,
                Map.of(
                        "2", 3,
                        "3", 0),
                null,
                null);

        assertNull(
                service.resolveLeaguePoints(
                        100,
                        "score2 + score3",
                        report));
    }

    @Test
    void resolveLeaguePointsShouldReturnNullWhenConfiguredStandardScoreIsMissing() {

        BiwengerPlayerReport report = new BiwengerPlayerReport(
                false,
                null,
                Map.of(
                        "2", 3,
                        "3", 0),
                Map.of(),
                null);

        assertNull(
                service.resolveLeaguePoints(
                        8,
                        null,
                        report));
    }
}