package com.artajerjes.biwengerassistant.matchday;

import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;

@Service
public class MatchdayRoundSyncService {

    private final BiwengerClient biwengerClient;
    private final MatchdayGameService matchdayGameService;
    private final TeamStandingSnapshotService teamStandingSnapshotService;

    public MatchdayRoundSyncService(
            BiwengerClient biwengerClient,
            MatchdayGameService matchdayGameService,
            TeamStandingSnapshotService teamStandingSnapshotService) {

        this.biwengerClient = biwengerClient;
        this.matchdayGameService = matchdayGameService;
        this.teamStandingSnapshotService = teamStandingSnapshotService;
    }

    public MatchdayRoundSyncResult syncCurrentMatchday(Long leagueId) {

        BiwengerRoundsResponse response = biwengerClient.getRounds();

        int games = matchdayGameService.syncCurrentMatchday(
                leagueId,
                response);

        int teamStandings = teamStandingSnapshotService.syncCurrentMatchday(
                leagueId,
                response);

        return new MatchdayRoundSyncResult(
                games,
                teamStandings);
    }
}