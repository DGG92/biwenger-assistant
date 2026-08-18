package com.artajerjes.biwengerassistant.biwenger.dto.rounds;

public record BiwengerRoundGame(
        Long id,
        Long date,
        String status,
        BiwengerRoundTeam home,
        BiwengerRoundTeam away,
        BiwengerRoundRef round) {
}