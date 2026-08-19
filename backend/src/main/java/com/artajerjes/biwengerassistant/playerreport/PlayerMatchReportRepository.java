package com.artajerjes.biwengerassistant.playerreport;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerMatchReportRepository
                extends JpaRepository<PlayerMatchReport, Long> {

        Optional<PlayerMatchReport> findByPlayer_IdAndBiwengerMatchId(
                        Long playerId,
                        Long biwengerMatchId);

        Optional<PlayerMatchReport> findByPlayer_BiwengerPlayerIdAndBiwengerRoundId(
                        String biwengerPlayerId,
                        Long biwengerRoundId);

        List<PlayerMatchReport> findTop2ByPlayer_IdOrderByMatchDateDesc(
                        Long playerId);
}