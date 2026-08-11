package com.artajerjes.biwengerassistant.player.dto;

import java.util.List;

public record PlayerProtectionAlert(
        PlayerProtectionAlertLevel level,
        int score,
        List<PlayerProtectionReason> reasons) {
}