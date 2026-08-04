package com.artajerjes.biwengerassistant.player;

public class PlayerNotFoundException extends RuntimeException {

    public PlayerNotFoundException(
            Long playerId,
            Long leagueId
    ) {
        super(
                "Player with ID '"
                + playerId
                + "' was not found in league '"
                + leagueId
                + "'"
        );
    }
}