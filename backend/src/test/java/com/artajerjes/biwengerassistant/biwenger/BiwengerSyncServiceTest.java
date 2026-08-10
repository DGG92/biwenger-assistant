package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.biwenger.dto.sync.BiwengerSyncResponse;
import com.artajerjes.biwengerassistant.market.MarketService;
import com.artajerjes.biwengerassistant.market.dto.MarketSyncResponse;
import com.artajerjes.biwengerassistant.movement.MovementService;
import com.artajerjes.biwengerassistant.movement.dto.MovementSyncResponse;
import com.artajerjes.biwengerassistant.player.PlayerService;
import com.artajerjes.biwengerassistant.player.dto.PlayerLineupSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerOwnershipSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerSyncResponse;

@ExtendWith(MockitoExtension.class)
class BiwengerSyncServiceTest {

        private static final Long LEAGUE_ID = 1L;

        @Mock
        private PlayerService playerService;

        @Mock
        private MarketService marketService;

        @Mock
        private MovementService movementService;

        @InjectMocks
        private BiwengerSyncService biwengerSyncService;

        @Test
        void syncAllShouldExecuteAllSyncsInCorrectOrder() {
                PlayerSyncResponse players = new PlayerSyncResponse(
                                555,
                                0,
                                555,
                                0);

                PlayerOwnershipSyncResponse ownership = new PlayerOwnershipSyncResponse(
                                14,
                                211,
                                0);

                MarketSyncResponse market = new MarketSyncResponse(
                                54,
                                10,
                                0,
                                0);

                MovementSyncResponse movements = new MovementSyncResponse(
                                12,
                                0,
                                12,
                                0,
                                0);

                PlayerLineupSyncResponse lineup = new PlayerLineupSyncResponse(
                                13L,
                                "4-5-1",
                                38405L,
                                17756L,
                                41088L);

                when(
                                playerService.syncCompetitionPlayers(
                                                LEAGUE_ID))
                                .thenReturn(players);

                when(
                                playerService.syncPlayerOwnership(
                                                LEAGUE_ID))
                                .thenReturn(ownership);

                when(
                                marketService.sync(
                                                LEAGUE_ID))
                                .thenReturn(market);

                when(
                                movementService.sync(
                                                LEAGUE_ID))
                                .thenReturn(movements);

                when(
                                playerService.syncCurrentLineup(
                                                LEAGUE_ID))
                                .thenReturn(lineup);

                biwengerSyncService.syncAll(LEAGUE_ID);

                InOrder inOrder = inOrder(
                                playerService,
                                marketService,
                                movementService);

                inOrder.verify(playerService)
                                .syncCompetitionPlayers(LEAGUE_ID);

                inOrder.verify(playerService)
                                .syncPlayerOwnership(LEAGUE_ID);

                inOrder.verify(marketService)
                                .sync(LEAGUE_ID);

                inOrder.verify(movementService)
                                .sync(LEAGUE_ID);

                inOrder.verify(playerService)
                                .syncCurrentLineup(LEAGUE_ID);
        }

