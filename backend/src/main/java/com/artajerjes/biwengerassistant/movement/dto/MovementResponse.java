package com.artajerjes.biwengerassistant.movement.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.artajerjes.biwengerassistant.movement.MovementType;

public record MovementResponse(
        Long id,
        MovementType type,
        Long playerId,
        String biwengerPlayerId,
        String playerName,
        Long fromManagerId,
        String fromManagerName,
        Long toManagerId,
        String toManagerName,
        Long amount,
        Integer rounds,
        LocalDateTime occurredAt,
        List<MovementBidResponse> bids) {
}