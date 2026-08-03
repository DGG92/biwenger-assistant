package com.artajerjes.biwengerassistant.league;

public class LeagueAlreadyExistsException extends RuntimeException {

    public LeagueAlreadyExistsException(String biwengerLeagueId) {
        super("A league with Biwenger ID '" + biwengerLeagueId + "' already exists");
    }
}