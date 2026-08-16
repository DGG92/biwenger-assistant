package com.artajerjes.biwengerassistant.offer.dto;

public record OfferPlayerResponse(
        Long id,
        String name,
        Long marketValue,
        Long purchasePrice) {
}