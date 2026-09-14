package com.artajerjes.biwengerassistant.player;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlayerRepository extends JpaRepository<Player, Long> {

        boolean existsByBiwengerPlayerIdAndLeague_Id(
                        String biwengerPlayerId,
                        Long leagueId);

        boolean existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                        String biwengerPlayerId,
                        Long leagueId,
                        Long playerId);

        List<Player> findAllByLeague_Id(Long leagueId);

        @EntityGraph(attributePaths = "positions")
        @Query("""
                        SELECT p
                        FROM Player p
                        WHERE p.league.id = :leagueId
                        """)
        List<Player> findAllWithPositionsByLeagueId(
                        @Param("leagueId") Long leagueId);

        List<Player> findAllByOwner_IdAndLeague_Id(
                        Long ownerId,
                        Long leagueId);

        Optional<Player> findByIdAndLeague_Id(
                        Long playerId,
                        Long leagueId);

        Optional<Player> findByBiwengerPlayerIdAndLeague_Id(
                        String biwengerPlayerId,
                        Long leagueId);
}