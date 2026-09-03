package com.artajerjes.biwengerassistant.history;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerPriceHistoryRepository
                extends JpaRepository<PlayerPriceHistory, Long> {

        Optional<PlayerPriceHistory> findByPlayerIdAndPriceDate(
                        Long playerId,
                        LocalDate priceDate);

        List<PlayerPriceHistory> findAllByPlayerIdOrderByPriceDateAsc(
                        Long playerId);

        Optional<PlayerPriceHistory> findTopByPlayerIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                        Long playerId,
                        LocalDate priceDate);

        List<PlayerPriceHistory> findAllByLeagueIdAndPriceDateOrderByPlayerId(
                        Long leagueId,
                        LocalDate priceDate);

        @Query("""
                        SELECT MAX(p.priceDate)
                        FROM PlayerPriceHistory p
                        WHERE p.playerId = :playerId
                        """)
        Optional<LocalDate> findLatestPriceDateByPlayerId(
                        @Param("playerId") Long playerId);

        @Query("""
                        SELECT p.playerId, MAX(p.priceDate)
                        FROM PlayerPriceHistory p
                        WHERE p.leagueId = :leagueId
                        GROUP BY p.playerId
                        """)
        List<Object[]> findLatestPriceDatesByLeagueId(
                        @Param("leagueId") Long leagueId);

        @Query("""
                        SELECT DISTINCT p.playerId
                        FROM PlayerPriceHistory p
                        WHERE p.leagueId = :leagueId
                        """)
        List<Long> findPlayerIdsWithHistoryByLeagueId(
                        @Param("leagueId") Long leagueId);

        @Query("""
                        SELECT p
                        FROM PlayerPriceHistory p
                        WHERE p.leagueId = :leagueId
                        ORDER BY p.playerId ASC, p.priceDate ASC
                        """)
        List<PlayerPriceHistory> findAllByLeagueIdOrderByPlayerAndPriceDate(
                        @Param("leagueId") Long leagueId);
}