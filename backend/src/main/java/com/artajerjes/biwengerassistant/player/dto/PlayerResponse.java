package com.artajerjes.biwengerassistant.player.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.artajerjes.biwengerassistant.player.PlayerPosition;

public record PlayerResponse(
        Long id,
        String biwengerPlayerId,
        String name,
        List<PlayerPosition> positions,
        Integer points,
        String teamName,
        Long marketValue,
        Boolean injured,
        Boolean captain,
        Boolean ram,
        Long valueFluctuation,
        Boolean blockedClause,
        Long clauseValue,
        String ownerName,
        Boolean freePlayer,
        LocalDateTime signedAt,
        Long leagueId,
        LocalDateTime createdAt
) {
}