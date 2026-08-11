package com.artajerjes.biwengerassistant.biwenger.dto.playerdetail;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BiwengerPlayerRound(
        Long id,
        String name,
        @JsonProperty("short") String shortName,
        Integer part) {
}