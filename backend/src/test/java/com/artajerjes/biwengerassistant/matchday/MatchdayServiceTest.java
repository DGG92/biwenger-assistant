package com.artajerjes.biwengerassistant.matchday;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionData;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionPlayer;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionTeam;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueData;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueLeague;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueLineup;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueRound;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueSettings;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueStanding;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundGame;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundRef;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundTeam;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsData;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerLineupPlayerRef;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerUserData;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerUserResponse;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayGameStatus;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;
import com.artajerjes.biwengerassistant.auth.CurrentAssistantUserService;
import com.artajerjes.biwengerassistant.manager.Manager;

class MatchdayServiceTest {

        private BiwengerClient biwengerClient;
        private PlayerMatchReportRepository playerMatchReportRepository;
        private MatchdayService matchdayService;
        private CurrentAssistantUserService currentAssistantUserService;

        @BeforeEach
        void setUp() {

                biwengerClient = mock(BiwengerClient.class);

                playerMatchReportRepository = mock(
                                PlayerMatchReportRepository.class);

                currentAssistantUserService = mock(CurrentAssistantUserService.class);

                Manager currentManager = mock(Manager.class);

                when(currentManager.getBiwengerManagerId())
                                .thenReturn(13L);

                when(currentAssistantUserService.getCurrentManager())
                                .thenReturn(currentManager);

                matchdayService = new MatchdayService(
                                biwengerClient,
                                playerMatchReportRepository,
                                currentAssistantUserService);
        }

