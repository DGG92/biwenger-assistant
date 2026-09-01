package com.artajerjes.biwengerassistant.biwenger.dto.playerdetail;

import java.util.List;

public record BiwengerPlayerDetailData(
        Long id,
        String name,
        String slug,
        List<List<Long>> prices,
        List<BiwengerPlayerReport> reports,
        List<BiwengerPlayerSeason> seasons) {
}