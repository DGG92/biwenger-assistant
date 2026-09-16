package com.artajerjes.biwengerassistant.matchday;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Comparator;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.auth.CurrentAssistantUserService;
import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionPlayer;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionTeam;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueLineup;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueStanding;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundGame;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayGameStatus;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayPlayerResponse;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayResponse;
import com.artajerjes.biwengerassistant.player.LineupPositionResolver;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayRoundOptionResponse;

@Service
public class MatchdayService {

        private final BiwengerClient biwengerClient;
        private final PlayerMatchReportRepository playerMatchReportRepository;
        private final CurrentAssistantUserService currentAssistantUserService;

        public MatchdayService(
                        BiwengerClient biwengerClient,
                        PlayerMatchReportRepository playerMatchReportRepository,
                        CurrentAssistantUserService currentAssistantUserService) {

                this.biwengerClient = biwengerClient;
                this.playerMatchReportRepository = playerMatchReportRepository;
                this.currentAssistantUserService = currentAssistantUserService;
        }

        public MatchdayResponse getCurrentMatchday() {

                CompletableFuture<BiwengerRoundLeagueResponse> roundLeagueFuture = CompletableFuture.supplyAsync(
                                biwengerClient::getRoundLeague);

                CompletableFuture<BiwengerRoundsResponse> roundsFuture = CompletableFuture.supplyAsync(
                                biwengerClient::getRounds);

                CompletableFuture<BiwengerCompetitionResponse> competitionFuture = CompletableFuture.supplyAsync(
                                biwengerClient::getCompetition);

                return buildMatchdayResponse(
                                roundLeagueFuture.join(),
                                roundsFuture.join(),
                                competitionFuture.join());
        }

        public MatchdayResponse getMatchday(Long roundId) {

                CompletableFuture<BiwengerRoundLeagueResponse> roundLeagueFuture = CompletableFuture.supplyAsync(
                                () -> biwengerClient.getRoundLeague(roundId));

                CompletableFuture<BiwengerRoundsResponse> roundsFuture = CompletableFuture.supplyAsync(
                                () -> biwengerClient.getRounds(roundId));

                CompletableFuture<BiwengerCompetitionResponse> competitionFuture = CompletableFuture.supplyAsync(
                                biwengerClient::getCompetition);

                return buildMatchdayResponse(
                                roundLeagueFuture.join(),
                                roundsFuture.join(),
                                competitionFuture.join());
        }

        private MatchdayResponse buildMatchdayResponse(
                        BiwengerRoundLeagueResponse roundLeagueResponse,
                        BiwengerRoundsResponse roundsResponse,
                        BiwengerCompetitionResponse competitionResponse) {

                validateResponses(
                                roundLeagueResponse,
                                roundsResponse,
                                competitionResponse);

                var league = roundLeagueResponse.data().league();

                var round = roundsResponse.data();

                String season = resolveSeason(round.id());

                var competition = competitionResponse.data();

                Manager currentManager = currentAssistantUserService.getCurrentManager();

                Long currentManagerId = currentManager.getBiwengerManagerId();

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
                                        round.id(),
                                        season,
                                        round.shortName());

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

        private String resolveSeason(Long biwengerRoundId) {

                if (biwengerRoundId == null) {
                        return null;
                }

                List<String> seasons = playerMatchReportRepository
                                .findSeasonsByBiwengerRoundId(
                                                biwengerRoundId);

                if (seasons == null || seasons.isEmpty()) {
                        return null;
                }

                return seasons.get(0);
        }

        private Integer resolvePoints(
                        Long biwengerPlayerId,
                        Long biwengerRoundId,
                        String season,
                        String roundShort) {

                if (biwengerPlayerId == null) {
                        return null;
                }

                if (season != null
                                && roundShort != null
                                && !roundShort.isBlank()) {

                        Integer logicalRoundPoints = playerMatchReportRepository
                                        .findFirstByPlayer_BiwengerPlayerIdAndSeasonAndRoundShortAndPointsIsNotNullOrderByMatchDateDesc(
                                                        biwengerPlayerId.toString(),
                                                        season,
                                                        roundShort)
                                        .map(PlayerMatchReport::getPoints)
                                        .orElse(null);

                        if (logicalRoundPoints != null) {
                                return logicalRoundPoints;
                        }
                }

                if (biwengerRoundId == null) {
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
                        BiwengerCompetitionResponse competitionResponse) {

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
        }

        @Transactional(readOnly = true)
        public List<MatchdayRoundOptionResponse> getAvailableRounds() {

                Long leagueId = currentAssistantUserService
                                .getCurrentManager()
                                .getLeague()
                                .getId();

                List<String> seasons = playerMatchReportRepository
                                .findLatestScoredSeasonByLeague(
                                                leagueId,
                                                PageRequest.of(0, 1));

                if (seasons.isEmpty()) {
                        return List.of();
                }

                String season = seasons.get(0);

                List<PlayerMatchReport> reports = playerMatchReportRepository
                                .findRoundReportsByLeagueAndSeason(
                                                leagueId,
                                                season);

                Map<String, PlayerMatchReport> latestRoundByShortName = new HashMap<>();

                for (PlayerMatchReport report : reports) {

                        String roundShort = report.getRoundShort();
                        Long roundId = report.getBiwengerRoundId();

                        if (roundShort == null
                                        || roundShort.isBlank()
                                        || roundId == null) {
                                continue;
                        }

                        latestRoundByShortName.merge(
                                        roundShort,
                                        report,
                                        (current, candidate) -> {

                                                if (current.getMatchDate() == null) {
                                                        return candidate;
                                                }

                                                if (candidate.getMatchDate() == null) {
                                                        return current;
                                                }

                                                if (candidate.getMatchDate()
                                                                .isAfter(current.getMatchDate())) {
                                                        return candidate;
                                                }

                                                if (candidate.getMatchDate()
                                                                .equals(current.getMatchDate())
                                                                && candidate.getBiwengerRoundId() > current
                                                                                .getBiwengerRoundId()) {
                                                        return candidate;
                                                }

                                                return current;
                                        });
                }

                return latestRoundByShortName
                                .values()
                                .stream()
                                .map(report -> new MatchdayRoundOptionResponse(
                                                report.getBiwengerRoundId(),
                                                report.getRoundShort()))
                                .sorted(Comparator.comparingInt(
                                                option -> extractRoundNumber(
                                                                option.roundShortName())))
                                .toList();
        }

        private int extractRoundNumber(String roundShortName) {

                if (roundShortName == null
                                || !roundShortName.matches("J\\d+")) {
                        return Integer.MAX_VALUE;
                }

                return Integer.parseInt(
                                roundShortName.substring(1));
        }
}