package com.artajerjes.biwengerassistant.player.dto;

public record PlayerLineupSyncResponse(
        Long managerId,
        String formation,
        Long captainPlayerId,
        Long ramPlayerId,
        Long coachPlayerId) {
}