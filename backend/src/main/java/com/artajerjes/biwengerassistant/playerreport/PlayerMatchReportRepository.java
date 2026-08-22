package com.artajerjes.biwengerassistant.playerreport;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerMatchReportRepository
                extends JpaRepository<PlayerMatchReport, Long> {

        @Query("""
                        SELECT report
                        FROM PlayerMatchReport report
                        WHERE report.player.id = :playerId
                          AND report.biwengerMatchId = :biwengerMatchId
                        """)
        Optional<PlayerMatchReport> findByPlayerIdAndBiwengerMatchId(
                        @Param("playerId") Long playerId,
                        @Param("biwengerMatchId") Long biwengerMatchId);

        Optional<PlayerMatchReport> findByPlayer_BiwengerPlayerIdAndBiwengerRoundId(
                        String biwengerPlayerId,
                        Long biwengerRoundId);

        List<PlayerMatchReport> findTop2ByPlayer_IdOrderByMatchDateDesc(
                        Long playerId);
}