package com.artajerjes.biwengerassistant.credential.dto;

import java.time.LocalDateTime;

public record BiwengerCredentialStatusResponse(
        boolean linked,
        Long biwengerUserId,
        LocalDateTime lastValidatedAt) {
}