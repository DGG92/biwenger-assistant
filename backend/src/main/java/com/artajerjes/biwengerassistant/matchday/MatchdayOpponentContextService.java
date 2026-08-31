package com.artajerjes.biwengerassistant.matchday;

import java.util.List;
import java.util.Optional;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

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
                                createContext(
                                                roundId,
                                                teamId,
                                                game,
                                                opponentTeamId,
                                                opponentTeamName,
                                                home,
                                                opponentStanding));
        }

        private MatchdayOpponentContext createContext(
                        Long roundId,
                        Long teamId,
                        MatchdayGame game,
                        Long opponentTeamId,
                        String opponentTeamName,
                        boolean home,
                        TeamStandingSnapshot opponentStanding) {

                return new MatchdayOpponentContext(
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
                                                : opponentStanding.getWon(),
                                opponentStanding == null
                                                ? null
                                                : opponentStanding.getLost(),
                                opponentStanding == null
                                                ? null
                                                : opponentStanding.getTied(),
                                opponentStanding == null
                                                ? null
                                                : opponentStanding.getScored(),
                                opponentStanding == null
                                                ? null
                                                : opponentStanding.getAgainst());
        }

        @Transactional(readOnly = true)
        public Map<Long, MatchdayOpponentContext> resolveForTeams(
                        Long leagueId,
                        Long roundId,
                        Collection<Long> teamIds) {

                if (leagueId == null
                                || roundId == null
                                || teamIds == null
                                || teamIds.isEmpty()) {

                        return Map.of();
                }

                List<MatchdayGame> games = matchdayGameRepository.findByLeagueIdAndBiwengerRoundId(
                                leagueId,
                                roundId);

                if (games == null || games.isEmpty()) {
                        return Map.of();
                }

                List<TeamStandingSnapshot> standings = teamStandingSnapshotRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                leagueId,
                                                roundId);

                Map<Long, TeamStandingSnapshot> standingsByTeamId = new HashMap<>();

                if (standings != null) {
                        for (TeamStandingSnapshot standing : standings) {

                                if (standing != null
                                                && standing.getBiwengerTeamId() != null) {

                                        standingsByTeamId.put(
                                                        standing.getBiwengerTeamId(),
                                                        standing);
                                }
                        }
                }

                Map<Long, MatchdayOpponentContext> contexts = new HashMap<>();

                for (Long teamId : teamIds) {

                        if (teamId == null) {
                                continue;
                        }

                        MatchdayGame game = games.stream()
                                        .filter(candidate -> belongsToTeam(candidate, teamId))
                                        .findFirst()
                                        .orElse(null);

                        if (game == null) {
                                continue;
                        }

                        boolean home = teamId.equals(game.getHomeTeamId());

                        Long opponentTeamId = home
                                        ? game.getAwayTeamId()
                                        : game.getHomeTeamId();

                        String opponentTeamName = home
                                        ? game.getAwayTeamName()
                                        : game.getHomeTeamName();

                        TeamStandingSnapshot opponentStanding = opponentTeamId == null
                                        ? null
                                        : standingsByTeamId.get(opponentTeamId);

                        contexts.put(
                                        teamId,
                                        createContext(
                                                        roundId,
                                                        teamId,
                                                        game,
                                                        opponentTeamId,
                                                        opponentTeamName,
                                                        home,
                                                        opponentStanding));
                }

                return contexts;
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