package com.artajerjes.biwengerassistant.league;

public class LeagueNotFoundException extends RuntimeException {

    public LeagueNotFoundException(Long id) {
        super("League with ID '" + id + "' was not found");
    }
}