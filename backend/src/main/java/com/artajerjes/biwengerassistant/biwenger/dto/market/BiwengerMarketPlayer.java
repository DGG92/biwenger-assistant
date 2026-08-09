package com.artajerjes.biwengerassistant.biwenger.dto.market;

import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerPlayerOwner;

public record BiwengerMarketPlayer(
        Long id,
        BiwengerPlayerOwner owner) {
}