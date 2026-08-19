package com.artajerjes.biwengerassistant.matchday;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionPlayer;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionTeam;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueLineup;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueStanding;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundGame;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerUserResponse;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayGameStatus;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayPlayerResponse;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayResponse;
import com.artajerjes.biwengerassistant.player.LineupPositionResolver;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@Service
public class MatchdayService {

        private final BiwengerClient biwengerClient;
        private final PlayerMatchReportRepository playerMatchReportRepository;

        public MatchdayService(
                        BiwengerClient biwengerClient,
                        PlayerMatchReportRepository playerMatchReportRepository) {

                this.biwengerClient = biwengerClient;
                this.playerMatchReportRepository = playerMatchReportRepository;
        }

        public MatchdayResponse getCurrentMatchday() {

                BiwengerRoundLeagueResponse roundLeagueResponse = biwengerClient.getRoundLeague();

                BiwengerRoundsResponse roundsResponse = biwengerClient.getRounds();

                BiwengerCompetitionResponse competitionResponse = biwengerClient.getCompetition();

                BiwengerUserResponse currentUserResponse = biwengerClient.getCurrentUser();

                validateResponses(
                                roundLeagueResponse,
                                roundsResponse,
                                competitionResponse,
                                currentUserResponse);

                var league = roundLeagueResponse.data().league();

                var round = roundsResponse.data();

                var competition = competitionResponse.data();

                Long currentManagerId = currentUserResponse.data().id();

                BiwengerRoundLeagueStanding standing = findCurrentUserStanding(
                                league.standings(),
                                currentManagerId);

                BiwengerRoundLeagueLineup lineup = standing.lineup();

                if (lineup == null) {
                        throw new IllegalStateException(
                                        "Current Biwenger manager has no effective lineup for the round");
                }

                Map<Long, BiwengerCompetitionPlayer> competitionPlayers = buildCompetitionPlayerMap(
                                competition.players());

                Map<Long, BiwengerCompetitionTeam> competitionTeams = buildCompetitionTeamMap(
                                competition.teams());

                Map<Long, BiwengerRoundGame> gamesByTeam = buildGamesByTeam(
                                round.games());

                Set<Long> starters = toSet(lineup.players());

                Set<Long> reserves = toSet(lineup.reserves());

                Set<Long> discarded = toSet(lineup.discarded());

                Long captainId = lineup.captain() != null
                                ? lineup.captain().id()
                                : null;

                Long ramId = lineup.striker() != null
                                ? lineup.striker().id()
                                : null;

                Long coachId = lineup.coach() != null
                                ? lineup.coach().id()
                                : null;

                /*
                 * El índice de players dentro de la alineación tiene significado
                 * posicional.
                 *
                 * Ejemplo 4-4-2:
                 *
                 * 0 -> PT
                 * 1-4 -> DF
                 * 5-8 -> MC
                 * 9-10 -> DL
                 *
                 * Solo los titulares tienen lineupIndex.
                 */
                Map<Long, Integer> lineupIndexes = buildLineupIndexes(
                                lineup.players());

                List<PlayerPosition> lineupPositions = LineupPositionResolver.resolve(
                                lineup.type());

                /*
                 * LinkedHashSet:
                 *
                 * 1. titulares en su orden original
                 * 2. reservas en su orden original
                 * 3. descartados
                 * 4. entrenador
                 *
                 * Así evitamos perder el orden de la alineación.
                 */
                Set<Long> lineupPlayerIds = new LinkedHashSet<>();

                addValues(
                                lineupPlayerIds,
                                lineup.players());

                addValues(
                                lineupPlayerIds,
                                lineup.reserves());

                addValues(
                                lineupPlayerIds,
                                lineup.discarded());

                if (coachId != null) {
                        lineupPlayerIds.add(coachId);
                }

                List<MatchdayPlayerResponse> players = new ArrayList<>();

                for (Long playerId : lineupPlayerIds) {

                        BiwengerCompetitionPlayer competitionPlayer = competitionPlayers.get(playerId);

                        if (competitionPlayer == null) {
                                /*
                                 * No hacemos fallar toda la jornada por un jugador que
                                 * Biwenger no haya incluido en competition/data.
                                 */
                                continue;
                        }

                        Long teamId = competitionPlayer.teamId();

                        BiwengerCompetitionTeam team = teamId != null
                                        ? competitionTeams.get(teamId)
                                        : null;

                        BiwengerRoundGame game = teamId != null
                                        ? gamesByTeam.get(teamId)
                                        : null;

                        MatchdayGameStatus gameStatus = resolveGameStatus(game);

                        boolean locked = resolveLocked(
                                        gameStatus,
                                        league.settings() != null
                                                        ? league.settings().splitRound()
                                                        : null);

                        boolean modifiable = resolveModifiable(
                                        gameStatus,
                                        league.settings() != null
                                                        ? league.settings().lineupRoundChangesIn()
                                                        : null);

                        Integer gameRoundPart = resolveGameRoundPart(
                                        game);

                        Integer lineupIndex = lineupIndexes.get(
                                        playerId);

                        boolean coach = playerId.equals(
                                        coachId);

                        PlayerPosition lineupPosition = resolveLineupPosition(
                                        lineupIndex,
                                        coach,
                                        lineupPositions);

                        Integer points = resolvePoints(
                                        playerId,
                                        round.id());

                        players.add(
                                        new MatchdayPlayerResponse(
                                                        playerId,
                                                        competitionPlayer.name(),
                                                        team != null
                                                                        ? team.name()
                                                                        : null,
                                                        teamId,
                                                        game != null
                                                                        ? game.id()
                                                                        : null,
                                                        gameRoundPart,
                                                        lineupIndex,
                                                        lineupPosition,
                                                        starters.contains(playerId),
                                                        reserves.contains(playerId),
                                                        discarded.contains(playerId),
                                                        playerId.equals(captainId),
                                                        playerId.equals(ramId),
                                                        coach,
                                                        gameStatus,
                                                        locked,
                                                        modifiable,
                                                        points));
                }

                return new MatchdayResponse(
                                round.id(),
                                round.name(),
                                round.shortName(),
                                round.part(),
                                round.status(),
                                lineup.type(),
                                league.settings() != null
                                                ? league.settings().splitRound()
                                                : null,
                                league.settings() != null
                                                ? league.settings().lineupRoundChangesIn()
                                                : null,
                                players);
        }

