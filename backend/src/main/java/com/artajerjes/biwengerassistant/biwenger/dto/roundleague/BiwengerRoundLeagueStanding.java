package com.artajerjes.biwengerassistant.biwenger.dto.roundleague;

public record BiwengerRoundLeagueStanding(
        Long id,
        String name,
        String icon,
        Integer points,
        Long teamValue,
        Long teamValueInc,
        Integer position,
        BiwengerRoundLeagueLineup lineup) {
}