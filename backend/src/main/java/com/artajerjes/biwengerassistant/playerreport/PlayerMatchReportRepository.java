package com.artajerjes.biwengerassistant.playerreport;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerMatchReportRepository
        extends JpaRepository<PlayerMatchReport, Long> {

    Optional<PlayerMatchReport> findByPlayer_IdAndBiwengerMatchId(
            Long playerId,
            Long biwengerMatchId);

    List<PlayerMatchReport> findTop2ByPlayer_IdOrderByMatchDateDesc(
            Long playerId);
}