        @Test
        void getCurrentMatchdayShouldUseAuthenticatedManagerAndResolvePlayerGameStates() {

                Long currentManagerId = 13L;

                Long finishedPlayerId = 2184L;
                Long pendingPlayerId = 32435L;
                Long coachPlayerId = 41088L;

                Long finishedTeamId = 100L;
                Long pendingTeamId = 200L;
                Long coachTeamId = 300L;

                /*
                 * Este manager tiene count=true deliberadamente,
                 * pero NO es el usuario autenticado.
                 *
                 * El servicio no debe elegirlo.
                 */
                BiwengerRoundLeagueLineup wrongLineup = new BiwengerRoundLeagueLineup(
                                "4-3-3",
                                null,
                                null,
                                null,
                                1L,
                                List.of(9999L),
                                List.of(),
                                List.of(),
                                true);

                BiwengerRoundLeagueStanding wrongStanding = new BiwengerRoundLeagueStanding(
                                99L,
                                "Otro manager",
                                null,
                                50,
                                10_000_000L,
                                0L,
                                1,
                                wrongLineup);

                /*
                 * Esta es la alineación efectiva del usuario autenticado.
                 */
                BiwengerRoundLeagueLineup currentLineup = new BiwengerRoundLeagueLineup(
                                "4-4-2",
                                new BiwengerLineupPlayerRef(
                                                finishedPlayerId),
                                new BiwengerLineupPlayerRef(
                                                pendingPlayerId),
                                new BiwengerLineupPlayerRef(
                                                coachPlayerId),
                                1L,
                                List.of(
                                                finishedPlayerId,
                                                pendingPlayerId),
                                List.of(),
                                List.of(),
                                false);

                BiwengerRoundLeagueStanding currentStanding = new BiwengerRoundLeagueStanding(
                                currentManagerId,
                                "Manager actual",
                                null,
                                40,
                                9_000_000L,
                                0L,
                                2,
                                currentLineup);

                BiwengerRoundLeagueSettings settings = new BiwengerRoundLeagueSettings(
                                "rollingLockout",
                                null,
                                5,
                                "onlyNoPlayed",
                                true);

                BiwengerRoundLeagueLeague league = new BiwengerRoundLeagueLeague(
                                1L,
                                "Liga test",
                                "la-liga",
                                "classic",
                                "private",
                                "normal",
                                100,
                                List.of(
                                                wrongStanding,
                                                currentStanding),
                                settings);

                BiwengerRoundLeagueData roundLeagueData = new BiwengerRoundLeagueData(
                                new BiwengerRoundLeagueRound(
                                                4899L),
                                league);

                BiwengerRoundLeagueResponse roundLeagueResponse = new BiwengerRoundLeagueResponse(
                                200,
                                roundLeagueData);

                /*
                 * Jornada partida:
                 *
                 * finishedPlayer -> tramo 1, partido terminado
                 * pendingPlayer -> tramo 2, partido pendiente
                 * coachPlayer -> tramo 2, partido pendiente
                 */
                BiwengerRoundRef roundPart1 = new BiwengerRoundRef(
                                4899L,
                                "Jornada 1",
                                "J1",
                                1);

                BiwengerRoundRef roundPart2 = new BiwengerRoundRef(
                                4899L,
                                "Jornada 1",
                                "J1",
                                2);

                BiwengerRoundGame finishedGame = new BiwengerRoundGame(
                                5001L,
                                1_786_815_000L,
                                "finished",
                                new BiwengerRoundTeam(
                                                finishedTeamId,
                                                "Equipo terminado",
                                                "equipo-terminado",
                                                2),
                                new BiwengerRoundTeam(
                                                400L,
                                                "Rival terminado",
                                                "rival-terminado",
                                                0),
                                roundPart1);

                BiwengerRoundGame pendingGame = new BiwengerRoundGame(
                                5002L,
                                1_786_900_000L,
                                "preview",
                                new BiwengerRoundTeam(
                                                pendingTeamId,
                                                "Equipo pendiente",
                                                "equipo-pendiente",
                                                null),
                                new BiwengerRoundTeam(
                                                coachTeamId,
                                                "Equipo entrenador",
                                                "equipo-entrenador",
                                                null),
                                roundPart2);

                BiwengerRoundsData roundsData = new BiwengerRoundsData(
                                4899L,
                                "Jornada 1",
                                "J1",
                                "active",
                                100,
                                1,
                                List.of(
                                                finishedGame,
                                                pendingGame),
                                null);

                BiwengerRoundsResponse roundsResponse = new BiwengerRoundsResponse(
                                200,
                                roundsData);

                /*
                 * Competition:
                 * relacionamos playerId -> teamId.
                 */
                BiwengerCompetitionPlayer finishedPlayer = competitionPlayer(
                                finishedPlayerId,
                                "Jugador terminado",
                                finishedTeamId);

                BiwengerCompetitionPlayer pendingPlayer = competitionPlayer(
                                pendingPlayerId,
                                "Jugador pendiente",
                                pendingTeamId);

                BiwengerCompetitionPlayer coachPlayer = competitionPlayer(
                                coachPlayerId,
                                "Entrenador",
                                coachTeamId);

                Map<String, BiwengerCompetitionPlayer> competitionPlayers = Map.of(
                                String.valueOf(finishedPlayerId),
                                finishedPlayer,
                                String.valueOf(pendingPlayerId),
                                pendingPlayer,
                                String.valueOf(coachPlayerId),
                                coachPlayer);

                Map<String, BiwengerCompetitionTeam> competitionTeams = Map.of(
                                String.valueOf(finishedTeamId),
                                new BiwengerCompetitionTeam(
                                                finishedTeamId,
                                                "Equipo terminado",
                                                "equipo-terminado",
                                                null),

                                String.valueOf(pendingTeamId),
                                new BiwengerCompetitionTeam(
                                                pendingTeamId,
                                                "Equipo pendiente",
                                                "equipo-pendiente",
                                                null),

                                String.valueOf(coachTeamId),
                                new BiwengerCompetitionTeam(
                                                coachTeamId,
                                                "Equipo entrenador",
                                                "equipo-entrenador",
                                                null));

                BiwengerCompetitionData competitionData = new BiwengerCompetitionData(
                                1L,
                                "LaLiga",
                                "la-liga",
                                "football",
                                "EUR",
                                competitionPlayers,
                                competitionTeams);

                BiwengerCompetitionResponse competitionResponse = new BiwengerCompetitionResponse(
                                200,
                                competitionData);

                when(biwengerClient.getRoundLeague())
                                .thenReturn(roundLeagueResponse);

                when(biwengerClient.getRounds())
                                .thenReturn(roundsResponse);

                when(biwengerClient.getCompetition())
                                .thenReturn(competitionResponse);

                Player reportPlayer = mock(Player.class);

                PlayerMatchReport finishedReport = new PlayerMatchReport(
                                reportPlayer,
                                5001L,
                                4899L,
                                "Jornada 1",
                                "J1",
                                LocalDateTime.now(),
                                "2026-2027",
                                true,
                                null,
                                3);

                when(playerMatchReportRepository
                                .findByPlayer_BiwengerPlayerIdAndBiwengerRoundId(
                                                finishedPlayerId.toString(),
                                                4899L))
                                .thenReturn(Optional.of(finishedReport));

                var response = matchdayService.getCurrentMatchday();

                /*
                 * Datos generales de jornada.
                 */
                assertEquals(
                                4899L,
                                response.roundId());

                assertEquals(
                                "Jornada 1",
                                response.roundName());

                assertEquals(
                                "J1",
                                response.roundShortName());

                assertEquals(
                                1,
                                response.roundPart());

                assertEquals(
                                "active",
                                response.roundStatus());

                assertEquals(
                                "4-4-2",
                                response.formation());

                assertEquals(
                                "rollingLockout",
                                response.splitRound());

                assertEquals(
                                "onlyNoPlayed",
                                response.lineupRoundChangesIn());

                /*
                 * Tenemos dos titulares + entrenador.
                 *
                 * El jugador 9999 del manager incorrecto NO debe aparecer.
                 */
                assertEquals(
                                3,
                                response.players().size());

                /*
                 * Primer jugador:
                 * tramo 1, partido terminado.
                 *
                 * Es el primer elemento de playersID de un 4-4-2,
                 * por tanto ocupa la portería.
                 */
                var finished = response.players().get(0);

                assertEquals(
                                finishedPlayerId,
                                finished.biwengerPlayerId());

                assertEquals(
                                "Jugador terminado",
                                finished.name());

                assertEquals(
                                finishedTeamId,
                                finished.teamId());

                assertEquals(
                                5001L,
                                finished.gameId());

                assertEquals(
                                1,
                                finished.gameRoundPart());

                assertEquals(
                                0,
                                finished.lineupIndex());

                assertEquals(
                                PlayerPosition.PT,
                                finished.lineupPosition());

                assertTrue(
                                finished.starter());

                assertTrue(
                                finished.captain());

                assertFalse(
                                finished.ram());

                assertFalse(
                                finished.coach());

                assertEquals(
                                MatchdayGameStatus.FINISHED,
                                finished.gameStatus());

                assertTrue(
                                finished.locked());

                assertFalse(
                                finished.modifiable());

                assertEquals(
                                3,
                                finished.points());

                /*
                 * Segundo jugador:
                 * tramo 2, partido todavía pendiente.
                 *
                 * Es el índice 1 de un 4-4-2, por tanto defensa.
                 */
                var pending = response.players().get(1);

                assertEquals(
                                pendingPlayerId,
                                pending.biwengerPlayerId());

                assertEquals(
                                pendingTeamId,
                                pending.teamId());

                assertEquals(
                                5002L,
                                pending.gameId());

                assertEquals(
                                2,
                                pending.gameRoundPart());

                assertEquals(
                                1,
                                pending.lineupIndex());

                assertEquals(
                                PlayerPosition.DF,
                                pending.lineupPosition());

                assertTrue(
                                pending.starter());

                assertFalse(
                                pending.captain());

                assertTrue(
                                pending.ram());

                assertEquals(
                                MatchdayGameStatus.PENDING,
                                pending.gameStatus());

                assertFalse(
                                pending.locked());

                assertTrue(
                                pending.modifiable());

                assertEquals(
                                null,
                                pending.points());

                /*
                 * Tercer elemento:
                 * entrenador del tramo 2.
                 *
                 * No ocupa un índice dentro de playersID.
                 */
                var coach = response.players().get(2);

                assertEquals(
                                coachPlayerId,
                                coach.biwengerPlayerId());

                assertEquals(
                                2,
                                coach.gameRoundPart());

                assertNull(
                                coach.lineupIndex());

                assertEquals(
                                PlayerPosition.E,
                                coach.lineupPosition());

                assertTrue(
                                coach.coach());

                assertFalse(
                                coach.starter());

                assertEquals(
                                MatchdayGameStatus.PENDING,
                                coach.gameStatus());

                assertFalse(
                                coach.locked());

                assertTrue(
                                coach.modifiable());

                /*
                 * Prueba explícita:
                 * no hemos elegido al standing con count=true.
                 */
                assertTrue(
                                response.players()
                                                .stream()
                                                .noneMatch(player -> Long.valueOf(9999L)
                                                                .equals(
                                                                                player.biwengerPlayerId())));
        }

