package com.artajerjes.biwengerassistant.movement;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementRepository
        extends JpaRepository<Movement, Long> {

    List<Movement> findAllByLeague_IdOrderByOccurredAtDesc(
            Long leagueId);

    boolean existsByLeague_IdAndPlayer_IdAndTypeAndAmountAndOccurredAtAndFromManager_IdAndToManager_Id(
            Long leagueId,
            Long playerId,
            MovementType type,
            Long amount,
            LocalDateTime occurredAt,
            Long fromManagerId,
            Long toManagerId);

    boolean existsByExternalKey(String externalKey);
}