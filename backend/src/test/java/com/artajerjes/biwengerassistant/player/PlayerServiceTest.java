package com.artajerjes.biwengerassistant.player;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.player.dto.CreatePlayerRequest;
import com.artajerjes.biwengerassistant.player.dto.PlayerResponse;
import com.artajerjes.biwengerassistant.player.dto.UpdatePlayerRequest;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    private static final Long LEAGUE_ID = 1L;
    private static final Long PLAYER_ID = 10L;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @InjectMocks
    private PlayerService playerService;

    @Test
    void createShouldSaveAndReturnPlayer() {
        League league = createLeague();

        CreatePlayerRequest request = new CreatePlayerRequest(
                "player-001",
                "Jugador de prueba",
                List.of(PlayerPosition.DF, PlayerPosition.MC),
                "Equipo de prueba",
                1_500_000L
        );

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(
                playerRepository.existsByBiwengerPlayerIdAndLeague_Id(
                        "player-001",
                        LEAGUE_ID
                )
        ).thenReturn(false);

        when(playerRepository.save(any(Player.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PlayerResponse response = playerService.create(
                LEAGUE_ID,
                request
        );

        assertEquals("player-001", response.biwengerPlayerId());
        assertEquals("Jugador de prueba", response.name());
        assertEquals(
                List.of(PlayerPosition.DF, PlayerPosition.MC),
                response.positions()
        );
        assertEquals("Equipo de prueba", response.teamName());
        assertEquals(1_500_000L, response.marketValue());
        assertEquals(0, response.points());
        assertTrue(response.freePlayer());
        assertFalse(response.injured());
        assertFalse(response.captain());
        assertFalse(response.ram());

        verify(playerRepository).save(any(Player.class));
    }

    @Test
    void createShouldThrowWhenLeagueDoesNotExist() {
        CreatePlayerRequest request = new CreatePlayerRequest(
                "player-001",
                "Jugador",
                List.of(PlayerPosition.DF),
                null,
                1_000_000L
        );

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                LeagueNotFoundException.class,
                () -> playerService.create(LEAGUE_ID, request)
        );

        verify(playerRepository, never())
                .save(any(Player.class));
    }

    @Test
    void createShouldThrowWhenPlayerAlreadyExistsInLeague() {
        League league = createLeague();

        CreatePlayerRequest request = new CreatePlayerRequest(
                "player-001",
                "Jugador duplicado",
                List.of(PlayerPosition.MC),
                "Equipo",
                1_000_000L
        );

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(
                playerRepository.existsByBiwengerPlayerIdAndLeague_Id(
                        "player-001",
                        LEAGUE_ID
                )
        ).thenReturn(true);

        assertThrows(
                PlayerAlreadyExistsException.class,
                () -> playerService.create(LEAGUE_ID, request)
        );

        verify(playerRepository, never())
                .save(any(Player.class));
    }

    @Test
    void findAllShouldReturnPlayersFromLeague() {
        League league = createLeague();

        Player firstPlayer = new Player(
                "player-001",
                "Primer jugador",
                List.of(PlayerPosition.DF),
                "Equipo uno",
                1_000_000L,
                league
        );

        Player secondPlayer = new Player(
                "player-002",
                "Segundo jugador",
                List.of(PlayerPosition.MC, PlayerPosition.DF),
                "Equipo dos",
                2_000_000L,
                league
        );

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(List.of(firstPlayer, secondPlayer));

        List<PlayerResponse> result = playerService.findAll(LEAGUE_ID);

        assertEquals(2, result.size());
        assertEquals("Primer jugador", result.get(0).name());
        assertEquals("Segundo jugador", result.get(1).name());
        assertEquals(
                List.of(PlayerPosition.MC, PlayerPosition.DF),
                result.get(1).positions()
        );
    }

    @Test
    void findAllShouldThrowWhenLeagueDoesNotExist() {
        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(false);

        assertThrows(
                LeagueNotFoundException.class,
                () -> playerService.findAll(LEAGUE_ID)
        );

        verify(playerRepository, never())
                .findAllByLeague_Id(LEAGUE_ID);
    }

    @Test
    void findByIdShouldReturnPlayerWhenItExists() {
        Player player = createPlayer();

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                playerRepository.findByIdAndLeague_Id(
                        PLAYER_ID,
                        LEAGUE_ID
                )
        ).thenReturn(Optional.of(player));

        PlayerResponse response = playerService.findById(
                LEAGUE_ID,
                PLAYER_ID
        );

        assertEquals("player-001", response.biwengerPlayerId());
        assertEquals("Jugador de prueba", response.name());
        assertEquals(List.of(PlayerPosition.DF), response.positions());
        assertTrue(response.freePlayer());
    }

    @Test
    void findByIdShouldThrowWhenPlayerDoesNotExist() {
        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                playerRepository.findByIdAndLeague_Id(
                        PLAYER_ID,
                        LEAGUE_ID
                )
        ).thenReturn(Optional.empty());

        assertThrows(
                PlayerNotFoundException.class,
                () -> playerService.findById(
                        LEAGUE_ID,
                        PLAYER_ID
                )
        );
    }

    @Test
    void updateShouldModifyAndReturnPlayer() {
        Player player = createPlayer();

        UpdatePlayerRequest request = new UpdatePlayerRequest(
                "player-001",
                "Jugador actualizado",
                List.of(PlayerPosition.MC, PlayerPosition.DF),
                42,
                "Equipo actualizado",
                1_750_000L,
                false,
                true,
                false,
                250_000L,
                true,
                3_000_000L,
                "Diego",
                LocalDateTime.of(2026, 8, 4, 21, 10)
        );

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                playerRepository.findByIdAndLeague_Id(
                        PLAYER_ID,
                        LEAGUE_ID
                )
        ).thenReturn(Optional.of(player));

        when(
                playerRepository
                        .existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                                "player-001",
                                LEAGUE_ID,
                                PLAYER_ID
                        )
        ).thenReturn(false);

        PlayerResponse response = playerService.update(
                LEAGUE_ID,
                PLAYER_ID,
                request
        );

        assertEquals("Jugador actualizado", response.name());
        assertEquals(
                List.of(PlayerPosition.MC, PlayerPosition.DF),
                response.positions()
        );
        assertEquals(42, response.points());
        assertEquals(1_750_000L, response.marketValue());
        assertTrue(response.captain());
        assertTrue(response.blockedClause());
        assertEquals(3_000_000L, response.clauseValue());
        assertEquals("Diego", response.ownerName());
        assertFalse(response.freePlayer());

        verify(playerRepository, never())
                .save(any(Player.class));
    }

    @Test
    void updateShouldThrowWhenAnotherPlayerHasBiwengerId() {
        Player player = createPlayer();

        UpdatePlayerRequest request = new UpdatePlayerRequest(
                "player-002",
                "Jugador actualizado",
                List.of(PlayerPosition.DL),
                20,
                "Equipo",
                2_000_000L,
                false,
                false,
                false,
                100_000L,
                false,
                null,
                null,
                null
        );

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                playerRepository.findByIdAndLeague_Id(
                        PLAYER_ID,
                        LEAGUE_ID
                )
        ).thenReturn(Optional.of(player));

        when(
                playerRepository
                        .existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                                "player-002",
                                LEAGUE_ID,
                                PLAYER_ID
                        )
        ).thenReturn(true);

        assertThrows(
                PlayerAlreadyExistsException.class,
                () -> playerService.update(
                        LEAGUE_ID,
                        PLAYER_ID,
                        request
                )
        );

        assertEquals("Jugador de prueba", player.getName());
        assertEquals("player-001", player.getBiwengerPlayerId());
    }

    @Test
    void deleteShouldDeleteExistingPlayer() {
        Player player = createPlayer();

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                playerRepository.findByIdAndLeague_Id(
                        PLAYER_ID,
                        LEAGUE_ID
                )
        ).thenReturn(Optional.of(player));

        playerService.delete(LEAGUE_ID, PLAYER_ID);

        verify(playerRepository).delete(player);
    }

    @Test
    void deleteShouldThrowWhenPlayerDoesNotExist() {
        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                playerRepository.findByIdAndLeague_Id(
                        PLAYER_ID,
                        LEAGUE_ID
                )
        ).thenReturn(Optional.empty());

        assertThrows(
                PlayerNotFoundException.class,
                () -> playerService.delete(
                        LEAGUE_ID,
                        PLAYER_ID
                )
        );

        verify(playerRepository, never())
                .delete(any(Player.class));
    }

    private League createLeague() {
        return new League(
                "Liga de prueba",
                "league-001"
        );
    }

    private Player createPlayer() {
        return new Player(
                "player-001",
                "Jugador de prueba",
                List.of(PlayerPosition.DF),
                "Equipo de prueba",
                1_500_000L,
                createLeague()
        );
    }
}