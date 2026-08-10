package com.artajerjes.biwengerassistant.biwenger.dto.market;

import java.util.List;

public record BiwengerMarketOffer(
        Long id,
        Long amount,
        Long created,
        Long until,
        String status,
        String type,
        BiwengerMarketUser from,
        BiwengerMarketUser to,
        List<Long> requestedPlayers) {
}