        @Test
        void getCurrentMatchdayShouldMarkInPlayPlayerAsLockedAndNotModifiable() {

                Long managerId = 13L;
                Long playerId = 2184L;
                Long teamId = 100L;

                BiwengerRoundLeagueLineup lineup = new BiwengerRoundLeagueLineup(
                                "4-4-2",
                                null,
                                null,
                                null,
                                1L,
                                List.of(playerId),
                                List.of(),
                                List.of(),
                                true);

                BiwengerRoundLeagueStanding standing = new BiwengerRoundLeagueStanding(
                                managerId,
                                "Manager actual",
                                null,
                                0,
                                0L,
                                0L,
                                1,
                                lineup);

                BiwengerRoundLeagueSettings settings = new BiwengerRoundLeagueSettings(
                                "rollingLockout",
                                null,
                                5,
                                "onlyNoPlayed",
                                true);

                BiwengerRoundLeagueResponse roundLeagueResponse = new BiwengerRoundLeagueResponse(
                                200,
                                new BiwengerRoundLeagueData(
                                                new BiwengerRoundLeagueRound(4899L),
                                                new BiwengerRoundLeagueLeague(
                                                                1L,
                                                                "Liga test",
                                                                "la-liga",
                                                                "classic",
                                                                "private",
                                                                "normal",
                                                                100,
                                                                List.of(standing),
                                                                settings)));

                BiwengerRoundRef roundRef = new BiwengerRoundRef(
                                4899L,
                                "Jornada 1",
                                "J1",
                                1);

                BiwengerRoundGame game = new BiwengerRoundGame(
                                5001L,
                                1_786_815_000L,
                                "secondTime",
                                new BiwengerRoundTeam(
                                                teamId,
                                                "Equipo",
                                                "equipo",
                                                1),
                                new BiwengerRoundTeam(
                                                200L,
                                                "Rival",
                                                "rival",
                                                1),
                                roundRef);

                BiwengerRoundsResponse roundsResponse = new BiwengerRoundsResponse(
                                200,
                                new BiwengerRoundsData(
                                                4899L,
                                                "Jornada 1",
                                                "J1",
                                                "active",
                                                100,
                                                1,
                                                List.of(game),
                                                null));

                BiwengerCompetitionResponse competitionResponse = new BiwengerCompetitionResponse(
                                200,
                                new BiwengerCompetitionData(
                                                1L,
                                                "LaLiga",
                                                "la-liga",
                                                "football",
                                                "EUR",
                                                Map.of(
                                                                String.valueOf(playerId),
                                                                competitionPlayer(
                                                                                playerId,
                                                                                "Jugador",
                                                                                teamId)),
                                                Map.of(
                                                                String.valueOf(teamId),
                                                                new BiwengerCompetitionTeam(
                                                                                teamId,
                                                                                "Equipo",
                                                                                "equipo",
                                                                                null))));

                when(biwengerClient.getRoundLeague())
                                .thenReturn(roundLeagueResponse);

                when(biwengerClient.getRounds())
                                .thenReturn(roundsResponse);

                when(biwengerClient.getCompetition())
                                .thenReturn(competitionResponse);

                var response = matchdayService.getCurrentMatchday();

                assertEquals(
                                1,
                                response.players().size());

                var player = response.players().get(0);

                assertEquals(
                                1,
                                player.gameRoundPart());

                assertEquals(
                                0,
                                player.lineupIndex());

                assertEquals(
                                PlayerPosition.PT,
                                player.lineupPosition());

                assertEquals(
                                MatchdayGameStatus.IN_PLAY,
                                player.gameStatus());

                assertTrue(
                                player.locked());

                assertFalse(
                                player.modifiable());
        }

