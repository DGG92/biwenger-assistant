package com.artajerjes.biwengerassistant.player;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    boolean existsByBiwengerPlayerIdAndLeague_Id(
            String biwengerPlayerId,
            Long leagueId
    );

    boolean existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
            String biwengerPlayerId,
            Long leagueId,
            Long playerId
    );

    List<Player> findAllByLeague_Id(Long leagueId);

    Optional<Player> findByIdAndLeague_Id(
            Long playerId,
            Long leagueId
    );
    Optional<Player> findByBiwengerPlayerIdAndLeague_Id(
        String biwengerPlayerId,
        Long leagueId
    );
}