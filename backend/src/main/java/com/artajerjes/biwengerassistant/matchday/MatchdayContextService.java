package com.artajerjes.biwengerassistant.matchday;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueSettings;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;

@Service
public class MatchdayContextService {

    private final MatchdayContextRepository matchdayContextRepository;
    private final LeagueRepository leagueRepository;
    private final BiwengerClient biwengerClient;

    public MatchdayContextService(
            MatchdayContextRepository matchdayContextRepository,
            LeagueRepository leagueRepository,
            BiwengerClient biwengerClient) {

        this.matchdayContextRepository = matchdayContextRepository;
        this.leagueRepository = leagueRepository;
        this.biwengerClient = biwengerClient;
    }

    @Transactional
    public MatchdayContext syncCurrentMatchday(Long leagueId) {

        League league = leagueRepository.findById(leagueId)
                .orElseThrow(
                        () -> new LeagueNotFoundException(leagueId));

        BiwengerRoundLeagueResponse response = biwengerClient.getRoundLeague();

        if (response == null
                || response.data() == null
                || response.data().round() == null
                || response.data().league() == null) {

            throw new IllegalStateException(
                    "Invalid Biwenger round league response");
        }

        Long roundId = response.data().round().id();

        if (roundId == null) {
            throw new IllegalStateException(
                    "Biwenger current round has no id");
        }

        BiwengerRoundLeagueSettings settings = response.data().league().settings();

        MatchdayContext context = matchdayContextRepository
                .findByLeagueIdAndBiwengerRoundId(
                        leagueId,
                        roundId)
                .orElse(null);

        String splitRound = settings != null ? settings.splitRound() : null;

        String lineupShow = settings != null ? settings.lineupShow() : null;

        Integer lineupRoundChanges = settings != null ? settings.lineupRoundChanges() : null;

        String lineupRoundChangesIn = settings != null ? settings.lineupRoundChangesIn() : null;

        Boolean lineupRoundChangeStrategy = settings != null
                ? settings.lineupRoundChangeStrategy()
                : null;

        if (context == null) {

            context = new MatchdayContext(
                    league,
                    roundId,
                    splitRound,
                    lineupShow,
                    lineupRoundChanges,
                    lineupRoundChangesIn,
                    lineupRoundChangeStrategy);

        } else {

            context.update(
                    splitRound,
                    lineupShow,
                    lineupRoundChanges,
                    lineupRoundChangesIn,
                    lineupRoundChangeStrategy);
        }

        return matchdayContextRepository.save(context);
    }
}