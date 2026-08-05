package com.artajerjes.biwengerassistant.manager;

public class ManagerNotFoundException extends RuntimeException {

    public ManagerNotFoundException(
            Long managerId,
            Long leagueId
    ) {
        super(
                "Manager with ID '"
                + managerId
                + "' was not found in league '"
                + leagueId
                + "'"
        );
    }
}