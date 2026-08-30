package com.artajerjes.biwengerassistant.matchday;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundGame;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundRef;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundTeam;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsData;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;

@ExtendWith(MockitoExtension.class)
class MatchdayGameServiceTest {

    private static final Long LEAGUE_ID = 1L;

    @Mock
    private MatchdayGameRepository matchdayGameRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private BiwengerClient biwengerClient;

    @InjectMocks
    private MatchdayGameService matchdayGameService;

    @Test
    void syncCurrentMatchdayShouldCreateNewGames() {

        League league = createLeague();

        BiwengerRoundGame game = new BiwengerRoundGame(
                5001L,
                1_786_815_000L,
                "preview",
                new BiwengerRoundTeam(
                        100L,
                        "Equipo local",
                        "equipo-local",
                        null),
                new BiwengerRoundTeam(
                        200L,
                        "Equipo visitante",
                        "equipo-visitante",
                        null),
                new BiwengerRoundRef(
                        4899L,
                        "Jornada 1",
                        "J1",
                        null));

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(biwengerClient.getRounds())
                .thenReturn(createRoundsResponse(
                        List.of(game)));

        when(matchdayGameRepository
                .findByLeagueIdAndBiwengerGameId(
                        LEAGUE_ID,
                        5001L))
                .thenReturn(Optional.empty());

        int synced = matchdayGameService
                .syncCurrentMatchday(LEAGUE_ID);

        assertEquals(1, synced);

        ArgumentCaptor<MatchdayGame> captor = ArgumentCaptor.forClass(MatchdayGame.class);

        verify(matchdayGameRepository)
                .save(captor.capture());

        MatchdayGame saved = captor.getValue();

        assertEquals(
                league,
                saved.getLeague());

        assertEquals(
                4899L,
                saved.getBiwengerRoundId());

        assertEquals(
                5001L,
                saved.getBiwengerGameId());

        assertEquals(
                2,
                saved.getRoundPart());

        assertEquals(
                1_786_815_000L,
                saved.getGameDate());

        assertEquals(
                "preview",
                saved.getStatus());

        assertEquals(
                100L,
                saved.getHomeTeamId());

        assertEquals(
                "Equipo local",
                saved.getHomeTeamName());

        assertEquals(
                200L,
                saved.getAwayTeamId());

        assertEquals(
                "Equipo visitante",
                saved.getAwayTeamName());
    }

    @Test
    void syncCurrentMatchdayShouldUpdateExistingGame() {

        League league = createLeague();

        MatchdayGame existing = new MatchdayGame(
                league,
                4899L,
                5001L,
                1,
                1_786_815_000L,
                "preview",
                100L,
                "Equipo local",
                200L,
                "Equipo visitante");

        BiwengerRoundGame updatedGame = new BiwengerRoundGame(
                5001L,
                1_786_815_000L,
                "finished",
                new BiwengerRoundTeam(
                        100L,
                        "Equipo local actualizado",
                        "equipo-local",
                        2),
                new BiwengerRoundTeam(
                        200L,
                        "Equipo visitante actualizado",
                        "equipo-visitante",
                        1),
                new BiwengerRoundRef(
                        4899L,
                        "Jornada 1",
                        "J1",
                        2));

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(biwengerClient.getRounds())
                .thenReturn(createRoundsResponse(
                        List.of(updatedGame)));

        when(matchdayGameRepository
                .findByLeagueIdAndBiwengerGameId(
                        LEAGUE_ID,
                        5001L))
                .thenReturn(Optional.of(existing));

        int synced = matchdayGameService
                .syncCurrentMatchday(LEAGUE_ID);

        assertEquals(1, synced);

        verify(matchdayGameRepository)
                .save(existing);

        assertEquals(
                4899L,
                existing.getBiwengerRoundId());

        assertEquals(
                5001L,
                existing.getBiwengerGameId());

        assertEquals(
                2,
                existing.getRoundPart());

        assertEquals(
                "finished",
                existing.getStatus());

        assertEquals(
                "Equipo local actualizado",
                existing.getHomeTeamName());

        assertEquals(
                "Equipo visitante actualizado",
                existing.getAwayTeamName());
    }

    private BiwengerRoundsResponse createRoundsResponse(
            List<BiwengerRoundGame> games) {

        BiwengerRoundsData data = new BiwengerRoundsData(
                4899L,
                "Jornada 1",
                "J1",
                "active",
                100,
                2,
                games);

        return new BiwengerRoundsResponse(
                200,
                data);
    }

    private League createLeague() {

        League league = new League(
                "VII Güenguer",
                "1268640");

        ReflectionTestUtils.setField(
                league,
                "id",
                LEAGUE_ID);

        return league;
    }
}