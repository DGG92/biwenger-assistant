package com.artajerjes.biwengerassistant.biwenger.dto.home;

import tools.jackson.databind.JsonNode;

public record BiwengerBoardEvent(
        String type,
        JsonNode content,
        Long date) {
}