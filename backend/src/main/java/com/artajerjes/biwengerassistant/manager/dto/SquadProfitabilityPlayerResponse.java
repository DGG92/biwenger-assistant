package com.artajerjes.biwengerassistant.manager.dto;

public record SquadProfitabilityPlayerResponse(
        Long playerId,
        String name,
        Long currentValue,
        Long purchasePrice,
        Long unrealizedProfit,
        Double unrealizedProfitPercent,
        Integer points,
        Double pointsPerMillion) {
}