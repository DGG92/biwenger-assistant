package com.artajerjes.biwengerassistant.biwenger.dto.competition;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BiwengerCompetitionPlayer(
        Long id,
        String name,
        String slug,

        @JsonProperty("teamID") Long teamId,

        Integer position,
        List<Integer> altPositions,
        Long price,
        Long fantasyPrice,
        Integer number,
        String status,
        Long priceIncrement,
        Integer points,
        String icon,
        String iconHero) {
}