        @Test
        void syncAllShouldReturnResponsesFromEverySync() {
                PlayerSyncResponse players = new PlayerSyncResponse(
                                555,
                                0,
                                555,
                                0);

                PlayerOwnershipSyncResponse ownership = new PlayerOwnershipSyncResponse(
                                14,
                                211,
                                0);

                MarketSyncResponse market = new MarketSyncResponse(
                                54,
                                10,
                                0,
                                0);

                MovementSyncResponse movements = new MovementSyncResponse(
                                12,
                                0,
                                12,
                                0,
                                0);

                PlayerLineupSyncResponse lineup = new PlayerLineupSyncResponse(
                                13L,
                                "4-5-1",
                                38405L,
                                17756L,
                                41088L);

                when(
                                playerService.syncCompetitionPlayers(
                                                LEAGUE_ID))
                                .thenReturn(players);

                when(
                                playerService.syncPlayerOwnership(
                                                LEAGUE_ID))
                                .thenReturn(ownership);

                when(
                                marketService.sync(
                                                LEAGUE_ID))
                                .thenReturn(market);

                when(
                                movementService.sync(
                                                LEAGUE_ID))
                                .thenReturn(movements);

                when(
                                playerService.syncCurrentLineup(
                                                LEAGUE_ID))
                                .thenReturn(lineup);

                BiwengerSyncResponse result = biwengerSyncService.syncAll(
                                LEAGUE_ID);

                assertSame(
                                players,
                                result.players());

                assertSame(
                                ownership,
                                result.ownership());

                assertSame(
                                market,
                                result.market());

                assertSame(
                                movements,
                                result.movements());

                assertSame(
                                lineup,
                                result.lineup());

                assertEquals(
                                555,
                                result.players().total());

                assertEquals(
                                211,
                                result.ownership().playersAssigned());

                assertEquals(
                                54,
                                result.market().sales());

                assertEquals(
                                12,
                                result.movements().processed());

                assertEquals(
                                "4-5-1",
                                result.lineup().formation());
        }

        @Test
        void syncAllShouldUseProvidedLeagueIdForEverySync() {
                Long customLeagueId = 42L;

                PlayerSyncResponse players = new PlayerSyncResponse(
                                0,
                                0,
                                0,
                                0);

                PlayerOwnershipSyncResponse ownership = new PlayerOwnershipSyncResponse(
                                0,
                                0,
                                0);

                MarketSyncResponse market = new MarketSyncResponse(
                                0,
                                0,
                                0,
                                0);

                MovementSyncResponse movements = new MovementSyncResponse(
                                0,
                                0,
                                0,
                                0,
                                0);

                PlayerLineupSyncResponse lineup = new PlayerLineupSyncResponse(
                                null,
                                null,
                                null,
                                null,
                                null);

                when(
                                playerService.syncCompetitionPlayers(
                                                customLeagueId))
                                .thenReturn(players);

                when(
                                playerService.syncPlayerOwnership(
                                                customLeagueId))
                                .thenReturn(ownership);

                when(
                                marketService.sync(
                                                customLeagueId))
                                .thenReturn(market);

                when(
                                movementService.sync(
                                                customLeagueId))
                                .thenReturn(movements);

                when(
                                playerService.syncCurrentLineup(
                                                customLeagueId))
                                .thenReturn(lineup);

                biwengerSyncService.syncAll(
                                customLeagueId);

                verify(playerService)
                                .syncCompetitionPlayers(
                                                customLeagueId);

                verify(playerService)
                                .syncPlayerOwnership(
                                                customLeagueId);

                verify(marketService)
                                .sync(
                                                customLeagueId);

                verify(movementService)
                                .sync(
                                                customLeagueId);

                verify(playerService)
                                .syncCurrentLineup(
                                                customLeagueId);
        }

        @Test
        void syncAllShouldStopWhenOneSyncFails() {
                PlayerSyncResponse players = new PlayerSyncResponse(
                                555,
                                0,
                                555,
                                0);

                when(
                                playerService.syncCompetitionPlayers(
                                                LEAGUE_ID))
                                .thenReturn(players);

                when(
                                playerService.syncPlayerOwnership(
                                                LEAGUE_ID))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Ownership sync failed"));

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> biwengerSyncService.syncAll(
                                                LEAGUE_ID));

                assertEquals(
                                "Ownership sync failed",
                                exception.getMessage());

                verify(playerService)
                                .syncCompetitionPlayers(
                                                LEAGUE_ID);

                verify(playerService)
                                .syncPlayerOwnership(
                                                LEAGUE_ID);

                verify(
                                marketService,
                                never()).sync(LEAGUE_ID);

                verify(
                                movementService,
                                never()).sync(LEAGUE_ID);

                verify(
                                playerService,
                                never()).syncCurrentLineup(
                                                LEAGUE_ID);
        }
}