        @Test
        void getCurrentMatchdayShouldMarkUnknownWhenPlayerGameCannotBeResolved() {

                Long managerId = 13L;
                Long playerId = 2184L;
                Long teamId = 999L;

                BiwengerRoundLeagueLineup lineup = new BiwengerRoundLeagueLineup(
                                "4-4-2",
                                null,
                                null,
                                null,
                                1L,
                                List.of(playerId),
                                List.of(),
                                List.of(),
                                true);

                BiwengerRoundLeagueStanding standing = new BiwengerRoundLeagueStanding(
                                managerId,
                                "Manager actual",
                                null,
                                0,
                                0L,
                                0L,
                                1,
                                lineup);

                BiwengerRoundLeagueSettings settings = new BiwengerRoundLeagueSettings(
                                "rollingLockout",
                                null,
                                5,
                                "onlyNoPlayed",
                                true);

                BiwengerRoundLeagueResponse roundLeagueResponse = new BiwengerRoundLeagueResponse(
                                200,
                                new BiwengerRoundLeagueData(
                                                new BiwengerRoundLeagueRound(4899L),
                                                new BiwengerRoundLeagueLeague(
                                                                1L,
                                                                "Liga test",
                                                                "la-liga",
                                                                "classic",
                                                                "private",
                                                                "normal",
                                                                100,
                                                                List.of(standing),
                                                                settings)));

                BiwengerRoundsResponse roundsResponse = new BiwengerRoundsResponse(
                                200,
                                new BiwengerRoundsData(
                                                4899L,
                                                "Jornada 1",
                                                "J1",
                                                "active",
                                                100,
                                                1,
                                                List.of(),
                                                null));

                BiwengerCompetitionResponse competitionResponse = new BiwengerCompetitionResponse(
                                200,
                                new BiwengerCompetitionData(
                                                1L,
                                                "LaLiga",
                                                "la-liga",
                                                "football",
                                                "EUR",
                                                Map.of(
                                                                String.valueOf(playerId),
                                                                competitionPlayer(
                                                                                playerId,
                                                                                "Jugador sin partido",
                                                                                teamId)),
                                                Map.of(
                                                                String.valueOf(teamId),
                                                                new BiwengerCompetitionTeam(
                                                                                teamId,
                                                                                "Equipo sin partido",
                                                                                "equipo-sin-partido",
                                                                                null))));

                when(biwengerClient.getRoundLeague())
                                .thenReturn(roundLeagueResponse);

                when(biwengerClient.getRounds())
                                .thenReturn(roundsResponse);

                when(biwengerClient.getCompetition())
                                .thenReturn(competitionResponse);

                var response = matchdayService.getCurrentMatchday();

                var player = response.players().get(0);

                /*
                 * El partido no puede resolverse, por tanto tampoco
                 * conocemos el tramo.
                 *
                 * Sin embargo, al ser titular sí conocemos su índice
                 * y posición dentro de la formación.
                 */
                assertNull(
                                player.gameRoundPart());

                assertEquals(
                                0,
                                player.lineupIndex());

                assertEquals(
                                PlayerPosition.PT,
                                player.lineupPosition());

                assertEquals(
                                MatchdayGameStatus.UNKNOWN,
                                player.gameStatus());

                assertFalse(
                                player.locked());

                assertFalse(
                                player.modifiable());
        }

