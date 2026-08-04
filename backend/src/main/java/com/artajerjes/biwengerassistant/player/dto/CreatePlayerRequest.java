package com.artajerjes.biwengerassistant.player.dto;

import java.util.List;

import com.artajerjes.biwengerassistant.player.PlayerPosition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CreatePlayerRequest(

        @NotBlank(message = "Biwenger player ID is required")
        @Size(
                max = 255,
                message = "Biwenger player ID cannot exceed 255 characters"
        )
        String biwengerPlayerId,

        @NotBlank(message = "Player name is required")
        @Size(
                max = 100,
                message = "Player name cannot exceed 100 characters"
        )
        String name,

        @NotEmpty(message = "Player must have at least one position")
        List<PlayerPosition> positions,

        @Size(
                max = 100,
                message = "Team name cannot exceed 100 characters"
        )
        String teamName,

        @NotNull(message = "Market value is required")
        @PositiveOrZero(message = "Market value cannot be negative")
        Long marketValue
) {
}