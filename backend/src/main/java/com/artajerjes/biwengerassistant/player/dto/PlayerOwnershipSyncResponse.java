package com.artajerjes.biwengerassistant.player.dto;

public record PlayerOwnershipSyncResponse(
        int managers,
        int playersAssigned,
        int playersNotFound) {
}