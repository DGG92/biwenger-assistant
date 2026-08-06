package com.artajerjes.biwengerassistant.biwenger.dto.competition;

import java.util.Map;

public record BiwengerCompetitionData(
                Long id,
                String name,
                String slug,
                String sport,
                String currency,
                Map<String, BiwengerCompetitionPlayer> players,
                Map<String, BiwengerCompetitionTeam> teams) {
}