package com.artajerjes.biwengerassistant.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLeagueRequest(

        @NotBlank(message = "League name is required") @Size(max = 100, message = "League name cannot exceed 100 characters") String name,

        @Size(max = 255, message = "Biwenger league ID cannot exceed 255 characters") String biwengerLeagueId) {
}