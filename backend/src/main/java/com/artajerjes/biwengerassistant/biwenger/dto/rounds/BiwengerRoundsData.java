package com.artajerjes.biwengerassistant.biwenger.dto.rounds;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BiwengerRoundsData(
        Long id,
        String name,
        @JsonProperty("short") String shortName,
        String status,
        Integer scoreID,
        Integer part,
        List<BiwengerRoundGame> games) {
}