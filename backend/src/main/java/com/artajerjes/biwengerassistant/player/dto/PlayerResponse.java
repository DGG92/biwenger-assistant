package com.artajerjes.biwengerassistant.player.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerStatus;

public record PlayerResponse(
                Long id,
                String biwengerPlayerId,
                String name,
                List<PlayerPosition> positions,
                Integer points,
                String teamName,
                Long marketValue,
                Long purchasePrice,
                Long profitability,
                Boolean injured,
                PlayerStatus status,
                Boolean captain,
                Boolean ram,
                Boolean coach,
                Boolean starter,
                Boolean reserve,
                PlayerPosition lineupPosition,
                PlayerPosition benchPosition,
                Long valueFluctuation,
                Boolean blockedClause,
                LocalDateTime clauseLockedUntil,
                Long clauseValue,
                Long ownerId,
                String ownerName,
                Boolean freePlayer,
                LocalDateTime signedAt,
                Long leagueId,
                LocalDateTime createdAt,
                PlayerProtectionAlert playerProtectionAlert) {
}