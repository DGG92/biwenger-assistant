package com.artajerjes.biwengerassistant.manager.dto;

public record SquadProfitabilityResponse(
        Long managerId,
        String managerName,

        Integer players,
        Integer playersWithPurchasePrice,

        Long currentSquadValue,
        Long analyzedSquadValue,
        Long totalInvestment,

        Long unrealizedProfit,
        Double unrealizedProfitPercent,

        Integer profitablePlayers,
        Integer losingPlayers,
        Integer breakEvenPlayers,

        SquadProfitabilityPlayerResponse bestInvestment,
        SquadProfitabilityPlayerResponse worstInvestment,
        SquadProfitabilityPlayerResponse mostEfficientPlayer) {
}