package com.artajerjes.biwengerassistant.player.dto;

import java.util.List;

import com.artajerjes.biwengerassistant.player.PlayerPosition;

public record LeaguePlayerStatisticsResponse(
        Long playerId,
        String name,
        List<PlayerPosition> positions,
        Long marketValue,
        Integer totalPoints,
        Integer matchesPlayed,
        Double averagePoints,
        Double pointsPerMillion) {
}