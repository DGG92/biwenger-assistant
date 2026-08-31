package com.artajerjes.biwengerassistant.matchday;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchdayOpponentContextService {

    private final MatchdayGameRepository matchdayGameRepository;
    private final TeamStandingSnapshotRepository teamStandingSnapshotRepository;

    public MatchdayOpponentContextService(
            MatchdayGameRepository matchdayGameRepository,
            TeamStandingSnapshotRepository teamStandingSnapshotRepository) {

        this.matchdayGameRepository = matchdayGameRepository;
        this.teamStandingSnapshotRepository = teamStandingSnapshotRepository;
    }

    @Transactional(readOnly = true)
    public Optional<MatchdayOpponentContext> resolve(
            Long leagueId,
            Long roundId,
            Long teamId) {

        if (leagueId == null
                || roundId == null
                || teamId == null) {

            return Optional.empty();
        }

        List<MatchdayGame> games = matchdayGameRepository.findByLeagueIdAndBiwengerRoundId(
                leagueId,
                roundId);

        if (games == null || games.isEmpty()) {
            return Optional.empty();
        }

        MatchdayGame game = games.stream()
                .filter(candidate -> belongsToTeam(
                        candidate,
                        teamId))
                .findFirst()
                .orElse(null);

        if (game == null) {
            return Optional.empty();
        }

        boolean home = teamId.equals(
                game.getHomeTeamId());

        Long opponentTeamId = home
                ? game.getAwayTeamId()
                : game.getHomeTeamId();

        String opponentTeamName = home
                ? game.getAwayTeamName()
                : game.getHomeTeamName();

        TeamStandingSnapshot opponentStanding = opponentTeamId == null
                ? null
                : teamStandingSnapshotRepository
                        .findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
                                leagueId,
                                roundId,
                                opponentTeamId)
                        .orElse(null);

        return Optional.of(
                new MatchdayOpponentContext(
                        roundId,
                        game.getBiwengerGameId(),
                        teamId,
                        opponentTeamId,
                        opponentTeamName,
                        home
                                ? MatchdayVenue.HOME
                                : MatchdayVenue.AWAY,
                        game.getStatus(),
                        opponentStanding == null
                                ? null
                                : opponentStanding.getPosition(),
                        opponentStanding == null
                                ? null
                                : opponentStanding.getPoints(),
                        opponentStanding == null
                                ? null
                                : opponentStanding.getScored(),
                        opponentStanding == null
                                ? null
                                : opponentStanding.getAgainst()));
    }

    private boolean belongsToTeam(
            MatchdayGame game,
            Long teamId) {

        if (game == null) {
            return false;
        }

        return teamId.equals(game.getHomeTeamId())
                || teamId.equals(game.getAwayTeamId());
    }
}