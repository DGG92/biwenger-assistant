package com.artajerjes.biwengerassistant.biwenger.dto.user;

import java.util.List;

public record BiwengerUserData(
        Long id,
        String name,
        List<BiwengerUserPlayer> players) {
}