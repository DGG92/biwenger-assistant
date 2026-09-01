package com.artajerjes.biwengerassistant.history;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.player.PlayerStatus;

@ExtendWith(MockitoExtension.class)
class PlayerSnapshotServiceTest {

    private static final Long LEAGUE_ID = 1L;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private PlayerSnapshotRepository playerSnapshotRepository;

    private PlayerSnapshotService service;

    @BeforeEach
    void setUp() {
        service = new PlayerSnapshotService(
                playerRepository,
                playerSnapshotRepository);
    }

    @Test
    void captureDailySnapshotsShouldCreateSnapshotWhenNoneExistsForToday() {

        Player player = createPlayer();

        LocalDateTime capturedAt = LocalDateTime.of(
                2026,
                9,
                1,
                12,
                0);

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(
                playerSnapshotRepository
                        .findByPlayerIdAndSnapshotDate(
                                player.getId(),
                                LocalDate.of(
                                        2026,
                                        9,
                                        1)))
                .thenReturn(Optional.empty());

        int captured = service.captureDailySnapshots(
                LEAGUE_ID,
                capturedAt);

        assertEquals(
                1,
                captured);

        ArgumentCaptor<PlayerSnapshot> captor = ArgumentCaptor.forClass(PlayerSnapshot.class);

        verify(playerSnapshotRepository)
                .save(captor.capture());

        PlayerSnapshot snapshot = captor.getValue();

        assertEquals(
                player.getId(),
                snapshot.getPlayerId());

        assertEquals(
                LEAGUE_ID,
                snapshot.getLeagueId());

        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        1),
                snapshot.getSnapshotDate());

        assertEquals(
                capturedAt,
                snapshot.getCapturedAt());

        assertEquals(
                42,
                snapshot.getPoints());

        assertEquals(
                5_000_000L,
                snapshot.getMarketValue());

        assertEquals(
                50_000L,
                snapshot.getValueFluctuation());

        assertEquals(
                PlayerStatus.OK,
                snapshot.getStatus());

        assertEquals(
                10L,
                snapshot.getTeamId());

        assertEquals(
                20L,
                snapshot.getOwnerId());

        assertEquals(
                4_000_000L,
                snapshot.getPurchasePrice());
    }

    @Test
    void captureDailySnapshotsShouldUpdateExistingSnapshotFromSameDay() {

        Player player = createPlayer();

        LocalDate snapshotDate = LocalDate.of(
                2026,
                9,
                1);

        LocalDateTime firstCapture = LocalDateTime.of(
                2026,
                9,
                1,
                10,
                0);

        LocalDateTime secondCapture = LocalDateTime.of(
                2026,
                9,
                1,
                20,
                0);

        PlayerSnapshot existing = new PlayerSnapshot(
                player,
                snapshotDate,
                firstCapture);

        player.updateCompetitionData(
                player.getName(),
                player.getSlug(),
                player.getPositions(),
                50,
                player.getTeamName(),
                player.getTeamId(),
                5_500_000L,
                PlayerStatus.DOUBT,
                100_000L);

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(
                playerSnapshotRepository
                        .findByPlayerIdAndSnapshotDate(
                                player.getId(),
                                snapshotDate))
                .thenReturn(Optional.of(existing));

        int captured = service.captureDailySnapshots(
                LEAGUE_ID,
                secondCapture);

        assertEquals(
                1,
                captured);

        verify(playerSnapshotRepository)
                .save(existing);

        assertEquals(
                secondCapture,
                existing.getCapturedAt());

        assertEquals(
                50,
                existing.getPoints());

        assertEquals(
                5_500_000L,
                existing.getMarketValue());

        assertEquals(
                100_000L,
                existing.getValueFluctuation());

        assertEquals(
                PlayerStatus.DOUBT,
                existing.getStatus());
    }

    @Test
    void captureDailySnapshotsShouldCreateDifferentSnapshotOnNextDay() {

        Player player = createPlayer();

        LocalDateTime capturedAt = LocalDateTime.of(
                2026,
                9,
                2,
                8,
                0);

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(player));

        when(
                playerSnapshotRepository
                        .findByPlayerIdAndSnapshotDate(
                                player.getId(),
                                LocalDate.of(
                                        2026,
                                        9,
                                        2)))
                .thenReturn(Optional.empty());

        service.captureDailySnapshots(
                LEAGUE_ID,
                capturedAt);

        ArgumentCaptor<PlayerSnapshot> captor = ArgumentCaptor.forClass(PlayerSnapshot.class);

        verify(playerSnapshotRepository)
                .save(captor.capture());

        assertEquals(
                LocalDate.of(
                        2026,
                        9,
                        2),
                captor.getValue().getSnapshotDate());
    }

    private Player createPlayer() {

        League league = org.mockito.Mockito.mock(League.class);
        Manager manager = org.mockito.Mockito.mock(Manager.class);

        when(league.getId())
                .thenReturn(LEAGUE_ID);

        when(manager.getId())
                .thenReturn(20L);

        Player player = new Player(
                "12345",
                "Test Player",
                List.of(PlayerPosition.MC),
                "Test Team",
                5_000_000L,
                league);

        org.springframework.test.util.ReflectionTestUtils.setField(
                player,
                "id",
                100L);

        player.updateCompetitionData(
                "Test Player",
                "test-player",
                List.of(PlayerPosition.MC),
                42,
                "Test Team",
                10L,
                5_000_000L,
                PlayerStatus.OK,
                50_000L);

        player.updateOwnership(
                manager,
                LocalDateTime.of(
                        2026,
                        8,
                        20,
                        12,
                        0),
                4_000_000L,
                6_000_000L,
                null);

        return player;
    }
}