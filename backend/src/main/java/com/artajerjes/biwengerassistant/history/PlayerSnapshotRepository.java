package com.artajerjes.biwengerassistant.history;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerSnapshotRepository
        extends JpaRepository<PlayerSnapshot, Long> {

    Optional<PlayerSnapshot> findByPlayerIdAndSnapshotDate(
            Long playerId,
            LocalDate snapshotDate);

    List<PlayerSnapshot> findAllByPlayerIdOrderBySnapshotDateAsc(
            Long playerId);

    List<PlayerSnapshot> findAllByLeagueIdAndSnapshotDateOrderByPlayerId(
            Long leagueId,
            LocalDate snapshotDate);
}