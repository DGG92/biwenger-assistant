package com.artajerjes.biwengerassistant.matchday.dto;

import java.util.List;

public record MatchdayResponse(
        Long roundId,
        String roundName,
        String roundShortName,
        Integer roundPart,
        String roundStatus,
        String formation,
        String splitRound,
        String lineupRoundChangesIn,
        List<MatchdayPlayerResponse> players) {
}