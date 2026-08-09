package com.artajerjes.biwengerassistant.biwenger.dto.home;

import java.util.List;

public record BiwengerHomeLeague(
        Long id,
        String name,
        List<BiwengerBoardEvent> board) {
}