        private BiwengerRoundLeagueStanding findCurrentUserStanding(
                        List<BiwengerRoundLeagueStanding> standings,
                        Long currentManagerId) {

                if (currentManagerId == null) {
                        throw new IllegalStateException(
                                        "Current Biwenger user has no id");
                }

                if (standings == null || standings.isEmpty()) {
                        throw new IllegalStateException(
                                        "Biwenger round league response has no standings");
                }

                return standings.stream()
                                .filter(standing -> currentManagerId.equals(
                                                standing.id()))
                                .findFirst()
                                .orElseThrow(() -> new IllegalStateException(
                                                "Could not find current user in round standings"));
        }

        private Map<Long, BiwengerCompetitionPlayer> buildCompetitionPlayerMap(
                        Map<String, BiwengerCompetitionPlayer> players) {

                Map<Long, BiwengerCompetitionPlayer> result = new HashMap<>();

                if (players == null) {
                        return result;
                }

                for (BiwengerCompetitionPlayer player : players.values()) {

                        if (player != null
                                        && player.id() != null) {

                                result.put(
                                                player.id(),
                                                player);
                        }
                }

                return result;
        }

        private Map<Long, BiwengerCompetitionTeam> buildCompetitionTeamMap(
                        Map<String, BiwengerCompetitionTeam> teams) {

                Map<Long, BiwengerCompetitionTeam> result = new HashMap<>();

                if (teams == null) {
                        return result;
                }

                for (BiwengerCompetitionTeam team : teams.values()) {

                        if (team != null
                                        && team.id() != null) {

                                result.put(
                                                team.id(),
                                                team);
                        }
                }

                return result;
        }

        private Map<Long, BiwengerRoundGame> buildGamesByTeam(
                        List<BiwengerRoundGame> games) {

                Map<Long, BiwengerRoundGame> result = new HashMap<>();

                if (games == null) {
                        return result;
                }

                for (BiwengerRoundGame game : games) {

                        if (game == null) {
                                continue;
                        }

                        if (game.home() != null
                                        && game.home().id() != null) {

                                result.put(
                                                game.home().id(),
                                                game);
                        }

                        if (game.away() != null
                                        && game.away().id() != null) {

                                result.put(
                                                game.away().id(),
                                                game);
                        }
                }

                return result;
        }

        private Map<Long, Integer> buildLineupIndexes(
                        List<Long> players) {

                Map<Long, Integer> result = new HashMap<>();

                if (players == null) {
                        return result;
                }

                for (int index = 0; index < players.size(); index++) {

                        Long playerId = players.get(index);

                        if (playerId != null) {
                                result.put(
                                                playerId,
                                                index);
                        }
                }

                return result;
        }

