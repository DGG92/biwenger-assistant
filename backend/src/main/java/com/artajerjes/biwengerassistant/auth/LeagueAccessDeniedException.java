package com.artajerjes.biwengerassistant.auth;

public class LeagueAccessDeniedException extends RuntimeException {

    public LeagueAccessDeniedException(Long leagueId) {
        super("Authenticated user cannot access league " + leagueId);
    }
}