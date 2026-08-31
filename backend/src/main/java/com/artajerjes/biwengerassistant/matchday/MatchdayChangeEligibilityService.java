package com.artajerjes.biwengerassistant.matchday;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.matchday.dto.MatchdayGameStatus;

@Service
public class MatchdayChangeEligibilityService {

    private final MatchdayContextRepository matchdayContextRepository;
    private final MatchdayGameRepository matchdayGameRepository;

    public MatchdayChangeEligibilityService(
            MatchdayContextRepository matchdayContextRepository,
            MatchdayGameRepository matchdayGameRepository) {

        this.matchdayContextRepository = matchdayContextRepository;
        this.matchdayGameRepository = matchdayGameRepository;
    }

    @Transactional(readOnly = true)
    public Map<Long, Boolean> resolveModifiableByTeam(
            Long leagueId) {

        if (leagueId == null) {
            return Map.of();
        }

        Optional<MatchdayContext> contextOptional = matchdayContextRepository
                .findTopByLeagueIdOrderByIdDesc(
                        leagueId);

        if (contextOptional.isEmpty()) {
            return Map.of();
        }

        MatchdayContext context = contextOptional.get();

        List<MatchdayGame> games = matchdayGameRepository
                .findByLeagueIdAndBiwengerRoundId(
                        leagueId,
                        context.getBiwengerRoundId());

        if (games.isEmpty()) {
            return Map.of();
        }

        Map<Long, Boolean> result = new HashMap<>();

        for (MatchdayGame game : games) {

            boolean modifiable = isGameModifiable(
                    game,
                    context);

            if (game.getHomeTeamId() != null) {
                result.put(
                        game.getHomeTeamId(),
                        modifiable);
            }

            if (game.getAwayTeamId() != null) {
                result.put(
                        game.getAwayTeamId(),
                        modifiable);
            }
        }

        return Map.copyOf(result);
    }

    private boolean isGameModifiable(
            MatchdayGame game,
            MatchdayContext context) {

        MatchdayGameStatus status = parseStatus(game.getStatus());

        /*
         * Con onlyNoPlayed solo permitimos modificar jugadores
         * cuyo partido todavía no haya comenzado.
         *
         * No usamos la fecha de adquisición del jugador:
         * un jugador comprado durante una jornada partida puede
         * seguir siendo utilizable si su partido aún no se ha jugado.
         */
        if ("onlyNoPlayed".equals(
                context.getLineupRoundChangesIn())) {

            return status == MatchdayGameStatus.PENDING;
        }

        /*
         * Con rollingLockout, un jugador queda congelado
         * en cuanto comienza su partido.
         */
        if ("rollingLockout".equals(
                context.getSplitRound())) {

            return status == MatchdayGameStatus.PENDING;
        }

        /*
         * Para configuraciones cuyo comportamiento todavía
         * no conocemos con seguridad, no asumimos que el
         * jugador sea modificable.
         */
        return false;
    }

    private MatchdayGameStatus parseStatus(
            String status) {

        if (status == null
                || status.isBlank()) {

            return MatchdayGameStatus.UNKNOWN;
        }

        return switch (status.trim()) {
            case "preview",
                    "pending",
                    "PENDING" ->
                MatchdayGameStatus.PENDING;

            case "firstTime",
                    "halfTime",
                    "secondTime",
                    "extraTime",
                    "penalties",
                    "playing",
                    "IN_PLAY" ->
                MatchdayGameStatus.IN_PLAY;

            case "finished",
                    "FINISHED" ->
                MatchdayGameStatus.FINISHED;

            default ->
                MatchdayGameStatus.UNKNOWN;
        };
    }
}