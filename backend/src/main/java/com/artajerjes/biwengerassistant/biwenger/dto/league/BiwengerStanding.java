package com.artajerjes.biwengerassistant.biwenger.dto.league;

import java.util.List;

public record BiwengerStanding(
        Long id,
        String name,
        String icon,
        Integer points,
        List<Integer> lastPositions,
        Integer teamSize,
        Long teamValue,
        Long teamValueInc,
        Integer position,
        String role
) {
}