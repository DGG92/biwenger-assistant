package com.artajerjes.biwengerassistant.biwenger.dto.roundleague;

import java.util.List;

public record BiwengerRoundLeagueLeague(
        Long id,
        String name,
        String competition,
        String mode,
        String type,
        String marketMode,
        Integer scoreID,
        List<BiwengerRoundLeagueStanding> standings,
        BiwengerRoundLeagueSettings settings) {
}