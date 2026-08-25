package com.artajerjes.biwengerassistant.recommendation.action;

import java.util.List;

public record ActionCandidate(
        ActionType type,
        ActionPriority priority,
        Long playerId,
        String playerName,
        String title,
        String explanation,
        Integer confidence,
        Long suggestedAmount,
        List<String> sourceSignals) {
}