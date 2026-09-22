package com.artajerjes.biwengerassistant.credential.dto;

import jakarta.validation.constraints.NotBlank;

public record SaveBiwengerCredentialRequest(
        @NotBlank String token) {
}