package com.artajerjes.biwengerassistant.movement.dto;

public record MovementSyncResponse(
        int processed,
        int created,
        int duplicated,
        int playersNotFound,
        int managersNotFound) {
}