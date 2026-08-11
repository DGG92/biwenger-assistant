package com.artajerjes.biwengerassistant.player;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlert;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlertLevel;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionReason;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@ExtendWith(MockitoExtension.class)
class PlayerProtectionServiceTest {

    @Mock
    private PlayerMatchReportRepository playerMatchReportRepository;

    @InjectMocks
    private PlayerProtectionService playerProtectionService;

    @Test
    void shouldProtectPlayerWithFastValueRiseAndExcellentRecentForm() {
        Player player = createOwnedPlayer(
                10L,
                1_000_000L,
                120_000L,
                false);

        when(playerMatchReportRepository
                .findTop2ByPlayer_IdOrderByMatchDateDesc(10L))
                .thenReturn(List.of(
                        createReport(
                                player,
                                1002L,
                                "J2",
                                true,
                                11),
                        createReport(
                                player,
                                1001L,
                                "J1",
                                true,
                                9)));

        PlayerProtectionAlert result = playerProtectionService.calculate(player);

        assertEquals(
                PlayerProtectionAlertLevel.PROTECT,
                result.level());

        assertEquals(70, result.score());

        assertEquals(
                List.of(
                        PlayerProtectionReason.VALUE_RISING_FAST,
                        PlayerProtectionReason.EXCELLENT_RECENT_FORM),
                result.reasons());
    }

    @Test
    void shouldWatchPlayerWithModerateValueRise() {
        Player player = createOwnedPlayer(
                11L,
                1_000_000L,
                60_000L,
                false);

        when(playerMatchReportRepository
                .findTop2ByPlayer_IdOrderByMatchDateDesc(11L))
                .thenReturn(List.of());

        PlayerProtectionAlert result = playerProtectionService.calculate(player);

        assertEquals(
                PlayerProtectionAlertLevel.NONE,
                result.level());

        assertEquals(25, result.score());

        assertEquals(
                List.of(PlayerProtectionReason.VALUE_RISING),
                result.reasons());
    }

    @Test
    void shouldResetRecentFormWhenPlayerMissedLatestMatch() {
        Player player = createOwnedPlayer(
                12L,
                1_000_000L,
                0L,
                false);

        when(playerMatchReportRepository
                .findTop2ByPlayer_IdOrderByMatchDateDesc(12L))
                .thenReturn(List.of(
                        createReport(
                                player,
                                1202L,
                                "J2",
                                false,
                                null),
                        createReport(
                                player,
                                1201L,
                                "J1",
                                true,
                                15)));

        PlayerProtectionAlert result = playerProtectionService.calculate(player);

        assertEquals(
                PlayerProtectionAlertLevel.NONE,
                result.level());

        assertEquals(0, result.score());
    }

    @Test
    void shouldReduceProtectionScoreWhenPlayerIsInjured() {
        Player player = createOwnedPlayer(
                13L,
                1_000_000L,
                120_000L,
                true);

        when(playerMatchReportRepository
                .findTop2ByPlayer_IdOrderByMatchDateDesc(13L))
                .thenReturn(List.of());

        PlayerProtectionAlert result = playerProtectionService.calculate(player);

        assertEquals(
                PlayerProtectionAlertLevel.NONE,
                result.level());

        assertEquals(15, result.score());

        assertEquals(
                List.of(
                        PlayerProtectionReason.VALUE_RISING_FAST,
                        PlayerProtectionReason.INJURED),
                result.reasons());
    }

    @Test
    void shouldReturnNoneForFreePlayer() {
        League league = new League(
                "Liga",
                "league-1");

        Player player = new Player(
                "100",
                "Jugador libre",
                List.of(PlayerPosition.DL),
                "Equipo",
                1_000_000L,
                league);

        ReflectionTestUtils.setField(
                player,
                "id",
                14L);

        PlayerProtectionAlert result = playerProtectionService.calculate(player);

        assertEquals(
                PlayerProtectionAlertLevel.NONE,
                result.level());

        assertEquals(0, result.score());

        assertEquals(
                List.of(),
                result.reasons());
    }

    private Player createOwnedPlayer(
            Long id,
            Long marketValue,
            Long valueFluctuation,
            boolean injured) {

        League league = new League(
                "Liga",
                "league-1");

        Manager manager = new Manager(
                123L,
                "Manager",
                null,
                0,
                0,
                0L,
                0L,
                1,
                "manager",
                league);

        Player player = new Player(
                id.toString(),
                "Jugador " + id,
                List.of(PlayerPosition.DL),
                "Equipo",
                marketValue,
                league);

        ReflectionTestUtils.setField(
                player,
                "id",
                id);

        ReflectionTestUtils.setField(
                player,
                "owner",
                manager);

        ReflectionTestUtils.setField(
                player,
                "valueFluctuation",
                valueFluctuation);

        ReflectionTestUtils.setField(
                player,
                "injured",
                injured);

        return player;
    }

    private PlayerMatchReport createReport(
            Player player,
            Long matchId,
            String roundShort,
            boolean participated,
            Integer points) {

        return new PlayerMatchReport(
                player,
                matchId,
                matchId,
                "Jornada",
                roundShort,
                LocalDateTime.of(
                        2026,
                        8,
                        10,
                        21,
                        0),
                "2026-2027",
                participated,
                participated ? null : "injured",
                points);
    }
}