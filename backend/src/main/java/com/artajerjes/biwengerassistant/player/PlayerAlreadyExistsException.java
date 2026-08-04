package com.artajerjes.biwengerassistant.player;

public class PlayerAlreadyExistsException extends RuntimeException {

    public PlayerAlreadyExistsException(
            String biwengerPlayerId,
            Long leagueId
    ) {
        super(
                "A player with Biwenger ID '"
                + biwengerPlayerId
                + "' already exists in league '"
                + leagueId
                + "'"
        );
    }
}