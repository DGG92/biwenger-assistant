package com.artajerjes.biwengerassistant.playerreport;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

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

    Optional<PlayerMatchReport> findTopByPlayer_IdOrderByMatchDateDesc(
            Long playerId);

    Optional<PlayerMatchReport> findByPlayer_BiwengerPlayerIdAndBiwengerRoundId(
            String biwengerPlayerId,
            Long biwengerRoundId);

    List<PlayerMatchReport> findTop2ByPlayer_IdOrderByMatchDateDesc(
            Long playerId);

    List<PlayerMatchReport> findTop5ByPlayer_IdOrderByMatchDateDesc(
            Long playerId);

    @Query(value = """
            WITH ranked AS (
                SELECT r.id,
                       ROW_NUMBER() OVER (
                           PARTITION BY r.player_id
                           ORDER BY r.match_date DESC
                       ) AS rn
                FROM player_match_reports r
                WHERE r.player_id IN (:playerIds)
            )
            SELECT r.*
            FROM player_match_reports r
            JOIN ranked x ON x.id = r.id
            WHERE x.rn <= 5
            ORDER BY r.player_id, r.match_date DESC
            """, nativeQuery = true)
    List<PlayerMatchReport> findTop5ReportsByPlayerIds(
            @Param("playerIds") List<Long> playerIds);

    List<PlayerMatchReport> findTop10ByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
            Long playerId);

    List<PlayerMatchReport> findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
            Long playerId);

    @Query("""
            SELECT r.player.id, MAX(r.matchDate)
            FROM PlayerMatchReport r
            WHERE r.player.league.id = :leagueId
            GROUP BY r.player.id
            """)
    List<Object[]> findLatestReportDateByPlayer(
            @Param("leagueId") Long leagueId);

    @Query("""
            SELECT r
            FROM PlayerMatchReport r
            JOIN FETCH r.player p
            WHERE p.league.id = :leagueId
              AND r.participated = true
              AND r.points IS NOT NULL
            ORDER BY r.matchDate DESC
            """)
    List<PlayerMatchReport> findAllScoredReportsByLeague(
            @Param("leagueId") Long leagueId);

    @Query("""
            SELECT r.season
            FROM PlayerMatchReport r
            JOIN r.player p
            WHERE p.league.id = :leagueId
              AND r.participated = true
              AND r.points IS NOT NULL
              AND r.season IS NOT NULL
              AND TRIM(r.season) <> ''
            ORDER BY r.matchDate DESC
            """)
    List<String> findLatestScoredSeasonByLeague(
            @Param("leagueId") Long leagueId,
            Pageable pageable);

    @Query("""
            SELECT r
            FROM PlayerMatchReport r
            JOIN FETCH r.player p
            WHERE p.league.id = :leagueId
            AND r.participated = true
            AND r.points IS NOT NULL
            AND r.season = :season
            ORDER BY r.matchDate DESC
            """)
    List<PlayerMatchReport> findAllScoredReportsByLeagueAndSeason(
            @Param("leagueId") Long leagueId,
            @Param("season") String season);
}