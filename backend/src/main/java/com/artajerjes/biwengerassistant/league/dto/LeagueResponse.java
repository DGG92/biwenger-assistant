package com.artajerjes.biwengerassistant.league.dto;

import java.time.LocalDateTime;

public record LeagueResponse(
        Long id,
        String name,
        String biwengerLeagueId,
        LocalDateTime createdAt
) {
}