package com.artajerjes.biwengerassistant.player.dto;

public record PlayerSyncResponse(
        int total,
        int created,
        int updated,
        int skipped
) {
}