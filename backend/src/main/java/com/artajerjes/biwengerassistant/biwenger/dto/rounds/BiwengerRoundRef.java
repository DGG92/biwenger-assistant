package com.artajerjes.biwengerassistant.biwenger.dto.rounds;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BiwengerRoundRef(
        Long id,
        String name,
        @JsonProperty("short") String shortName,
        Integer part) {
}