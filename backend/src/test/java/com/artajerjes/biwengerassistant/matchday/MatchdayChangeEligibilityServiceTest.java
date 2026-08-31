package com.artajerjes.biwengerassistant.matchday;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.league.League;

@ExtendWith(MockitoExtension.class)
class MatchdayChangeEligibilityServiceTest {

    private static final Long LEAGUE_ID = 1L;
    private static final Long ROUND_ID = 4901L;

    @Mock
    private MatchdayContextRepository matchdayContextRepository;

    @Mock
    private MatchdayGameRepository matchdayGameRepository;

    @InjectMocks
    private MatchdayChangeEligibilityService matchdayChangeEligibilityService;

    @Test
    void shouldReturnEmptyWhenLeagueIdIsNull() {

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(null);

        assertTrue(result.isEmpty());

        verify(
                matchdayContextRepository,
                never())
                .findTopByLeagueIdOrderByIdDesc(
                        org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldReturnEmptyWhenThereIsNoCurrentContext() {

        when(matchdayContextRepository
                .findTopByLeagueIdOrderByIdDesc(
                        LEAGUE_ID))
                .thenReturn(Optional.empty());

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertTrue(result.isEmpty());

        verify(
                matchdayGameRepository,
                never())
                .findByLeagueIdAndBiwengerRoundId(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void shouldReturnEmptyWhenCurrentRoundHasNoGames() {

        MatchdayContext context = createContext(
                "rollingLockout",
                "onlyNoPlayed");

        when(matchdayContextRepository
                .findTopByLeagueIdOrderByIdDesc(
                        LEAGUE_ID))
                .thenReturn(Optional.of(context));

        when(matchdayGameRepository
                .findByLeagueIdAndBiwengerRoundId(
                        LEAGUE_ID,
                        ROUND_ID))
                .thenReturn(List.of());

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldAllowTeamsWhoseGameIsPending() {

        MatchdayContext context = createContext(
                "rollingLockout",
                "onlyNoPlayed");

        MatchdayGame game = createGame(
                5001L,
                "PENDING",
                100L,
                200L);

        mockCurrentRound(
                context,
                List.of(game));

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertEquals(2, result.size());

        assertTrue(result.get(100L));
        assertTrue(result.get(200L));
    }

    @Test
    void shouldBlockTeamsWhoseGameIsInPlay() {

        MatchdayContext context = createContext(
                "rollingLockout",
                "onlyNoPlayed");

        MatchdayGame game = createGame(
                5001L,
                "IN_PLAY",
                100L,
                200L);

        mockCurrentRound(
                context,
                List.of(game));

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertFalse(result.get(100L));
        assertFalse(result.get(200L));
    }

    @Test
    void shouldBlockTeamsWhoseGameIsFinished() {

        MatchdayContext context = createContext(
                "rollingLockout",
                "onlyNoPlayed");

        MatchdayGame game = createGame(
                5001L,
                "FINISHED",
                100L,
                200L);

        mockCurrentRound(
                context,
                List.of(game));

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertFalse(result.get(100L));
        assertFalse(result.get(200L));
    }

    @Test
    void shouldBlockTeamsWhenGameStatusIsUnknown() {

        MatchdayContext context = createContext(
                "rollingLockout",
                "onlyNoPlayed");

        MatchdayGame game = createGame(
                5001L,
                "UNKNOWN",
                100L,
                200L);

        mockCurrentRound(
                context,
                List.of(game));

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertFalse(result.get(100L));
        assertFalse(result.get(200L));
    }

    @Test
    void shouldBlockTeamsWhenPersistedStatusIsNotRecognized() {

        MatchdayContext context = createContext(
                "rollingLockout",
                "onlyNoPlayed");

        MatchdayGame game = createGame(
                5001L,
                "unexpected-status",
                100L,
                200L);

        mockCurrentRound(
                context,
                List.of(game));

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertFalse(result.get(100L));
        assertFalse(result.get(200L));
    }

    @Test
    void shouldKeepFuturePartModifiableWhenPreviousPartIsFinished() {

        MatchdayContext context = createContext(
                "rollingLockout",
                "onlyNoPlayed");

        MatchdayGame finishedGame = createGame(
                5001L,
                "FINISHED",
                100L,
                200L);

        MatchdayGame futureGame = createGame(
                5002L,
                "PENDING",
                300L,
                400L);

        mockCurrentRound(
                context,
                List.of(
                        finishedGame,
                        futureGame));

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertFalse(result.get(100L));
        assertFalse(result.get(200L));

        assertTrue(result.get(300L));
        assertTrue(result.get(400L));
    }

    @Test
    void shouldNotAssumeModifiableForUnknownLeagueConfiguration() {

        MatchdayContext context = createContext(
                null,
                null);

        MatchdayGame game = createGame(
                5001L,
                "PENDING",
                100L,
                200L);

        mockCurrentRound(
                context,
                List.of(game));

        Map<Long, Boolean> result = matchdayChangeEligibilityService
                .resolveModifiableByTeam(
                        LEAGUE_ID);

        assertFalse(result.get(100L));
        assertFalse(result.get(200L));
    }

    private void mockCurrentRound(
            MatchdayContext context,
            List<MatchdayGame> games) {

        when(matchdayContextRepository
                .findTopByLeagueIdOrderByIdDesc(
                        LEAGUE_ID))
                .thenReturn(Optional.of(context));

        when(matchdayGameRepository
                .findByLeagueIdAndBiwengerRoundId(
                        LEAGUE_ID,
                        ROUND_ID))
                .thenReturn(games);
    }

    private MatchdayContext createContext(
            String splitRound,
            String lineupRoundChangesIn) {

        League league = new League(
                "VII Güenguer",
                "1268640");

        return new MatchdayContext(
                league,
                ROUND_ID,
                splitRound,
                "round",
                1,
                lineupRoundChangesIn,
                false);
    }

    private MatchdayGame createGame(
            Long gameId,
            String status,
            Long homeTeamId,
            Long awayTeamId) {

        League league = new League(
                "VII Güenguer",
                "1268640");

        return new MatchdayGame(
                league,
                ROUND_ID,
                gameId,
                null,
                1_786_815_000L,
                status,
                homeTeamId,
                "Equipo local",
                awayTeamId,
                "Equipo visitante");
    }
}