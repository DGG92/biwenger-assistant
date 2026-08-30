package com.artajerjes.biwengerassistant.biwenger.dto.rounds;

public record BiwengerRoundsStanding(
        Integer position,
        BiwengerRoundsCompetitionTeam team,
        Integer points,
        Integer won,
        Integer lost,
        Integer tied,
        Integer scored,
        Integer against) {
}