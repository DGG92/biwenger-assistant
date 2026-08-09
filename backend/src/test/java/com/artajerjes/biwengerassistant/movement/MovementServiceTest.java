package com.artajerjes.biwengerassistant.movement;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.ArgumentMatchers.any;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerBoardEvent;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerHomeData;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerHomeLeague;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerHomeResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerMovementBid;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerMovementItem;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerMovementUser;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.movement.dto.MovementResponse;
import com.artajerjes.biwengerassistant.movement.dto.MovementSyncResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class MovementServiceTest {

    private static final Long LEAGUE_ID = 1L;

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private LeagueRepository leagueRepository;

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private BiwengerClient biwengerClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private MovementService movementService;

    @Test
    void syncShouldCreateMarketPurchase() {
        League league = createLeague();

        Player player = createPlayer(
                10L,
                "18564",
                "Jugador mercado");

        Manager buyer = createManager(
                20L,
                8_365_817L,
                "AC LEO");

        BiwengerMovementItem item = new BiwengerMovementItem(
                18564L,
                null,
                new BiwengerMovementUser(
                        8_365_817L,
                        "AC LEO",
                        null),
                5_955_000L,
                null,
                null);

        BiwengerHomeResponse response = createHomeResponse(
                "market",
                1786251807L,
                List.of(item));

        mockLeaguePlayersAndManagers(
                league,
                List.of(player),
                List.of(buyer));

        when(biwengerClient.getHome())
                .thenReturn(response);

        when(
                movementRepository.existsByExternalKey(
                        "1|1786251807|18564|MARKET_PURCHASE|null|8365817|5955000"))
                .thenReturn(false);

        MovementSyncResponse result = movementService.sync(LEAGUE_ID);

        assertEquals(1, result.processed());
        assertEquals(1, result.created());
        assertEquals(0, result.duplicated());
        assertEquals(0, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);

        verify(movementRepository)
                .save(captor.capture());

        Movement movement = captor.getValue();

        assertEquals(
                MovementType.MARKET_PURCHASE,
                movement.getType());

        assertEquals(player, movement.getPlayer());
        assertNull(movement.getFromManager());
        assertEquals(buyer, movement.getToManager());

        assertEquals(
                5_955_000L,
                movement.getAmount());

        assertEquals(
                "1|1786251807|18564|MARKET_PURCHASE|null|8365817|5955000",
                movement.getExternalKey());
    }

    @Test
    void syncShouldCreateAuctionPurchase() {
        League league = createLeague();

        Player player = createPlayer(
                11L,
                "14800",
                "Jugador subasta");

        Manager buyer = createManager(
                21L,
                13_961_282L,
                "Al Water");

        BiwengerMovementItem item = new BiwengerMovementItem(
                14800L,
                null,
                new BiwengerMovementUser(
                        13_961_282L,
                        "Al Water",
                        null),
                1_850_000L,
                "auction",
                null);

        BiwengerHomeResponse response = createHomeResponse(
                "transfer",
                1786243329L,
                List.of(item));

        mockLeaguePlayersAndManagers(
                league,
                List.of(player),
                List.of(buyer));

        when(biwengerClient.getHome())
                .thenReturn(response);

        when(movementRepository.existsByExternalKey(any(String.class)))
                .thenReturn(false);

        MovementSyncResponse result = movementService.sync(LEAGUE_ID);

        assertEquals(1, result.created());

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);

        verify(movementRepository)
                .save(captor.capture());

        Movement movement = captor.getValue();

        assertEquals(
                MovementType.AUCTION_PURCHASE,
                movement.getType());

        assertNull(movement.getFromManager());
        assertEquals(buyer, movement.getToManager());

        assertEquals(
                1_850_000L,
                movement.getAmount());
    }

    @Test
    void syncShouldCreateImmediateSale() {
        League league = createLeague();

        Player player = createPlayer(
                12L,
                "39874",
                "Lucas Cepeda");

        Manager seller = createManager(
                22L,
                8_365_817L,
                "AC LEO");

        BiwengerMovementItem item = new BiwengerMovementItem(
                39874L,
                new BiwengerMovementUser(
                        8_365_817L,
                        "AC LEO",
                        null),
                null,
                75_000L,
                "immediateSale",
                null);

        BiwengerHomeResponse response = createHomeResponse(
                "transfer",
                1786176824L,
                List.of(item));

        mockLeaguePlayersAndManagers(
                league,
                List.of(player),
                List.of(seller));

        when(biwengerClient.getHome())
                .thenReturn(response);

        when(movementRepository.existsByExternalKey(any(String.class)))
                .thenReturn(false);

        MovementSyncResponse result = movementService.sync(LEAGUE_ID);

        assertEquals(1, result.processed());
        assertEquals(1, result.created());

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);

        verify(movementRepository)
                .save(captor.capture());

        Movement movement = captor.getValue();

        assertEquals(
                MovementType.IMMEDIATE_SALE,
                movement.getType());

        assertEquals(player, movement.getPlayer());
        assertEquals(seller, movement.getFromManager());
        assertNull(movement.getToManager());

        assertEquals(
                75_000L,
                movement.getAmount());

        assertEquals(
                "1|1786176824|39874|IMMEDIATE_SALE|8365817|null|75000",
                movement.getExternalKey());
    }

    @Test
    void syncShouldCreateGenericTransfer() {
        League league = createLeague();

        Player player = createPlayer(
                13L,
                "2169",
                "Jugador transfer");

        Manager seller = createManager(
                23L,
                11_470_376L,
                "SIRG");

        BiwengerMovementItem item = new BiwengerMovementItem(
                2169L,
                new BiwengerMovementUser(
                        11_470_376L,
                        "SIRG",
                        null),
                null,
                2_557_700L,
                null,
                null);

        BiwengerHomeResponse response = createHomeResponse(
                "transfer",
                1786315995L,
                List.of(item));

        mockLeaguePlayersAndManagers(
                league,
                List.of(player),
                List.of(seller));

        when(biwengerClient.getHome())
                .thenReturn(response);

        when(movementRepository.existsByExternalKey(any(String.class)))
                .thenReturn(false);

        movementService.sync(LEAGUE_ID);

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);

        verify(movementRepository)
                .save(captor.capture());

        assertEquals(
                MovementType.TRANSFER,
                captor.getValue().getType());
    }

    @Test
    void syncShouldSaveMarketBids() {
        League league = createLeague();

        Player player = createPlayer(
                14L,
                "18564",
                "Jugador mercado");

        Manager winner = createManager(
                24L,
                8_365_817L,
                "AC LEO");

        Manager bidderOne = createManager(
                25L,
                11_467_137L,
                "Califato Omeya");

        Manager bidderTwo = createManager(
                26L,
                11_470_376L,
                "SIRG");

        BiwengerMovementItem item = new BiwengerMovementItem(
                18564L,
                null,
                new BiwengerMovementUser(
                        8_365_817L,
                        "AC LEO",
                        null),
                5_955_000L,
                null,
                List.of(
                        new BiwengerMovementBid(
                                new BiwengerMovementUser(
                                        11_467_137L,
                                        "Califato Omeya",
                                        null),
                                5_270_005L),
                        new BiwengerMovementBid(
                                new BiwengerMovementUser(
                                        11_470_376L,
                                        "SIRG",
                                        null),
                                5_200_000L)));

        mockLeaguePlayersAndManagers(
                league,
                List.of(player),
                List.of(
                        winner,
                        bidderOne,
                        bidderTwo));

        BiwengerHomeResponse response = createHomeResponse(
                "market",
                1786251807L,
                List.of(item));

        when(biwengerClient.getHome())
                .thenReturn(response);

        when(movementRepository.existsByExternalKey(any(String.class)))
                .thenReturn(false);

        movementService.sync(LEAGUE_ID);

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);

        verify(movementRepository)
                .save(captor.capture());

        Movement movement = captor.getValue();

        assertEquals(2, movement.getBids().size());

        assertEquals(
                bidderOne,
                movement.getBids().get(0).getManager());

        assertEquals(
                5_270_005L,
                movement.getBids().get(0).getAmount());

        assertEquals(
                movement,
                movement.getBids().get(0).getMovement());

        assertEquals(
                bidderTwo,
                movement.getBids().get(1).getManager());

        assertEquals(
                5_200_000L,
                movement.getBids().get(1).getAmount());
    }

    @Test
    void syncShouldNotDuplicateExistingMovement() {
        League league = createLeague();

        Player player = createPlayer(
                15L,
                "39874",
                "Lucas Cepeda");

        Manager seller = createManager(
                27L,
                8_365_817L,
                "AC LEO");

        BiwengerMovementItem item = new BiwengerMovementItem(
                39874L,
                new BiwengerMovementUser(
                        8_365_817L,
                        "AC LEO",
                        null),
                null,
                75_000L,
                "immediateSale",
                null);

        mockLeaguePlayersAndManagers(
                league,
                List.of(player),
                List.of(seller));

        BiwengerHomeResponse response = createHomeResponse(
                "transfer",
                1786176824L,
                List.of(item));

        when(biwengerClient.getHome())
                .thenReturn(response);

        when(
                movementRepository.existsByExternalKey(
                        "1|1786176824|39874|IMMEDIATE_SALE|8365817|null|75000"))
                .thenReturn(true);

        MovementSyncResponse result = movementService.sync(LEAGUE_ID);

        assertEquals(1, result.processed());
        assertEquals(0, result.created());
        assertEquals(1, result.duplicated());
        assertEquals(0, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        verify(
                movementRepository,
                never()).save(any(Movement.class));
    }

    @Test
    void syncShouldCountPlayerNotFoundAndSkipMovement() {
        League league = createLeague();

        BiwengerMovementItem item = new BiwengerMovementItem(
                999999L,
                null,
                null,
                100_000L,
                null,
                null);

        mockLeaguePlayersAndManagers(
                league,
                List.of(),
                List.of());

        BiwengerHomeResponse response = createHomeResponse(
                "transfer",
                1786200000L,
                List.of(item));

        when(biwengerClient.getHome())
                .thenReturn(response);

        MovementSyncResponse result = movementService.sync(LEAGUE_ID);

        assertEquals(1, result.processed());
        assertEquals(0, result.created());
        assertEquals(0, result.duplicated());
        assertEquals(1, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        verify(
                movementRepository,
                never()).save(any(Movement.class));
    }

    @Test
    void syncShouldCountMissingManagerButStillSaveMovement() {
        League league = createLeague();

        Player player = createPlayer(
                16L,
                "2169",
                "Jugador transfer");

        BiwengerMovementItem item = new BiwengerMovementItem(
                2169L,
                new BiwengerMovementUser(
                        99_999_999L,
                        "Manager desconocido",
                        null),
                null,
                500_000L,
                null,
                null);

        mockLeaguePlayersAndManagers(
                league,
                List.of(player),
                List.of());

        BiwengerHomeResponse response = createHomeResponse(
                "transfer",
                1786200000L,
                List.of(item));

        when(biwengerClient.getHome())
                .thenReturn(response);

        when(movementRepository.existsByExternalKey(any(String.class)))
                .thenReturn(false);

        MovementSyncResponse result = movementService.sync(LEAGUE_ID);

        assertEquals(1, result.created());
        assertEquals(1, result.managersNotFound());

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);

        verify(movementRepository)
                .save(captor.capture());

        assertNull(
                captor.getValue().getFromManager());
    }

    @Test
    void syncShouldCountMissingBidderAndSkipBid() {
        League league = createLeague();

        Player player = createPlayer(
                17L,
                "18564",
                "Jugador mercado");

        BiwengerMovementItem item = new BiwengerMovementItem(
                18564L,
                null,
                null,
                5_000_000L,
                null,
                List.of(
                        new BiwengerMovementBid(
                                new BiwengerMovementUser(
                                        99_999_999L,
                                        "Pujador desconocido",
                                        null),
                                4_900_000L)));

        mockLeaguePlayersAndManagers(
                league,
                List.of(player),
                List.of());

        BiwengerHomeResponse response = createHomeResponse(
                "market",
                1786251807L,
                List.of(item));

        when(biwengerClient.getHome())
                .thenReturn(response);

        when(movementRepository.existsByExternalKey(any(String.class)))
                .thenReturn(false);

        MovementSyncResponse result = movementService.sync(LEAGUE_ID);

        assertEquals(1, result.created());
        assertEquals(1, result.managersNotFound());

        ArgumentCaptor<Movement> captor = ArgumentCaptor.forClass(Movement.class);

        verify(movementRepository)
                .save(captor.capture());

        assertEquals(
                0,
                captor.getValue().getBids().size());
    }

    @Test
    void syncShouldIgnoreUnsupportedBoardEvents() {
        League league = createLeague();

        mockLeaguePlayersAndManagers(
                league,
                List.of(),
                List.of());

        BiwengerBoardEvent adminEvent = new BiwengerBoardEvent(
                "adminText",
                objectMapper.valueToTree(
                        "Mensaje administrativo"),
                1786200000L);

        BiwengerHomeResponse response = createHomeResponse(
                List.of(adminEvent));

        when(biwengerClient.getHome())
                .thenReturn(response);

        MovementSyncResponse result = movementService.sync(LEAGUE_ID);

        assertEquals(0, result.processed());
        assertEquals(0, result.created());
        assertEquals(0, result.duplicated());
        assertEquals(0, result.playersNotFound());
        assertEquals(0, result.managersNotFound());

        verify(
                movementRepository,
                never()).save(any(Movement.class));
    }

    @Test
    void syncShouldThrowWhenLeagueDoesNotExist() {
        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.empty());

        assertThrows(
                LeagueNotFoundException.class,
                () -> movementService.sync(LEAGUE_ID));

        verify(biwengerClient, never())
                .getHome();
    }

    @Test
    void syncShouldThrowWhenHomeResponseIsInvalid() {
        League league = createLeague();

        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(biwengerClient.getHome())
                .thenReturn(null);

        assertThrows(
                IllegalStateException.class,
                () -> movementService.sync(LEAGUE_ID));

        verify(
                movementRepository,
                never()).save(any(Movement.class));
    }

    @Test
    void findAllShouldReturnMovementResponsesWithManagersAndBids() {
        League league = createLeague();

        Player player = createPlayer(
                30L,
                "18564",
                "Jugador mercado");

        Manager buyer = createManager(
                31L,
                8_365_817L,
                "AC LEO");

        Manager bidder = createManager(
                32L,
                11_467_137L,
                "Califato Omeya");

        Movement movement = new Movement(
                "1|1786251807|18564|MARKET_PURCHASE|null|8365817|5955000",
                MovementType.MARKET_PURCHASE,
                player,
                null,
                buyer,
                5_955_000L,
                LocalDateTime.of(2026, 8, 9, 12, 0),
                league);

        ReflectionTestUtils.setField(
                movement,
                "id",
                100L);

        movement.addBid(
                new MovementBid(
                        bidder,
                        5_270_005L));

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                movementRepository
                        .findAllByLeague_IdOrderByOccurredAtDesc(
                                LEAGUE_ID))
                .thenReturn(List.of(movement));

        List<MovementResponse> result = movementService.findAll(LEAGUE_ID);

        assertEquals(1, result.size());

        MovementResponse response = result.get(0);

        assertEquals(100L, response.id());
        assertEquals(
                MovementType.MARKET_PURCHASE,
                response.type());

        assertEquals(30L, response.playerId());
        assertEquals(
                "18564",
                response.biwengerPlayerId());
        assertEquals(
                "Jugador mercado",
                response.playerName());

        assertNull(response.fromManagerId());
        assertNull(response.fromManagerName());

        assertEquals(31L, response.toManagerId());
        assertEquals(
                "AC LEO",
                response.toManagerName());

        assertEquals(
                5_955_000L,
                response.amount());

        assertEquals(
                LocalDateTime.of(
                        2026,
                        8,
                        9,
                        12,
                        0),
                response.occurredAt());

        assertEquals(1, response.bids().size());

        assertEquals(
                32L,
                response.bids().get(0).managerId());

        assertEquals(
                "Califato Omeya",
                response.bids().get(0).managerName());

        assertEquals(
                5_270_005L,
                response.bids().get(0).amount());
    }

    @Test
    void findAllShouldReturnImmediateSaleWithOnlyFromManager() {
        League league = createLeague();

        Player player = createPlayer(
                33L,
                "39874",
                "Lucas Cepeda");

        Manager seller = createManager(
                34L,
                8_365_817L,
                "AC LEO");

        Movement movement = new Movement(
                "1|1786176824|39874|IMMEDIATE_SALE|8365817|null|75000",
                MovementType.IMMEDIATE_SALE,
                player,
                seller,
                null,
                75_000L,
                LocalDateTime.of(
                        2026,
                        8,
                        8,
                        15,
                        0),
                league);

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                movementRepository
                        .findAllByLeague_IdOrderByOccurredAtDesc(
                                LEAGUE_ID))
                .thenReturn(List.of(movement));

        MovementResponse response = movementService
                .findAll(LEAGUE_ID)
                .get(0);

        assertEquals(
                MovementType.IMMEDIATE_SALE,
                response.type());

        assertEquals(
                "Lucas Cepeda",
                response.playerName());

        assertEquals(
                34L,
                response.fromManagerId());

        assertEquals(
                "AC LEO",
                response.fromManagerName());

        assertNull(response.toManagerId());
        assertNull(response.toManagerName());

        assertEquals(
                75_000L,
                response.amount());

        assertEquals(
                0,
                response.bids().size());
    }

    @Test
    void findAllShouldReturnMovementWithoutManagers() {
        League league = createLeague();

        Player player = createPlayer(
                35L,
                "12345",
                "Jugador sin managers");

        Movement movement = new Movement(
                "1|1786200000|12345|TRANSFER|null|null|100000",
                MovementType.TRANSFER,
                player,
                null,
                null,
                100_000L,
                LocalDateTime.of(
                        2026,
                        8,
                        8,
                        20,
                        0),
                league);

        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                movementRepository
                        .findAllByLeague_IdOrderByOccurredAtDesc(
                                LEAGUE_ID))
                .thenReturn(List.of(movement));

        MovementResponse response = movementService
                .findAll(LEAGUE_ID)
                .get(0);

        assertNull(response.fromManagerId());
        assertNull(response.fromManagerName());
        assertNull(response.toManagerId());
        assertNull(response.toManagerName());
        assertEquals(0, response.bids().size());
    }

    @Test
    void findAllShouldReturnEmptyListWhenThereAreNoMovements() {
        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(true);

        when(
                movementRepository
                        .findAllByLeague_IdOrderByOccurredAtDesc(
                                LEAGUE_ID))
                .thenReturn(List.of());

        List<MovementResponse> result = movementService.findAll(LEAGUE_ID);

        assertEquals(0, result.size());
    }

    @Test
    void findAllShouldThrowWhenLeagueDoesNotExist() {
        when(leagueRepository.existsById(LEAGUE_ID))
                .thenReturn(false);

        assertThrows(
                LeagueNotFoundException.class,
                () -> movementService.findAll(LEAGUE_ID));

        verify(
                movementRepository,
                never()).findAllByLeague_IdOrderByOccurredAtDesc(
                        LEAGUE_ID);
    }

    private void mockLeaguePlayersAndManagers(
            League league,
            List<Player> players,
            List<Manager> managers) {
        when(leagueRepository.findById(LEAGUE_ID))
                .thenReturn(Optional.of(league));

        when(playerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(players);

        when(managerRepository.findAllByLeague_Id(LEAGUE_ID))
                .thenReturn(managers);
    }

    private BiwengerHomeResponse createHomeResponse(
            String eventType,
            Long date,
            List<BiwengerMovementItem> items) {
        JsonNode content = objectMapper.valueToTree(items);

        BiwengerBoardEvent event = new BiwengerBoardEvent(
                eventType,
                content,
                date);

        return createHomeResponse(
                List.of(event));
    }

    private BiwengerHomeResponse createHomeResponse(
            List<BiwengerBoardEvent> events) {
        BiwengerHomeLeague externalLeague = new BiwengerHomeLeague(
                1_268_640L,
                "VII Güenguer",
                events);

        BiwengerHomeData data = new BiwengerHomeData(
                externalLeague);

        return new BiwengerHomeResponse(
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

    private Player createPlayer(
            Long id,
            String biwengerPlayerId,
            String name) {
        Player player = new Player(
                biwengerPlayerId,
                name,
                List.of(PlayerPosition.DL),
                "Equipo",
                1_000_000L,
                createLeague());

        ReflectionTestUtils.setField(
                player,
                "id",
                id);

        return player;
    }

    private Manager createManager(
            Long id,
            Long biwengerManagerId,
            String name) {
        Manager manager = new Manager(
                biwengerManagerId,
                name,
                null,
                0,
                15,
                50_000_000L,
                100_000L,
                1,
                "manager",
                createLeague());

        ReflectionTestUtils.setField(
                manager,
                "id",
                id);

        return manager;
    }
}