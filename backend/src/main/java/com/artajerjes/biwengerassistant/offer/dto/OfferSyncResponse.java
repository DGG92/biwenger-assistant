package com.artajerjes.biwengerassistant.offer.dto;

public record OfferSyncResponse(
        int total,
        int created,
        int updated,
        int playersNotFound,
        int managersNotFound) {
}