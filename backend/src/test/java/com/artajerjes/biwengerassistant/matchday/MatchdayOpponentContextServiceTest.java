package com.artajerjes.biwengerassistant.matchday;

import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.league.League;

@ExtendWith(MockitoExtension.class)
class MatchdayOpponentContextServiceTest {

        private static final Long LEAGUE_ID = 1L;
        private static final Long ROUND_ID = 4901L;

        @Mock
        private MatchdayGameRepository matchdayGameRepository;

        @Mock
        private TeamStandingSnapshotRepository teamStandingSnapshotRepository;

        @InjectMocks
        private MatchdayOpponentContextService matchdayOpponentContextService;

        @Test
        void resolveShouldReturnOpponentContextWhenTeamIsHome() {

                MatchdayGame game = createGame();

                TeamStandingSnapshot opponentStanding = createStanding(
                                200L,
                                "Equipo visitante",
                                4,
                                6,
                                5,
                                3);

                when(matchdayGameRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of(game));

                when(teamStandingSnapshotRepository
                                .findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
                                                LEAGUE_ID,
                                                ROUND_ID,
                                                200L))
                                .thenReturn(Optional.of(opponentStanding));

                Optional<MatchdayOpponentContext> result = matchdayOpponentContextService.resolve(
                                LEAGUE_ID,
                                ROUND_ID,
                                100L);

                assertTrue(result.isPresent());

                MatchdayOpponentContext context = result.get();

                assertEquals(ROUND_ID, context.roundId());
                assertEquals(5001L, context.gameId());
                assertEquals(100L, context.teamId());
                assertEquals(200L, context.opponentTeamId());
                assertEquals("Equipo visitante", context.opponentTeamName());
                assertEquals(MatchdayVenue.HOME, context.venue());
                assertEquals("preview", context.gameStatus());
                assertEquals(4, context.opponentPosition());
                assertEquals(6, context.opponentPoints());
                assertEquals(
                                opponentStanding.getWon(),
                                context.opponentWon());

                assertEquals(
                                opponentStanding.getLost(),
                                context.opponentLost());

