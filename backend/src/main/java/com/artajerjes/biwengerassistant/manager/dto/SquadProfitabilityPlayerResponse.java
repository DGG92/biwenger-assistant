package com.artajerjes.biwengerassistant.manager.dto;

public record SquadProfitabilityPlayerResponse(
                Long playerId,
                String biwengerPlayerId,
                String name,
                Long currentValue,
                Long purchasePrice,
                Long unrealizedProfit,
                Double unrealizedProfitPercent,
                Integer points,
                Double pointsPerMillion) {
}