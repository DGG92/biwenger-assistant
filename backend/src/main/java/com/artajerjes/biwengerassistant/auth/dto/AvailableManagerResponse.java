package com.artajerjes.biwengerassistant.auth.dto;

public record AvailableManagerResponse(
        Long id,
        String name,
        String icon,
        Long leagueId) {
}