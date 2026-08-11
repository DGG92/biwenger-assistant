package com.artajerjes.biwengerassistant.biwenger.dto.playerdetail;

public record BiwengerPlayerMatch(
        Long id,
        Long date,
        String status,
        BiwengerPlayerRound round) {
}