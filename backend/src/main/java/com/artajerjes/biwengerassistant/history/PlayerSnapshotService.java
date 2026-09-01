package com.artajerjes.biwengerassistant.history;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@Service
public class PlayerSnapshotService {

    private final PlayerRepository playerRepository;
    private final PlayerSnapshotRepository playerSnapshotRepository;

    public PlayerSnapshotService(
            PlayerRepository playerRepository,
            PlayerSnapshotRepository playerSnapshotRepository) {

        this.playerRepository = playerRepository;
        this.playerSnapshotRepository = playerSnapshotRepository;
    }

    @Transactional
    public int captureDailySnapshots(
            Long leagueId) {

        LocalDateTime capturedAt = LocalDateTime.now();

        return captureDailySnapshots(
                leagueId,
                capturedAt);
    }

    int captureDailySnapshots(
            Long leagueId,
            LocalDateTime capturedAt) {

        LocalDate snapshotDate = capturedAt.toLocalDate();

        List<Player> players = playerRepository
                .findAllByLeague_Id(leagueId);

        for (Player player : players) {

            PlayerSnapshot snapshot = playerSnapshotRepository
                    .findByPlayerIdAndSnapshotDate(
                            player.getId(),
                            snapshotDate)
                    .orElseGet(
                            () -> new PlayerSnapshot(
                                    player,
                                    snapshotDate,
                                    capturedAt));

            snapshot.updateFrom(
                    player,
                    capturedAt);

            playerSnapshotRepository.save(snapshot);
        }

        return players.size();
    }
}