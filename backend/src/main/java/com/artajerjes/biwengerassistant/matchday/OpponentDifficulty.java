package com.artajerjes.biwengerassistant.matchday;

public record OpponentDifficulty(
        double overallDifficulty,
        double attackingStrength,
        double defensiveStrength,
        MatchdayVenue venue) {
}