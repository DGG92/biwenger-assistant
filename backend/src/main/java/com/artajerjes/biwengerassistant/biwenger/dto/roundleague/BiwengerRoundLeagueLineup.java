package com.artajerjes.biwengerassistant.biwenger.dto.roundleague;

import java.util.List;

import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerLineupPlayerRef;

public record BiwengerRoundLeagueLineup(
        String type,
        BiwengerLineupPlayerRef captain,
        BiwengerLineupPlayerRef striker,
        BiwengerLineupPlayerRef coach,
        Long date,
        List<Long> players,
        List<Long> reserves,
        List<Long> discarded,
        Boolean count) {
}