package com.artajerjes.biwengerassistant.matchday;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchdayDifficultyServiceTest {

    private static final Long LEAGUE_ID = 1L;
    private static final Long ROUND_ID = 4901L;

    @Mock
    private MatchdayContextService matchdayContextService;

    @Mock
    private MatchdayOpponentContextService matchdayOpponentContextService;

    @Mock
    private OpponentDifficultyService opponentDifficultyService;

    @InjectMocks
    private MatchdayDifficultyService matchdayDifficultyService;

    private MatchdayContext currentContext() {

        MatchdayContext context = org.mockito.Mockito.mock(
                MatchdayContext.class);

        when(context.getBiwengerRoundId())
                .thenReturn(ROUND_ID);

        return context;
    }

    @Test
    void resolveForTeamsShouldReturnDifficultyByTeam() {

        MatchdayContext currentContext = currentContext();

        MatchdayOpponentContext opponentContext = createOpponentContext();

        OpponentDifficulty difficulty = new OpponentDifficulty(
                80.0,
                70.0,
                90.0,
                MatchdayVenue.HOME);

        when(matchdayContextService
                .getCurrentContext(LEAGUE_ID))
                .thenReturn(
                        Optional.of(
                                currentContext));

        when(matchdayOpponentContextService
                .resolveForTeams(
                        LEAGUE_ID,
                        ROUND_ID,
                        List.of(100L)))
                .thenReturn(
                        Map.of(
                                100L,
                                opponentContext));

        when(opponentDifficultyService
                .calculate(opponentContext))
                .thenReturn(
                        Optional.of(
                                difficulty));

        Map<Long, OpponentDifficulty> result = matchdayDifficultyService
                .resolveForTeams(
                        LEAGUE_ID,
                        List.of(100L));

        assertEquals(1, result.size());
        assertEquals(
                difficulty,
                result.get(100L));
    }

    @Test
    void resolveForTeamsShouldReturnEmptyWhenThereIsNoCurrentContext() {

        when(matchdayContextService
                .getCurrentContext(LEAGUE_ID))
                .thenReturn(Optional.empty());

        Map<Long, OpponentDifficulty> result = matchdayDifficultyService
                .resolveForTeams(
                        LEAGUE_ID,
                        List.of(100L));

        assertTrue(result.isEmpty());

        verify(
                matchdayOpponentContextService,
                never())
                .resolveForTeams(
                        LEAGUE_ID,
                        ROUND_ID,
                        List.of(100L));
    }

    @Test
    void resolveForTeamsShouldReturnEmptyWhenCurrentContextHasNoRoundId() {

        MatchdayContext currentContext = org.mockito.Mockito.mock(
                MatchdayContext.class);

        when(currentContext.getBiwengerRoundId())
                .thenReturn(null);

        when(matchdayContextService
                .getCurrentContext(LEAGUE_ID))
                .thenReturn(
                        Optional.of(
                                currentContext));

        Map<Long, OpponentDifficulty> result = matchdayDifficultyService
                .resolveForTeams(
                        LEAGUE_ID,
                        List.of(100L));

        assertTrue(result.isEmpty());

        verify(
                matchdayOpponentContextService,
                never())
                .resolveForTeams(
                        LEAGUE_ID,
                        ROUND_ID,
                        List.of(100L));
    }

    @Test
    void resolveForTeamsShouldReturnEmptyWhenThereAreNoOpponentContexts() {

        MatchdayContext currentContext = currentContext();

        when(matchdayContextService
                .getCurrentContext(LEAGUE_ID))
                .thenReturn(
                        Optional.of(
                                currentContext));

        when(matchdayOpponentContextService
                .resolveForTeams(
                        LEAGUE_ID,
                        ROUND_ID,
                        List.of(100L)))
                .thenReturn(Map.of());

        Map<Long, OpponentDifficulty> result = matchdayDifficultyService
                .resolveForTeams(
                        LEAGUE_ID,
                        List.of(100L));

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveForTeamsShouldSkipOpponentWhenDifficultyCannotBeCalculated() {

        MatchdayContext currentContext = currentContext();

        MatchdayOpponentContext opponentContext = createOpponentContext();

        when(matchdayContextService
                .getCurrentContext(LEAGUE_ID))
                .thenReturn(
                        Optional.of(
                                currentContext));

        when(matchdayOpponentContextService
                .resolveForTeams(
                        LEAGUE_ID,
                        ROUND_ID,
                        List.of(100L)))
                .thenReturn(
                        Map.of(
                                100L,
                                opponentContext));

        when(opponentDifficultyService
                .calculate(opponentContext))
                .thenReturn(Optional.empty());

        Map<Long, OpponentDifficulty> result = matchdayDifficultyService
                .resolveForTeams(
                        LEAGUE_ID,
                        List.of(100L));

        assertTrue(result.isEmpty());
    }

    @Test
    void resolveForTeamsShouldReturnEmptyForEmptyTeamIdsWithoutCallingContextService() {

        Map<Long, OpponentDifficulty> result = matchdayDifficultyService
                .resolveForTeams(
                        LEAGUE_ID,
                        List.of());

        assertTrue(result.isEmpty());

        verify(
                matchdayContextService,
                never())
                .getCurrentContext(LEAGUE_ID);
    }

    private MatchdayOpponentContext createOpponentContext() {

        return new MatchdayOpponentContext(
                ROUND_ID,
                5001L,
                100L,
                200L,
                "Rival",
                MatchdayVenue.HOME,
                "preview",
                4,
                6,
                2,
                0,
                1,
                5,
                3);
    }
}