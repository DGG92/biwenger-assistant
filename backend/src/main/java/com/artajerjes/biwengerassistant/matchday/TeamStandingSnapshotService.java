package com.artajerjes.biwengerassistant.matchday;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsCompetitionStandings;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsStanding;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;

@Service
public class TeamStandingSnapshotService {

    private final TeamStandingSnapshotRepository teamStandingSnapshotRepository;
    private final LeagueRepository leagueRepository;
    private final BiwengerClient biwengerClient;

    public TeamStandingSnapshotService(
            TeamStandingSnapshotRepository teamStandingSnapshotRepository,
            LeagueRepository leagueRepository,
            BiwengerClient biwengerClient) {

        this.teamStandingSnapshotRepository = teamStandingSnapshotRepository;
        this.leagueRepository = leagueRepository;
        this.biwengerClient = biwengerClient;
    }

    @Transactional
    public int syncCurrentMatchday(Long leagueId) {

        BiwengerRoundsResponse response = biwengerClient.getRounds();

        return syncCurrentMatchday(
                leagueId,
                response);
    }

    @Transactional
    public int syncCurrentMatchday(
            Long leagueId,
            BiwengerRoundsResponse response) {

        League league = leagueRepository.findById(leagueId)
                .orElseThrow(
                        () -> new LeagueNotFoundException(leagueId));

        if (response == null
                || response.data() == null
                || response.data().id() == null) {

            throw new IllegalStateException(
                    "Invalid Biwenger rounds response");
        }

        Long roundId = response.data().id();

        if (response.data().competition() == null
                || response.data().competition().standings() == null
                || response.data().competition().standings().isEmpty()) {

            return 0;
        }

        int synced = 0;

        for (BiwengerRoundsCompetitionStandings standings : response.data().competition().standings()) {

            if (standings == null
                    || standings.teams() == null) {

                continue;
            }

            synced += syncStandings(
                    league,
                    roundId,
                    standings.teams());
        }

        return synced;
    }

    private int syncStandings(
            League league,
            Long roundId,
            List<BiwengerRoundsStanding> standings) {

        int synced = 0;

        for (BiwengerRoundsStanding standing : standings) {

            if (standing == null
                    || standing.team() == null
                    || standing.team().id() == null
                    || standing.team().name() == null) {

                continue;
            }

            TeamStandingSnapshot snapshot = teamStandingSnapshotRepository
                    .findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
                            league.getId(),
                            roundId,
                            standing.team().id())
                    .orElse(null);

            if (snapshot == null) {

                snapshot = new TeamStandingSnapshot(
                        league,
                        roundId,
                        standing.team().id(),
                        standing.team().name(),
                        standing.position(),
                        standing.points(),
                        standing.won(),
                        standing.lost(),
                        standing.tied(),
                        standing.scored(),
                        standing.against());

            } else {

                snapshot.update(
                        standing.team().name(),
                        standing.position(),
                        standing.points(),
                        standing.won(),
                        standing.lost(),
                        standing.tied(),
                        standing.scored(),
                        standing.against());
            }

            teamStandingSnapshotRepository.save(snapshot);

            synced++;
        }

        return synced;
    }
}