        @Test
        void getCurrentMatchdayShouldFailWhenRoundLeagueResponseIsInvalid() {

                when(biwengerClient.getRoundLeague())
                                .thenReturn(null);

                when(biwengerClient.getRounds())
                                .thenReturn(
                                                new BiwengerRoundsResponse(
                                                                200,
                                                                new BiwengerRoundsData(
                                                                                4899L,
                                                                                "Jornada 1",
                                                                                "J1",
                                                                                "active",
                                                                                100,
                                                                                1,
                                                                                List.of(),
                                                                                null)));

                when(biwengerClient.getCompetition())
                                .thenReturn(
                                                new BiwengerCompetitionResponse(
                                                                200,
                                                                new BiwengerCompetitionData(
                                                                                1L,
                                                                                "LaLiga",
                                                                                "la-liga",
                                                                                "football",
                                                                                "EUR",
                                                                                Map.of(),
                                                                                Map.of())));

                when(biwengerClient.getCurrentUser())
                                .thenReturn(
                                                new BiwengerUserResponse(
                                                                200,
                                                                new BiwengerUserData(
                                                                                13L,
                                                                                "Manager actual",
                                                                                null,
                                                                                List.of())));

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> matchdayService.getCurrentMatchday());

                assertEquals(
                                "Invalid Biwenger round league response",
                                exception.getMessage());
        }

        private BiwengerCompetitionPlayer competitionPlayer(
                        Long id,
                        String name,
                        Long teamId) {

                return new BiwengerCompetitionPlayer(
                                id,
                                name,
                                name.toLowerCase()
                                                .replace(" ", "-"),
                                teamId,
                                3,
                                List.of(),
                                1_000_000L,
                                1_000_000L,
                                null,
                                "ok",
                                0L,
                                0,
                                null,
                                null);
        }
}