package com.artajerjes.biwengerassistant.player;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;

import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionData;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionPlayer;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionTeam;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerNotFoundException;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.player.dto.CreatePlayerRequest;
import com.artajerjes.biwengerassistant.player.dto.PlayerResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.UpdatePlayerRequest;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

        private static final Long LEAGUE_ID = 1L;
        private static final Long PLAYER_ID = 10L;
        private static final Long MANAGER_ID = 2L;

        private static final LocalDateTime FUTURE_LOCK_DATE = LocalDateTime.of(2099, 1, 1, 0, 0);

        @Mock
        private PlayerRepository playerRepository;

        @Mock
        private LeagueRepository leagueRepository;

        @Mock
        private ManagerRepository managerRepository;

        @Mock
        private BiwengerClient biwengerClient;

        @InjectMocks
        private PlayerService playerService;

        @Test
        void createShouldSaveAndReturnPlayer() {
                League league = createLeague();

                CreatePlayerRequest request = new CreatePlayerRequest(
                                "player-001",
                                "Jugador de prueba",
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC),
                                "Equipo de prueba",
                                1_500_000L);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(
                                playerRepository.existsByBiwengerPlayerIdAndLeague_Id(
                                                "player-001",
                                                LEAGUE_ID))
                                .thenReturn(false);

                when(playerRepository.save(any(Player.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                PlayerResponse response = playerService.create(
                                LEAGUE_ID,
                                request);

                assertEquals(
                                "player-001",
                                response.biwengerPlayerId());

                assertEquals(
                                "Jugador de prueba",
                                response.name());

                assertEquals(
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC),
                                response.positions());

                assertEquals(
                                "Equipo de prueba",
                                response.teamName());

                assertEquals(
                                1_500_000L,
                                response.marketValue());

                assertEquals(0, response.points());
                assertTrue(response.freePlayer());
                assertFalse(response.injured());
                assertFalse(response.captain());
                assertFalse(response.ram());
                assertFalse(response.blockedClause());

                assertNull(response.clauseLockedUntil());
                assertNull(response.ownerId());
                assertNull(response.ownerName());

                verify(playerRepository)
                                .save(any(Player.class));
        }

        @Test
        void createShouldThrowWhenLeagueDoesNotExist() {
                CreatePlayerRequest request = new CreatePlayerRequest(
                                "player-001",
                                "Jugador",
                                List.of(PlayerPosition.DF),
                                null,
                                1_000_000L);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                LeagueNotFoundException.class,
                                () -> playerService.create(
                                                LEAGUE_ID,
                                                request));

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
                                1_000_000L);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(
                                playerRepository.existsByBiwengerPlayerIdAndLeague_Id(
                                                "player-001",
                                                LEAGUE_ID))
                                .thenReturn(true);

                assertThrows(
                                PlayerAlreadyExistsException.class,
                                () -> playerService.create(
                                                LEAGUE_ID,
                                                request));

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
                                league);

                Player secondPlayer = new Player(
                                "player-002",
                                "Segundo jugador",
                                List.of(
                                                PlayerPosition.MC,
                                                PlayerPosition.DF),
                                "Equipo dos",
                                2_000_000L,
                                league);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                                .thenReturn(
                                                List.of(
                                                                firstPlayer,
                                                                secondPlayer));

                List<PlayerResponse> result = playerService.findAll(LEAGUE_ID);

                assertEquals(2, result.size());

                assertEquals(
                                "Primer jugador",
                                result.get(0).name());

                assertEquals(
                                "Segundo jugador",
                                result.get(1).name());

                assertEquals(
                                List.of(
                                                PlayerPosition.MC,
                                                PlayerPosition.DF),
                                result.get(1).positions());
        }

        @Test
        void findAllShouldThrowWhenLeagueDoesNotExist() {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(false);

                assertThrows(
                                LeagueNotFoundException.class,
                                () -> playerService.findAll(LEAGUE_ID));

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
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(player));

                PlayerResponse response = playerService.findById(
                                LEAGUE_ID,
                                PLAYER_ID);

                assertEquals(
                                "player-001",
                                response.biwengerPlayerId());

                assertEquals(
                                "Jugador de prueba",
                                response.name());

                assertEquals(
                                List.of(PlayerPosition.DF),
                                response.positions());

                assertTrue(response.freePlayer());
                assertFalse(response.blockedClause());

                assertNull(response.clauseLockedUntil());
                assertNull(response.ownerId());
                assertNull(response.ownerName());
        }

        @Test
        void findByIdShouldThrowWhenPlayerDoesNotExist() {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(
                                playerRepository.findByIdAndLeague_Id(
                                                PLAYER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                PlayerNotFoundException.class,
                                () -> playerService.findById(
                                                LEAGUE_ID,
                                                PLAYER_ID));
        }

        @Test
        void updateShouldModifyAndReturnPlayerWithOwner() {
                Player player = createPlayer();
                Manager manager = createManager();

                UpdatePlayerRequest request = new UpdatePlayerRequest(
                                "player-001",
                                "Jugador actualizado",
                                List.of(
                                                PlayerPosition.MC,
                                                PlayerPosition.DF),
                                42,
                                "Equipo actualizado",
                                1_750_000L,
                                false,
                                true,
                                false,
                                250_000L,
                                FUTURE_LOCK_DATE,
                                3_000_000L,
                                MANAGER_ID,
                                LocalDateTime.of(
                                                2026,
                                                8,
                                                4,
                                                21,
                                                10));

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(
                                playerRepository.findByIdAndLeague_Id(
                                                PLAYER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(player));

                when(
                                playerRepository
                                                .existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                                                                "player-001",
                                                                LEAGUE_ID,
                                                                PLAYER_ID))
                                .thenReturn(false);

                when(
                                managerRepository.findByIdAndLeague_Id(
                                                MANAGER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(manager));

                PlayerResponse response = playerService.update(
                                LEAGUE_ID,
                                PLAYER_ID,
                                request);

                assertEquals(
                                "Jugador actualizado",
                                response.name());

                assertEquals(
                                List.of(
                                                PlayerPosition.MC,
                                                PlayerPosition.DF),
                                response.positions());

                assertEquals(42, response.points());

                assertEquals(
                                1_750_000L,
                                response.marketValue());

                assertTrue(response.captain());
                assertTrue(response.blockedClause());

                assertEquals(
                                FUTURE_LOCK_DATE,
                                response.clauseLockedUntil());

                assertEquals(
                                3_000_000L,
                                response.clauseValue());

                assertEquals(
                                MANAGER_ID,
                                response.ownerId());

                assertEquals(
                                "SIRG",
                                response.ownerName());

                assertFalse(response.freePlayer());

                verify(playerRepository, never())
                                .save(any(Player.class));
        }

        @Test
        void updateShouldAllowRemovingOwner() {
                Player player = createPlayer();
                Manager manager = createManager();

                player.update(
                                player.getBiwengerPlayerId(),
                                player.getName(),
                                player.getPositions(),
                                player.getPoints(),
                                player.getTeamName(),
                                player.getMarketValue(),
                                player.isInjured(),
                                player.isCaptain(),
                                player.isRam(),
                                player.getValueFluctuation(),
                                player.getClauseLockedUntil(),
                                player.getClauseValue(),
                                manager,
                                player.getSignedAt());

                UpdatePlayerRequest request = new UpdatePlayerRequest(
                                "player-001",
                                "Jugador libre",
                                List.of(PlayerPosition.DF),
                                10,
                                "Equipo de prueba",
                                1_400_000L,
                                false,
                                false,
                                false,
                                -100_000L,
                                null,
                                null,
                                null,
                                null);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(
                                playerRepository.findByIdAndLeague_Id(
                                                PLAYER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(player));

                when(
                                playerRepository
                                                .existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                                                                "player-001",
                                                                LEAGUE_ID,
                                                                PLAYER_ID))
                                .thenReturn(false);

                PlayerResponse response = playerService.update(
                                LEAGUE_ID,
                                PLAYER_ID,
                                request);

                assertTrue(response.freePlayer());
                assertFalse(response.blockedClause());

                assertNull(response.clauseLockedUntil());
                assertNull(response.ownerId());
                assertNull(response.ownerName());

                verify(
                                managerRepository,
                                never()).findByIdAndLeague_Id(
                                                any(Long.class),
                                                any(Long.class));
        }

        @Test
        void updateShouldThrowWhenManagerDoesNotExistInLeague() {
                Player player = createPlayer();

                UpdatePlayerRequest request = new UpdatePlayerRequest(
                                "player-001",
                                "Jugador actualizado",
                                List.of(PlayerPosition.DL),
                                20,
                                "Equipo",
                                2_000_000L,
                                false,
                                false,
                                false,
                                100_000L,
                                null,
                                null,
                                MANAGER_ID,
                                null);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(
                                playerRepository.findByIdAndLeague_Id(
                                                PLAYER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(player));

                when(
                                playerRepository
                                                .existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                                                                "player-001",
                                                                LEAGUE_ID,
                                                                PLAYER_ID))
                                .thenReturn(false);

                when(
                                managerRepository.findByIdAndLeague_Id(
                                                MANAGER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                ManagerNotFoundException.class,
                                () -> playerService.update(
                                                LEAGUE_ID,
                                                PLAYER_ID,
                                                request));

                assertTrue(player.isFreePlayer());
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
                                null,
                                null,
                                null,
                                null);

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(
                                playerRepository.findByIdAndLeague_Id(
                                                PLAYER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(player));

                when(
                                playerRepository
                                                .existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                                                                "player-002",
                                                                LEAGUE_ID,
                                                                PLAYER_ID))
                                .thenReturn(true);

                assertThrows(
                                PlayerAlreadyExistsException.class,
                                () -> playerService.update(
                                                LEAGUE_ID,
                                                PLAYER_ID,
                                                request));

                assertEquals(
                                "Jugador de prueba",
                                player.getName());

                assertEquals(
                                "player-001",
                                player.getBiwengerPlayerId());

                verify(
                                managerRepository,
                                never()).findByIdAndLeague_Id(
                                                any(Long.class),
                                                any(Long.class));
        }

        @Test
        void syncCompetitionPlayersShouldCreateNewPlayers() {
                League league = createLeague();

                BiwengerCompetitionPlayer externalPlayer = new BiwengerCompetitionPlayer(
                                17731L,
                                "Catena",
                                "catena",
                                93L,
                                2,
                                List.of(3),
                                3_630_000L,
                                45_000_000L,
                                24,
                                "ok",
                                120_000L,
                                87,
                                "icon.png",
                                "hero.png");

                Map<String, BiwengerCompetitionPlayer> players = Map.of(
                                "17731",
                                externalPlayer);

                Map<String, BiwengerCompetitionTeam> teams = Map.of(
                                "93",
                                new BiwengerCompetitionTeam(
                                                93L,
                                                "Osasuna",
                                                "osasuna",
                                                "icon.png"));

                BiwengerCompetitionData data = new BiwengerCompetitionData(
                                1L,
                                "Primera División",
                                "la-liga",
                                "football",
                                "€",
                                players,
                                teams);

                BiwengerCompetitionResponse response = new BiwengerCompetitionResponse(
                                200,
                                data);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(biwengerClient.getCompetition())
                                .thenReturn(response);

                when(playerRepository.findByBiwengerPlayerIdAndLeague_Id(
                                "17731",
                                LEAGUE_ID))
                                .thenReturn(Optional.empty());

                when(playerRepository.save(any(Player.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                PlayerSyncResponse result = playerService.syncCompetitionPlayers(LEAGUE_ID);

                assertEquals(1, result.total());
                assertEquals(1, result.created());
                assertEquals(0, result.updated());
                assertEquals(0, result.skipped());

                verify(playerRepository).save(any(Player.class));
        }

        @Test
        void syncCompetitionPlayersShouldUpdateExistingPlayer() {
                League league = createLeague();

                Player existingPlayer = new Player(
                                "17731",
                                "Nombre antiguo",
                                List.of(PlayerPosition.DF),
                                "Equipo antiguo",
                                1_000_000L,
                                league);

                BiwengerCompetitionPlayer externalPlayer = new BiwengerCompetitionPlayer(
                                17731L,
                                "Catena",
                                "catena",
                                93L,
                                2,
                                List.of(3),
                                3_630_000L,
                                45_000_000L,
                                24,
                                "ok",
                                120_000L,
                                87,
                                "icon.png",
                                "hero.png");

                Map<String, BiwengerCompetitionPlayer> players = Map.of(
                                "17731",
                                externalPlayer);

                Map<String, BiwengerCompetitionTeam> teams = Map.of(
                                "93",
                                new BiwengerCompetitionTeam(
                                                93L,
                                                "Osasuna",
                                                "osasuna",
                                                "icon.png"));

                BiwengerCompetitionData data = new BiwengerCompetitionData(
                                1L,
                                "Primera División",
                                "la-liga",
                                "football",
                                "€",
                                players,
                                teams);

                BiwengerCompetitionResponse response = new BiwengerCompetitionResponse(
                                200,
                                data);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(biwengerClient.getCompetition())
                                .thenReturn(response);

                when(
                                playerRepository.findByBiwengerPlayerIdAndLeague_Id(
                                                "17731",
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(existingPlayer));

                PlayerSyncResponse result = playerService.syncCompetitionPlayers(LEAGUE_ID);

                assertEquals(1, result.total());
                assertEquals(0, result.created());
                assertEquals(1, result.updated());
                assertEquals(0, result.skipped());

                assertEquals("Catena", existingPlayer.getName());

                assertEquals(
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC),
                                existingPlayer.getPositions());

                assertEquals("Osasuna", existingPlayer.getTeamName());
                assertEquals(3_630_000L, existingPlayer.getMarketValue());
                assertEquals(120_000L, existingPlayer.getValueFluctuation());
                assertEquals(87, existingPlayer.getPoints());
                assertFalse(existingPlayer.isInjured());

                verify(playerRepository, never())
                                .save(any(Player.class));
        }

        @Test
        void syncCompetitionPlayersShouldSkipInvalidPlayer() {
                League league = createLeague();

                BiwengerCompetitionPlayer invalidPlayer = new BiwengerCompetitionPlayer(
                                null,
                                "Jugador inválido",
                                "jugador-invalido",
                                93L,
                                2,
                                null,
                                1_000_000L,
                                null,
                                null,
                                "ok",
                                0L,
                                0,
                                null,
                                null);

                Map<String, BiwengerCompetitionPlayer> players = Map.of(
                                "invalid",
                                invalidPlayer);

                BiwengerCompetitionData data = new BiwengerCompetitionData(
                                1L,
                                "Primera División",
                                "la-liga",
                                "football",
                                "€",
                                players,
                                Map.of());

                BiwengerCompetitionResponse response = new BiwengerCompetitionResponse(
                                200,
                                data);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(biwengerClient.getCompetition())
                                .thenReturn(response);

                PlayerSyncResponse result = playerService.syncCompetitionPlayers(LEAGUE_ID);

                assertEquals(1, result.total());
                assertEquals(0, result.created());
                assertEquals(0, result.updated());
                assertEquals(1, result.skipped());

                verify(playerRepository, never())
                                .save(any(Player.class));
        }

        @Test
        void syncCompetitionPlayersShouldMapMultiplePositionsWithoutDuplicates() {
                League league = createLeague();

                BiwengerCompetitionPlayer externalPlayer = new BiwengerCompetitionPlayer(
                                99999L,
                                "Jugador polivalente",
                                "jugador-polivalente",
                                2L,
                                2,
                                List.of(3, 4, 2),
                                5_000_000L,
                                null,
                                10,
                                "ok",
                                50_000L,
                                100,
                                null,
                                null);

                Map<String, BiwengerCompetitionPlayer> players = Map.of(
                                "99999",
                                externalPlayer);

                Map<String, BiwengerCompetitionTeam> teams = Map.of(
                                "2",
                                new BiwengerCompetitionTeam(
                                                2L,
                                                "Atlético de Madrid",
                                                "atletico-madrid",
                                                "icon.png"));

                BiwengerCompetitionData data = new BiwengerCompetitionData(
                                1L,
                                "Primera División",
                                "la-liga",
                                "football",
                                "€",
                                players,
                                teams);

                BiwengerCompetitionResponse response = new BiwengerCompetitionResponse(
                                200,
                                data);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(biwengerClient.getCompetition())
                                .thenReturn(response);

                when(
                                playerRepository.findByBiwengerPlayerIdAndLeague_Id(
                                                "99999",
                                                LEAGUE_ID))
                                .thenReturn(Optional.empty());

                when(playerRepository.save(any(Player.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                playerService.syncCompetitionPlayers(LEAGUE_ID);

                ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);

                verify(playerRepository)
                                .save(playerCaptor.capture());

                Player savedPlayer = playerCaptor.getValue();

                assertEquals(
                                List.of(
                                                PlayerPosition.DF,
                                                PlayerPosition.MC,
                                                PlayerPosition.DL),
                                savedPlayer.getPositions());
        }

        @Test
        void syncCompetitionPlayersShouldMarkNonOkStatusAsInjured() {
                League league = createLeague();

                BiwengerCompetitionPlayer externalPlayer = new BiwengerCompetitionPlayer(
                                88888L,
                                "Jugador lesionado",
                                "jugador-lesionado",
                                93L,
                                4,
                                null,
                                7_000_000L,
                                null,
                                9,
                                "injured",
                                -50_000L,
                                25,
                                null,
                                null);

                Map<String, BiwengerCompetitionPlayer> players = Map.of(
                                "88888",
                                externalPlayer);

                Map<String, BiwengerCompetitionTeam> teams = Map.of(
                                "93",
                                new BiwengerCompetitionTeam(
                                                93L,
                                                "Osasuna",
                                                "osasuna",
                                                "icon.png"));

                BiwengerCompetitionData data = new BiwengerCompetitionData(
                                1L,
                                "Primera División",
                                "la-liga",
                                "football",
                                "€",
                                players,
                                teams);

                BiwengerCompetitionResponse response = new BiwengerCompetitionResponse(
                                200,
                                data);

                when(leagueRepository.findById(LEAGUE_ID))
                                .thenReturn(Optional.of(league));

                when(biwengerClient.getCompetition())
                                .thenReturn(response);

                when(
                                playerRepository.findByBiwengerPlayerIdAndLeague_Id(
                                                "88888",
                                                LEAGUE_ID))
                                .thenReturn(Optional.empty());

                when(playerRepository.save(any(Player.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));

                playerService.syncCompetitionPlayers(LEAGUE_ID);

                ArgumentCaptor<Player> playerCaptor = ArgumentCaptor.forClass(Player.class);

                verify(playerRepository)
                                .save(playerCaptor.capture());

                Player savedPlayer = playerCaptor.getValue();

                assertTrue(savedPlayer.isInjured());
                assertEquals(PlayerPosition.DL, savedPlayer.getPositions().get(0));
                assertEquals(-50_000L, savedPlayer.getValueFluctuation());
        }

        @Test
        void deleteShouldDeleteExistingPlayer() {
                Player player = createPlayer();

                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(
                                playerRepository.findByIdAndLeague_Id(
                                                PLAYER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.of(player));

                playerService.delete(
                                LEAGUE_ID,
                                PLAYER_ID);

                verify(playerRepository)
                                .delete(player);
        }

        @Test
        void deleteShouldThrowWhenPlayerDoesNotExist() {
                when(leagueRepository.existsById(LEAGUE_ID))
                                .thenReturn(true);

                when(
                                playerRepository.findByIdAndLeague_Id(
                                                PLAYER_ID,
                                                LEAGUE_ID))
                                .thenReturn(Optional.empty());

                assertThrows(
                                PlayerNotFoundException.class,
                                () -> playerService.delete(
                                                LEAGUE_ID,
                                                PLAYER_ID));

                verify(playerRepository, never())
                                .delete(any(Player.class));
        }

        private League createLeague() {
                League league = new League(
                                "Liga de prueba",
                                "league-001");

                ReflectionTestUtils.setField(
                                league,
                                "id",
                                LEAGUE_ID);

                return league;
        }

        private Player createPlayer() {
                Player player = new Player(
                                "player-001",
                                "Jugador de prueba",
                                List.of(PlayerPosition.DF),
                                "Equipo de prueba",
                                1_500_000L,
                                createLeague());

                ReflectionTestUtils.setField(
                                player,
                                "id",
                                PLAYER_ID);

                return player;
        }

        private Manager createManager() {
                Manager manager = new Manager(
                                11_470_376L,
                                "SIRG",
                                "i/u/11470376.png",
                                0,
                                13,
                                54_100_000L,
                                720_000L,
                                2,
                                "manager",
                                createLeague());

                ReflectionTestUtils.setField(
                                manager,
                                "id",
                                MANAGER_ID);

                return manager;
        }
}