        private Integer resolveGameRoundPart(
                        BiwengerRoundGame game) {

                if (game == null
                                || game.round() == null) {

                        return null;
                }

                return game.round().part();
        }

        private PlayerPosition resolveLineupPosition(
                        Integer lineupIndex,
                        boolean coach,
                        List<PlayerPosition> lineupPositions) {

                /*
                 * El entrenador no ocupa una de las once posiciones
                 * de playersID.
                 */
                if (coach) {
                        return PlayerPosition.E;
                }

                /*
                 * Reservas y descartados no tienen una posición
                 * efectiva dentro de los once titulares.
                 */
                if (lineupIndex == null) {
                        return null;
                }

                if (lineupIndex < 0
                                || lineupIndex >= lineupPositions.size()) {

                        return null;
                }

                return lineupPositions.get(
                                lineupIndex);
        }

        private Set<Long> toSet(
                        List<Long> values) {

                Set<Long> result = new HashSet<>();

                if (values == null) {
                        return result;
                }

                for (Long value : values) {

                        if (value != null) {
                                result.add(value);
                        }
                }

                return result;
        }

        private void addValues(
                        Set<Long> target,
                        List<Long> values) {

                if (values == null) {
                        return;
                }

                for (Long value : values) {

                        if (value != null) {
                                target.add(value);
                        }
                }
        }

        private MatchdayGameStatus resolveGameStatus(
                        BiwengerRoundGame game) {

                if (game == null
                                || game.status() == null) {

                        return MatchdayGameStatus.UNKNOWN;
                }

                return switch (game.status()) {

                        case "preview",
                                        "pending" ->
                                MatchdayGameStatus.PENDING;

                        case "finished" ->
                                MatchdayGameStatus.FINISHED;

                        case "firstTime",
                                        "halfTime",
                                        "secondTime",
                                        "extraTime",
                                        "penalties",
                                        "playing" ->
                                MatchdayGameStatus.IN_PLAY;

                        default ->
                                MatchdayGameStatus.UNKNOWN;
                };
        }

        private boolean resolveLocked(
                        MatchdayGameStatus gameStatus,
                        String splitRound) {

                /*
                 * Con rollingLockout, cuando comienza el partido del
                 * jugador queda congelado.
                 */
                if ("rollingLockout".equals(splitRound)) {

                        return gameStatus == MatchdayGameStatus.IN_PLAY
                                        || gameStatus == MatchdayGameStatus.FINISHED;
                }

                /*
                 * De momento no inferimos reglas de bloqueo de otros
                 * modos de liga que no hemos investigado.
                 */
                return false;
        }

        private boolean resolveModifiable(
                        MatchdayGameStatus gameStatus,
                        String lineupRoundChangesIn) {

                if ("onlyNoPlayed".equals(
                                lineupRoundChangesIn)) {

                        return gameStatus == MatchdayGameStatus.PENDING;
                }

                /*
                 * No inventamos el comportamiento de otras configuraciones.
                 */
                return false;
        }

        private Integer resolvePoints(
                        Long biwengerPlayerId,
                        Long biwengerRoundId) {

                if (biwengerPlayerId == null
                                || biwengerRoundId == null) {

                        return null;
                }

                return playerMatchReportRepository
                                .findByPlayer_BiwengerPlayerIdAndBiwengerRoundId(
                                                biwengerPlayerId.toString(),
                                                biwengerRoundId)
                                .map(PlayerMatchReport::getPoints)
                                .orElse(null);
        }

        private void validateResponses(
                        BiwengerRoundLeagueResponse roundLeagueResponse,
                        BiwengerRoundsResponse roundsResponse,
                        BiwengerCompetitionResponse competitionResponse,
                        BiwengerUserResponse currentUserResponse) {

                if (roundLeagueResponse == null
                                || roundLeagueResponse.data() == null
                                || roundLeagueResponse.data().league() == null) {

                        throw new IllegalStateException(
                                        "Invalid Biwenger round league response");
                }

                if (roundsResponse == null
                                || roundsResponse.data() == null) {

                        throw new IllegalStateException(
                                        "Invalid Biwenger rounds response");
                }

                if (competitionResponse == null
                                || competitionResponse.data() == null) {

                        throw new IllegalStateException(
                                        "Invalid Biwenger competition response");
                }

                if (currentUserResponse == null
                                || currentUserResponse.data() == null) {

                        throw new IllegalStateException(
                                        "Invalid Biwenger current user response");
                }
        }
}