                assertEquals(
                                opponentStanding.getTied(),
                                context.opponentTied());
                assertEquals(5, context.opponentScored());
                assertEquals(3, context.opponentAgainst());
        }

        @Test
        void resolveShouldReturnOpponentContextWhenTeamIsAway() {

                MatchdayGame game = createGame();

                TeamStandingSnapshot opponentStanding = createStanding(
                                100L,
                                "Equipo local",
                                2,
                                7,
                                7,
                                3);

                when(matchdayGameRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of(game));

                when(teamStandingSnapshotRepository
                                .findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
                                                LEAGUE_ID,
                                                ROUND_ID,
                                                100L))
                                .thenReturn(Optional.of(opponentStanding));

                Optional<MatchdayOpponentContext> result = matchdayOpponentContextService.resolve(
                                LEAGUE_ID,
                                ROUND_ID,
                                200L);

                assertTrue(result.isPresent());

                MatchdayOpponentContext context = result.get();

                assertEquals(100L, context.opponentTeamId());
                assertEquals("Equipo local", context.opponentTeamName());
                assertEquals(MatchdayVenue.AWAY, context.venue());
                assertEquals(2, context.opponentPosition());
                assertEquals(7, context.opponentPoints());
                assertEquals(
                                opponentStanding.getWon(),
                                context.opponentWon());

                assertEquals(
                                opponentStanding.getLost(),
                                context.opponentLost());

                assertEquals(
                                opponentStanding.getTied(),
                                context.opponentTied());
                assertEquals(7, context.opponentScored());
                assertEquals(3, context.opponentAgainst());
        }

        @Test
        void resolveShouldReturnEmptyWhenTeamHasNoGame() {

                MatchdayGame game = createGame();

                when(matchdayGameRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of(game));

                Optional<MatchdayOpponentContext> result = matchdayOpponentContextService.resolve(
                                LEAGUE_ID,
                                ROUND_ID,
                                999L);

                assertTrue(result.isEmpty());
        }

        @Test
        void resolveShouldReturnContextWithoutStandingWhenOpponentStandingDoesNotExist() {

                MatchdayGame game = createGame();

                when(matchdayGameRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of(game));

                when(teamStandingSnapshotRepository
                                .findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
                                                LEAGUE_ID,
                                                ROUND_ID,
                                                200L))
                                .thenReturn(Optional.empty());

                Optional<MatchdayOpponentContext> result = matchdayOpponentContextService.resolve(
                                LEAGUE_ID,
                                ROUND_ID,
                                100L);

                assertTrue(result.isPresent());

                MatchdayOpponentContext context = result.get();

                assertEquals(200L, context.opponentTeamId());
                assertEquals("Equipo visitante", context.opponentTeamName());
                assertEquals(MatchdayVenue.HOME, context.venue());

                assertEquals(null, context.opponentPosition());
                assertEquals(null, context.opponentPoints());
                assertEquals(null, context.opponentScored());
                assertEquals(null, context.opponentAgainst());
                assertNull(context.opponentWon());
                assertNull(context.opponentLost());
                assertNull(context.opponentTied());
        }

        @Test
        void resolveForTeamsShouldResolveMultipleTeamsWithOnlyTwoRepositoryQueries() {

                MatchdayGame game = createGame();

                TeamStandingSnapshot homeStanding = createStanding(
                                100L,
                                "Equipo local",
                                2,
                                7,
                                7,
                                3);

                TeamStandingSnapshot awayStanding = createStanding(
                                200L,
                                "Equipo visitante",
                                4,
                                6,
                                5,
                                3);

                when(matchdayGameRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of(game));

                when(teamStandingSnapshotRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of(
                                                homeStanding,
                                                awayStanding));

                Map<Long, MatchdayOpponentContext> result = matchdayOpponentContextService.resolveForTeams(
                                LEAGUE_ID,
                                ROUND_ID,
                                List.of(
                                                100L,
                                                200L));

                assertEquals(2, result.size());

                MatchdayOpponentContext homeContext = result.get(100L);

                assertEquals(200L, homeContext.opponentTeamId());
                assertEquals(
                                "Equipo visitante",
                                homeContext.opponentTeamName());
                assertEquals(
                                MatchdayVenue.HOME,
                                homeContext.venue());
                assertEquals(
                                4,
                                homeContext.opponentPosition());

                MatchdayOpponentContext awayContext = result.get(200L);

                assertEquals(100L, awayContext.opponentTeamId());
                assertEquals(
                                "Equipo local",
                                awayContext.opponentTeamName());
                assertEquals(
                                MatchdayVenue.AWAY,
                                awayContext.venue());
                assertEquals(
                                2,
                                awayContext.opponentPosition());

                verify(
                                matchdayGameRepository,
                                times(1))
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID);

                verify(
                                teamStandingSnapshotRepository,
                                times(1))
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID);
        }

        @Test
        void resolveForTeamsShouldSkipTeamWithoutMatch() {

                MatchdayGame game = createGame();

                when(matchdayGameRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of(game));

                when(teamStandingSnapshotRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of());

                Map<Long, MatchdayOpponentContext> result = matchdayOpponentContextService.resolveForTeams(
                                LEAGUE_ID,
                                ROUND_ID,
                                List.of(
                                                100L,
                                                999L));

                assertEquals(1, result.size());
                assertTrue(result.containsKey(100L));
                assertTrue(!result.containsKey(999L));
        }

        @Test
        void resolveForTeamsShouldKeepContextWhenOpponentStandingIsMissing() {

                MatchdayGame game = createGame();

                when(matchdayGameRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of(game));

                when(teamStandingSnapshotRepository
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID))
                                .thenReturn(List.of());

                Map<Long, MatchdayOpponentContext> result = matchdayOpponentContextService.resolveForTeams(
                                LEAGUE_ID,
                                ROUND_ID,
                                List.of(100L));

                assertEquals(1, result.size());

                MatchdayOpponentContext context = result.get(100L);

                assertEquals(200L, context.opponentTeamId());
                assertEquals(
                                "Equipo visitante",
                                context.opponentTeamName());

                assertNull(context.opponentPosition());
                assertNull(context.opponentPoints());
                assertNull(context.opponentWon());
                assertNull(context.opponentLost());
                assertNull(context.opponentTied());
                assertNull(context.opponentScored());
                assertNull(context.opponentAgainst());
        }

        @Test
        void resolveForTeamsShouldReturnEmptyWithoutQueryingRepositoriesWhenInputIsEmpty() {

                Map<Long, MatchdayOpponentContext> result = matchdayOpponentContextService.resolveForTeams(
                                LEAGUE_ID,
                                ROUND_ID,
                                List.of());

                assertTrue(result.isEmpty());

                verify(
                                matchdayGameRepository,
                                times(0))
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID);

                verify(
                                teamStandingSnapshotRepository,
                                times(0))
                                .findByLeagueIdAndBiwengerRoundId(
                                                LEAGUE_ID,
                                                ROUND_ID);
        }

        private MatchdayGame createGame() {

                League league = new League(
                                "VII Güenguer",
                                "1268640");

                return new MatchdayGame(
                                league,
                                ROUND_ID,
                                5001L,
                                null,
                                1_787_000_000L,
                                "preview",
                                100L,
                                "Equipo local",
                                200L,
                                "Equipo visitante");
        }

        private TeamStandingSnapshot createStanding(
                        Long teamId,
                        String teamName,
                        Integer position,
                        Integer points,
                        Integer scored,
                        Integer against) {

                League league = new League(
                                "VII Güenguer",
                                "1268640");

                return new TeamStandingSnapshot(
                                league,
                                ROUND_ID,
                                teamId,
                                teamName,
                                position,
                                points,
                                2,
                                0,
                                1,
                                scored,
                                against);
        }
}