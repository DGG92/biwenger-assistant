package com.artajerjes.biwengerassistant.matchday;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsCompetition;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsCompetitionStandings;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsCompetitionTeam;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsData;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsStanding;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;

@ExtendWith(MockitoExtension.class)
class TeamStandingSnapshotServiceTest {

    private static final Long LEAGUE_ID = 1L;
    private static final Long ROUND_ID = 4901L;

    @Mock
    private BiwengerClient biwengerClient;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private TeamStandingSnapshotRepository teamStandingSnapshotRepository;

    private TeamStandingSnapshotService teamStandingSnapshotService;

    private League league;

    @BeforeEach
    void setUp() {

        teamStandingSnapshotService = new TeamStandingSnapshotService(
                teamStandingSnapshotRepository,
                leagueRepository,
                biwengerClient);

        league = new League(
                "VII Güenguer",
                "1268640");

        ReflectionTestUtils.setField(
                league,
                "id",
                LEAGUE_ID);

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));
    }

    @Test
    void syncCurrentMatchdayShouldCreateStandingSnapshots() {

        BiwengerRoundsResponse response = createRoundsResponse(
                List.of(
                        createStanding(
                                1,
                                15L,
                                "Real Madrid",
                                9,
                                3,
                                0,
                                0,
                                10,
                                2),
                        createStanding(
                                2,
                                2L,
                                "Atlético",
                                7,
                                2,
                                0,
                                1,
                                7,
                                3)));

        when(teamStandingSnapshotRepository
                .findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
                        LEAGUE_ID,
                        ROUND_ID,
                        15L))
                .thenReturn(Optional.empty());

        when(teamStandingSnapshotRepository
                .findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
                        LEAGUE_ID,
                        ROUND_ID,
                        2L))
                .thenReturn(Optional.empty());

        int synced = teamStandingSnapshotService.syncCurrentMatchday(
                LEAGUE_ID,
                response);

        assertEquals(2, synced);

        ArgumentCaptor<TeamStandingSnapshot> captor = ArgumentCaptor.forClass(TeamStandingSnapshot.class);

        verify(teamStandingSnapshotRepository,
                org.mockito.Mockito.times(2))
                .save(captor.capture());

        List<TeamStandingSnapshot> saved = captor.getAllValues();

        TeamStandingSnapshot realMadrid = saved.get(0);

        assertSame(
                league,
                realMadrid.getLeague());

        assertEquals(
                ROUND_ID,
                realMadrid.getBiwengerRoundId());

        assertEquals(
                15L,
                realMadrid.getBiwengerTeamId());

        assertEquals(
                "Real Madrid",
                realMadrid.getTeamName());

        assertEquals(
                1,
                realMadrid.getPosition());

        assertEquals(
                9,
                realMadrid.getPoints());

        assertEquals(
                3,
                realMadrid.getWon());

        assertEquals(
                0,
                realMadrid.getLost());

        assertEquals(
                0,
                realMadrid.getTied());

        assertEquals(
                10,
                realMadrid.getScored());

        assertEquals(
                2,
                realMadrid.getAgainst());

        TeamStandingSnapshot atletico = saved.get(1);

        assertEquals(
                2L,
                atletico.getBiwengerTeamId());

        assertEquals(
                "Atlético",
                atletico.getTeamName());

        assertEquals(
                2,
                atletico.getPosition());

        assertEquals(
                7,
                atletico.getPoints());
    }

    @Test
    void syncCurrentMatchdayShouldUpdateExistingSnapshotInsteadOfCreatingDuplicate() {

        TeamStandingSnapshot existing = new TeamStandingSnapshot(
                league,
                ROUND_ID,
                15L,
                "Real Madrid",
                2,
                6,
                2,
                0,
                0,
                7,
                2);

        BiwengerRoundsResponse response = createRoundsResponse(
                List.of(
                        createStanding(
                                1,
                                15L,
                                "Real Madrid",
                                9,
                                3,
                                0,
                                0,
                                10,
                                2)));

        when(teamStandingSnapshotRepository
                .findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
                        LEAGUE_ID,
                        ROUND_ID,
                        15L))
                .thenReturn(Optional.of(existing));

        int synced = teamStandingSnapshotService.syncCurrentMatchday(
                LEAGUE_ID,
                response);

        assertEquals(1, synced);

        verify(teamStandingSnapshotRepository)
                .save(existing);

        assertEquals(
                "Real Madrid",
                existing.getTeamName());

        assertEquals(
                1,
                existing.getPosition());

        assertEquals(
                9,
                existing.getPoints());

        assertEquals(
                3,
                existing.getWon());

        assertEquals(
                0,
                existing.getLost());

        assertEquals(
                0,
                existing.getTied());

        assertEquals(
                10,
                existing.getScored());

        assertEquals(
                2,
                existing.getAgainst());
    }

    @Test
    void syncCurrentMatchdayShouldReturnZeroWhenStandingsAreEmpty() {

        BiwengerRoundsResponse response = createRoundsResponse(
                List.of());

        int synced = teamStandingSnapshotService.syncCurrentMatchday(
                LEAGUE_ID,
                response);

        assertEquals(0, synced);

        verify(
                teamStandingSnapshotRepository,
                never())
                .save(any());
    }

    private BiwengerRoundsResponse createRoundsResponse(
            List<BiwengerRoundsStanding> standings) {

        BiwengerRoundsCompetition competition = new BiwengerRoundsCompetition(
                List.of(
                        new BiwengerRoundsCompetitionStandings(
                                standings)));

        BiwengerRoundsData data = new BiwengerRoundsData(
                ROUND_ID,
                "Jornada 3",
                "J3",
                "active",
                100,
                null,
                List.of(),
                competition);

        return new BiwengerRoundsResponse(
                200,
                data);
    }

    private BiwengerRoundsStanding createStanding(
            Integer position,
            Long teamId,
            String teamName,
            Integer points,
            Integer won,
            Integer lost,
            Integer tied,
            Integer scored,
            Integer against) {

        return new BiwengerRoundsStanding(
                position,
                new BiwengerRoundsCompetitionTeam(
                        teamId,
                        teamName,
                        null),
                points,
                won,
                lost,
                tied,
                scored,
                against);
    }
}