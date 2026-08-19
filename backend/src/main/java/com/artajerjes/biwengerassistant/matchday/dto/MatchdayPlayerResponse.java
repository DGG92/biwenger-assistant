package com.artajerjes.biwengerassistant.matchday.dto;

import com.artajerjes.biwengerassistant.player.PlayerPosition;

public record MatchdayPlayerResponse(
        Long biwengerPlayerId,
        String name,
        String teamName,
        Long teamId,
        Long gameId,
        Integer gameRoundPart,
        Integer lineupIndex,
        PlayerPosition lineupPosition,
        boolean starter,
        boolean reserve,
        boolean discarded,
        boolean captain,
        boolean ram,
        boolean coach,
        MatchdayGameStatus gameStatus,
        boolean locked,
        boolean modifiable,
        Integer points) {
}