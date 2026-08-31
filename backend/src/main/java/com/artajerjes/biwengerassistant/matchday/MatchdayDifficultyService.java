package com.artajerjes.biwengerassistant.matchday;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchdayDifficultyService {

    private final MatchdayContextService matchdayContextService;
    private final MatchdayOpponentContextService matchdayOpponentContextService;
    private final OpponentDifficultyService opponentDifficultyService;

    public MatchdayDifficultyService(
            MatchdayContextService matchdayContextService,
            MatchdayOpponentContextService matchdayOpponentContextService,
            OpponentDifficultyService opponentDifficultyService) {

        this.matchdayContextService = matchdayContextService;
        this.matchdayOpponentContextService = matchdayOpponentContextService;
        this.opponentDifficultyService = opponentDifficultyService;
    }

    @Transactional(readOnly = true)
    public Map<Long, OpponentDifficulty> resolveForTeams(
            Long leagueId,
            Collection<Long> teamIds) {

        if (leagueId == null
                || teamIds == null
                || teamIds.isEmpty()) {

            return Map.of();
        }

        Optional<MatchdayContext> currentContext = matchdayContextService.getCurrentContext(
                leagueId);

        if (currentContext.isEmpty()) {
            return Map.of();
        }

        Long roundId = currentContext.get()
                .getBiwengerRoundId();

        if (roundId == null) {
            return Map.of();
        }

        Map<Long, MatchdayOpponentContext> opponentContexts = matchdayOpponentContextService.resolveForTeams(
                leagueId,
                roundId,
                teamIds);

        if (opponentContexts.isEmpty()) {
            return Map.of();
        }

        Map<Long, OpponentDifficulty> difficulties = new HashMap<>();

        for (Map.Entry<Long, MatchdayOpponentContext> entry : opponentContexts.entrySet()) {

            opponentDifficultyService
                    .calculate(entry.getValue())
                    .ifPresent(difficulty -> difficulties.put(
                            entry.getKey(),
                            difficulty));
        }

        return difficulties;
    }
}