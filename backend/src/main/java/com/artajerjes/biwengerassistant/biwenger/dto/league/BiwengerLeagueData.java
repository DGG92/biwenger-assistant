package com.artajerjes.biwengerassistant.biwenger.dto.league;

import java.util.List;

public record BiwengerLeagueData(
        Long id,
        String name,
        String type,
        String mode,
        String marketMode,
        Integer scoreID,
        String icon,
        String cover,
        Long created,
        String competition,
        List<BiwengerStanding> standings,
        BiwengerLeagueSettings settings) {
}