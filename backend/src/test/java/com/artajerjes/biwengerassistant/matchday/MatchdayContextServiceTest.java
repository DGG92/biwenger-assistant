package com.artajerjes.biwengerassistant.matchday;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueData;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueLeague;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueRound;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueSettings;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;

class MatchdayContextServiceTest {

    private static final Long LEAGUE_ID = 1L;
    private static final Long ROUND_ID = 4901L;

    private MatchdayContextRepository matchdayContextRepository;
    private LeagueRepository leagueRepository;
    private BiwengerClient biwengerClient;

    private MatchdayContextService matchdayContextService;

    private League league;

    @BeforeEach
    void setUp() {

        matchdayContextRepository = mock(
                MatchdayContextRepository.class);

        leagueRepository = mock(
                LeagueRepository.class);

        biwengerClient = mock(
                BiwengerClient.class);

        matchdayContextService = new MatchdayContextService(
                matchdayContextRepository,
                leagueRepository,
                biwengerClient);

        league = mock(League.class);

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(matchdayContextRepository.save(any(MatchdayContext.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldCreateCurrentMatchdayContextWhenItDoesNotExist() {

        BiwengerRoundLeagueSettings settings = new BiwengerRoundLeagueSettings(
                "rollingLockout",
                "round",
                1,
                "onlyNoPlayed",
                false);

        when(biwengerClient.getRoundLeague())
                .thenReturn(createResponse(settings));

        when(matchdayContextRepository
                .findByLeagueIdAndBiwengerRoundId(
                        LEAGUE_ID,
                        ROUND_ID))
                .thenReturn(Optional.empty());

        MatchdayContext result = matchdayContextService.syncCurrentMatchday(
                LEAGUE_ID);

        assertSame(
                league,
                result.getLeague());

        assertEquals(
                ROUND_ID,
                result.getBiwengerRoundId());

        assertEquals(
                "rollingLockout",
                result.getSplitRound());

        assertEquals(
                "round",
                result.getLineupShow());

        assertEquals(
                1,
                result.getLineupRoundChanges());

        assertEquals(
                "onlyNoPlayed",
                result.getLineupRoundChangesIn());

        assertEquals(
                false,
                result.getLineupRoundChangeStrategy());

        verify(matchdayContextRepository)
                .save(result);
    }

    @Test
    void shouldUpdateCurrentMatchdayContextWhenItAlreadyExists() {

        MatchdayContext existingContext = new MatchdayContext(
                league,
                ROUND_ID,
                "oldSplitRound",
                "oldLineupShow",
                0,
                "oldChangesIn",
                true);

        BiwengerRoundLeagueSettings settings = new BiwengerRoundLeagueSettings(
                "rollingLockout",
                "round",
                1,
                "onlyNoPlayed",
                false);

        when(biwengerClient.getRoundLeague())
                .thenReturn(createResponse(settings));

        when(matchdayContextRepository
                .findByLeagueIdAndBiwengerRoundId(
                        LEAGUE_ID,
                        ROUND_ID))
                .thenReturn(Optional.of(existingContext));

        MatchdayContext result = matchdayContextService.syncCurrentMatchday(
                LEAGUE_ID);

        assertSame(
                existingContext,
                result);

        assertEquals(
                "rollingLockout",
                result.getSplitRound());

        assertEquals(
                "round",
                result.getLineupShow());

        assertEquals(
                1,
                result.getLineupRoundChanges());

        assertEquals(
                "onlyNoPlayed",
                result.getLineupRoundChangesIn());

        assertEquals(
                false,
                result.getLineupRoundChangeStrategy());

        verify(matchdayContextRepository)
                .save(existingContext);
    }

    @Test
    void shouldPersistNullSettingsWhenRoundLeagueHasNoSettings() {

        when(biwengerClient.getRoundLeague())
                .thenReturn(createResponse(null));

        when(matchdayContextRepository
                .findByLeagueIdAndBiwengerRoundId(
                        LEAGUE_ID,
                        ROUND_ID))
                .thenReturn(Optional.empty());

        MatchdayContext result = matchdayContextService.syncCurrentMatchday(
                LEAGUE_ID);

        assertEquals(
                ROUND_ID,
                result.getBiwengerRoundId());

        assertNull(
                result.getSplitRound());

        assertNull(
                result.getLineupShow());

        assertNull(
                result.getLineupRoundChanges());

        assertNull(
                result.getLineupRoundChangesIn());

        assertNull(
                result.getLineupRoundChangeStrategy());

        verify(matchdayContextRepository)
                .save(result);
    }

    private BiwengerRoundLeagueResponse createResponse(
            BiwengerRoundLeagueSettings settings) {

        BiwengerRoundLeagueRound round = new BiwengerRoundLeagueRound(
                ROUND_ID);

        BiwengerRoundLeagueLeague roundLeague = new BiwengerRoundLeagueLeague(
                1268640L,
                "VII Güenguer",
                "la-liga",
                null,
                null,
                null,
                100,
                List.of(),
                settings);

        BiwengerRoundLeagueData data = new BiwengerRoundLeagueData(
                round,
                roundLeague);

        return new BiwengerRoundLeagueResponse(
                200,
                data);
    }
}