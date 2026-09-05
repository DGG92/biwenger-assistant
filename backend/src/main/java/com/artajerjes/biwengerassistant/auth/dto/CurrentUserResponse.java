package com.artajerjes.biwengerassistant.auth.dto;

import com.artajerjes.biwengerassistant.auth.AssistantRole;

public record CurrentUserResponse(
        Long id,
        String username,
        AssistantRole role,
        Long managerId,
        Long leagueId) {
}
