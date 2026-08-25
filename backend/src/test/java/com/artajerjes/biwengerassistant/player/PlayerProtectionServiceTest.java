package com.artajerjes.biwengerassistant.player;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlert;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionAlertLevel;
import com.artajerjes.biwengerassistant.player.dto.PlayerProtectionReason;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignalService;
import com.artajerjes.biwengerassistant.recommendation.signal.PlayerPerformanceSignals;

@ExtendWith(MockitoExtension.class)
class PlayerProtectionServiceTest {

        @Mock
        private PlayerPerformanceSignalService playerPerformanceSignalService;

        @InjectMocks
        private PlayerProtectionService playerProtectionService;

        @Test
        void shouldProtectPlayerWithFastValueRiseAndExcellentRecentForm() {
                Player player = createOwnedPlayer(
                                10L,
                                1_000_000L,
                                120_000L,
                                false);

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                10.33,
                                                                2,
                                                                true,
                                                                0,
                                                                0));

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

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                0,
                                                                0,
                                                                false,
                                                                0,
                                                                0));

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

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                0,
                                                                0,
                                                                false,
                                                                0,
                                                                0));

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

                when(playerPerformanceSignalService.analyze(player))
                                .thenReturn(
                                                new PlayerPerformanceSignals(
                                                                0,
                                                                0,
                                                                false,
                                                                0,
                                                                0));

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
                                "status",
                                injured
                                                ? PlayerStatus.INJURED
                                                : PlayerStatus.OK);

                return player;
        }
}