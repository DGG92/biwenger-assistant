package com.artajerjes.biwengerassistant.biwenger.dto.market;

public record BiwengerMarketLastBid(
        String type,
        Long amount,
        String status,
        BiwengerMarketUser from,
        BiwengerMarketUser to) {
}