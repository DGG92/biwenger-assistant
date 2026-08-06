package com.artajerjes.biwengerassistant.manager.dto;

import java.time.LocalDateTime;

public record ManagerResponse(
                Long id,
                Long biwengerManagerId,
                String name,
                String icon,
                Integer points,
                Integer teamSize,
                Long teamValue,
                Long teamValueInc,
                Integer position,
                String role,
                Boolean leagueAdministrator,
                Long cash,
                Long leagueId,
                LocalDateTime createdAt) {
}