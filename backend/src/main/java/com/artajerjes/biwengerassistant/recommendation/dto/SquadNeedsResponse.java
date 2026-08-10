package com.artajerjes.biwengerassistant.recommendation.dto;

import java.util.Map;

public record SquadNeedsResponse(
        Long managerId,
        String managerName,
        int totalPlayers,
        Map<String, Integer> playersByPosition,
        Map<String, Integer> startersByPosition,
        Map<String, Integer> injuredByPosition,
        Map<String, Integer> needScoreByPosition) {
}