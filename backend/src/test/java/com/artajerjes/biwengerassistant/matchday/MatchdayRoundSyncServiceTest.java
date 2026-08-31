package com.artajerjes.biwengerassistant.matchday;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;

@ExtendWith(MockitoExtension.class)
class MatchdayRoundSyncServiceTest {

    private static final Long LEAGUE_ID = 1L;

    @Mock
    private BiwengerClient biwengerClient;

    @Mock
    private MatchdayGameService matchdayGameService;

    @Mock
    private TeamStandingSnapshotService teamStandingSnapshotService;

    @InjectMocks
    private MatchdayRoundSyncService matchdayRoundSyncService;

    @Test
    void syncCurrentMatchdayShouldReuseSameRoundsResponse() {

        BiwengerRoundsResponse response = new BiwengerRoundsResponse(
                200,
                null);

        when(biwengerClient.getRounds())
                .thenReturn(response);

        when(matchdayGameService.syncCurrentMatchday(
                LEAGUE_ID,
                response))
                .thenReturn(10);

        when(teamStandingSnapshotService.syncCurrentMatchday(
                LEAGUE_ID,
                response))
                .thenReturn(20);

        MatchdayRoundSyncResult result = matchdayRoundSyncService
                .syncCurrentMatchday(LEAGUE_ID);

        assertEquals(
                10,
                result.games());

        assertEquals(
                20,
                result.teamStandings());

        verify(biwengerClient)
                .getRounds();

        InOrder inOrder = inOrder(
                biwengerClient,
                matchdayGameService,
                teamStandingSnapshotService);

        inOrder.verify(biwengerClient)
                .getRounds();

        inOrder.verify(matchdayGameService)
                .syncCurrentMatchday(
                        LEAGUE_ID,
                        response);

        inOrder.verify(teamStandingSnapshotService)
                .syncCurrentMatchday(
                        LEAGUE_ID,
                        response);
    }
}