package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
import com.artajerjes.biwengerassistant.history.PlayerSnapshotService;
import com.artajerjes.biwengerassistant.manager.ManagerService;
import com.artajerjes.biwengerassistant.manager.dto.ManagerSyncResponse;
import com.artajerjes.biwengerassistant.market.MarketService;
import com.artajerjes.biwengerassistant.market.dto.MarketSyncResponse;
import com.artajerjes.biwengerassistant.matchday.MatchdayContextService;
import com.artajerjes.biwengerassistant.matchday.MatchdayRoundSyncResult;
import com.artajerjes.biwengerassistant.matchday.MatchdayRoundSyncService;
import com.artajerjes.biwengerassistant.movement.MovementService;
import com.artajerjes.biwengerassistant.movement.dto.MovementSyncResponse;
import com.artajerjes.biwengerassistant.offer.OfferService;
import com.artajerjes.biwengerassistant.offer.dto.OfferSyncResponse;
import com.artajerjes.biwengerassistant.player.PlayerService;
import com.artajerjes.biwengerassistant.player.dto.PlayerLineupSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerOwnershipSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerSyncResponse;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportService;
import com.artajerjes.biwengerassistant.playerreport.dto.PlayerReportSyncResponse;

@ExtendWith(MockitoExtension.class)
class BiwengerSyncServiceTest {

        private static final Long LEAGUE_ID = 1L;

        @Mock
        private PlayerService playerService;

        @Mock
        private MarketService marketService;

        @Mock
        private PlayerSnapshotService playerSnapshotService;

        @Mock
        private OfferService offerService;

        @Mock
        private MovementService movementService;

        @Mock
        private ManagerService managerService;

        @Mock
        private MatchdayContextService matchdayContextService;

        @Mock
        private MatchdayRoundSyncService matchdayRoundSyncService;

        @Mock
        private PlayerMatchReportService playerMatchReportService;

        @InjectMocks
        private BiwengerSyncService biwengerSyncService;

        private void mockMatchdayRoundSync(Long leagueId) {

                when(matchdayRoundSyncService.syncCurrentMatchday(leagueId))
                                .thenReturn(
                                                new MatchdayRoundSyncResult(
                                                                10,
                                                                20));
        }

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

                OfferSyncResponse offers = new OfferSyncResponse(
                                1,
                                1,
                                0,
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
                                offerService.sync(
                                                LEAGUE_ID))
                                .thenReturn(offers);

                when(
                                movementService.sync(
                                                LEAGUE_ID))
                                .thenReturn(movements);

                when(
                                playerService.syncCurrentLineup(
                                                LEAGUE_ID))
                                .thenReturn(lineup);

                mockMatchdayRoundSync(LEAGUE_ID);

                biwengerSyncService.syncAll(LEAGUE_ID);

                InOrder inOrder = inOrder(
                                playerService,
                                managerService,
                                playerSnapshotService,
                                marketService,
                                offerService,
                                movementService,
                                matchdayContextService,
                                matchdayRoundSyncService);

                inOrder.verify(managerService)
                                .sync(LEAGUE_ID);

                inOrder.verify(playerService)
                                .syncCompetitionPlayers(LEAGUE_ID);

                inOrder.verify(playerService)
                                .syncPlayerOwnership(LEAGUE_ID);

                inOrder.verify(playerSnapshotService)
                                .captureDailySnapshots(LEAGUE_ID);

                inOrder.verify(marketService)
                                .sync(LEAGUE_ID);

                inOrder.verify(movementService)
                                .sync(LEAGUE_ID);

                inOrder.verify(playerService)
                                .syncCurrentLineup(LEAGUE_ID);

                inOrder.verify(matchdayContextService)
                                .syncCurrentMatchday(LEAGUE_ID);

                inOrder.verify(matchdayRoundSyncService)
                                .syncCurrentMatchday(LEAGUE_ID);

                inOrder.verify(offerService)
                                .sync(LEAGUE_ID);
        }

        @Test
        void syncAllShouldReturnResponsesFromEverySync() {
                ManagerSyncResponse managers = new ManagerSyncResponse(
                                14,
                                0,
                                14);

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

                OfferSyncResponse offers = new OfferSyncResponse(
                                1,
                                1,
                                0,
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

                when(managerService.sync(LEAGUE_ID))
                                .thenReturn(managers);

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
                                offerService.sync(
                                                LEAGUE_ID))
                                .thenReturn(offers);

                when(
                                movementService.sync(
                                                LEAGUE_ID))
                                .thenReturn(movements);

                when(
                                playerService.syncCurrentLineup(
                                                LEAGUE_ID))
                                .thenReturn(lineup);

                mockMatchdayRoundSync(LEAGUE_ID);

                BiwengerSyncResponse result = biwengerSyncService.syncAll(
                                LEAGUE_ID);

                assertSame(
                                managers,
                                result.managers());

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
                                offers,
                                result.offers());

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
                                1,
                                result.offers().total());

                assertEquals(
                                12,
                                result.movements().processed());

                assertEquals(
                                "4-5-1",
                                result.lineup().formation());

                assertEquals(14, result.managers().total());
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

                OfferSyncResponse offers = new OfferSyncResponse(
                                0,
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
                                offerService.sync(
                                                customLeagueId))
                                .thenReturn(offers);

                when(
                                movementService.sync(
                                                customLeagueId))
                                .thenReturn(movements);

                when(
                                playerService.syncCurrentLineup(
                                                customLeagueId))
                                .thenReturn(lineup);

                mockMatchdayRoundSync(customLeagueId);

                biwengerSyncService.syncAll(
                                customLeagueId);

                verify(playerService)
                                .syncCompetitionPlayers(
                                                customLeagueId);

                verify(managerService)
                                .sync(customLeagueId);

                verify(playerService)
                                .syncPlayerOwnership(
                                                customLeagueId);

                verify(playerSnapshotService)
                                .captureDailySnapshots(customLeagueId);

                verify(marketService)
                                .sync(
                                                customLeagueId);

                verify(offerService)
                                .sync(
                                                customLeagueId);

                verify(movementService)
                                .sync(
                                                customLeagueId);

                verify(playerService)
                                .syncCurrentLineup(
                                                customLeagueId);

                verify(matchdayContextService)
                                .syncCurrentMatchday(customLeagueId);

                verify(matchdayRoundSyncService)
                                .syncCurrentMatchday(customLeagueId);
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

                verify(managerService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncPlayerOwnership(
                                                LEAGUE_ID);

                verify(
                                playerSnapshotService,
                                never())
                                .captureDailySnapshots(
                                                LEAGUE_ID);

                verify(
                                marketService,
                                never()).sync(LEAGUE_ID);

                verify(
                                offerService,
                                never()).sync(LEAGUE_ID);

                verify(
                                movementService,
                                never()).sync(LEAGUE_ID);

                verify(
                                playerService,
                                never()).syncCurrentLineup(
                                                LEAGUE_ID);

                verify(
                                matchdayContextService,
                                never()).syncCurrentMatchday(
                                                LEAGUE_ID);

                verify(
                                matchdayRoundSyncService,
                                never()).syncCurrentMatchday(
                                                LEAGUE_ID);
        }

        @Test
        void syncAllShouldStopWhenOfferSyncFails() {
                PlayerSyncResponse players = new PlayerSyncResponse(
                                555,
                                0,
                                555,
                                0);

                PlayerOwnershipSyncResponse ownership = new PlayerOwnershipSyncResponse(
                                14,
                                209,
                                0);

                MarketSyncResponse market = new MarketSyncResponse(
                                47,
                                10,
                                0,
                                0);

                MovementSyncResponse movements = new MovementSyncResponse(
                                17,
                                0,
                                17,
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

                when(
                                offerService.sync(
                                                LEAGUE_ID))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Offer sync failed"));

                mockMatchdayRoundSync(LEAGUE_ID);

                IllegalStateException exception = assertThrows(
                                IllegalStateException.class,
                                () -> biwengerSyncService.syncAll(
                                                LEAGUE_ID));

                assertEquals(
                                "Offer sync failed",
                                exception.getMessage());

                verify(playerService)
                                .syncCompetitionPlayers(LEAGUE_ID);

                verify(managerService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncPlayerOwnership(LEAGUE_ID);

                verify(marketService)
                                .sync(LEAGUE_ID);

                verify(movementService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncCurrentLineup(LEAGUE_ID);

                verify(matchdayContextService)
                                .syncCurrentMatchday(LEAGUE_ID);

                verify(matchdayRoundSyncService)
                                .syncCurrentMatchday(LEAGUE_ID);

                verify(offerService)
                                .sync(LEAGUE_ID);
        }

        @Test
        void syncScheduledShouldContinueWhenMarketSyncFails() {

                when(marketService.sync(LEAGUE_ID))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Market temporarily unavailable"));

                assertDoesNotThrow(
                                () -> biwengerSyncService.syncScheduled(
                                                LEAGUE_ID));

                verify(managerService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncCompetitionPlayers(LEAGUE_ID);

                verify(playerService)
                                .syncPlayerOwnership(LEAGUE_ID);

                verify(marketService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncCurrentLineup(LEAGUE_ID);

                verify(matchdayContextService)
                                .syncCurrentMatchday(LEAGUE_ID);

                verify(matchdayRoundSyncService)
                                .syncCurrentMatchday(LEAGUE_ID);

                verify(offerService)
                                .sync(LEAGUE_ID);

                verify(playerMatchReportService)
                                .syncLeagueReports(LEAGUE_ID);
        }

        @Test
        void syncScheduledShouldContinueWhenMovementSyncFails() {

                when(movementService.sync(LEAGUE_ID))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Movements temporarily unavailable"));

                assertDoesNotThrow(
                                () -> biwengerSyncService.syncScheduled(
                                                LEAGUE_ID));

                verify(managerService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncCompetitionPlayers(LEAGUE_ID);

                verify(playerService)
                                .syncPlayerOwnership(LEAGUE_ID);

                verify(marketService)
                                .sync(LEAGUE_ID);

                verify(movementService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncCurrentLineup(LEAGUE_ID);

                verify(matchdayContextService)
                                .syncCurrentMatchday(LEAGUE_ID);

                verify(matchdayRoundSyncService)
                                .syncCurrentMatchday(LEAGUE_ID);

                verify(offerService)
                                .sync(LEAGUE_ID);

                verify(playerMatchReportService)
                                .syncLeagueReports(LEAGUE_ID);
        }

        @Test
        void syncScheduledShouldAbortRemainingPhasesWhenPlayerSyncFails() {

                when(playerService.syncCompetitionPlayers(LEAGUE_ID))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Players unavailable"));

                assertDoesNotThrow(
                                () -> biwengerSyncService.syncScheduled(
                                                LEAGUE_ID));

                verify(managerService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncCompetitionPlayers(LEAGUE_ID);

                verify(
                                playerService,
                                never())
                                .syncPlayerOwnership(LEAGUE_ID);

                verify(
                                playerSnapshotService,
                                never())
                                .captureDailySnapshots(
                                                LEAGUE_ID);

                verify(
                                marketService,
                                never())
                                .sync(LEAGUE_ID);

                verify(
                                movementService,
                                never())
                                .sync(LEAGUE_ID);

                verify(
                                playerService,
                                never())
                                .syncCurrentLineup(LEAGUE_ID);

                verify(
                                offerService,
                                never())
                                .sync(LEAGUE_ID);

                verify(
                                playerMatchReportService,
                                never())
                                .syncLeagueReports(LEAGUE_ID);

                verify(
                                matchdayContextService,
                                never()).syncCurrentMatchday(
                                                LEAGUE_ID);

                verify(
                                matchdayRoundSyncService,
                                never()).syncCurrentMatchday(
                                                LEAGUE_ID);
        }

        @Test
        void syncScheduledShouldAbsorbOfferFailure() {

                when(offerService.sync(LEAGUE_ID))
                                .thenThrow(
                                                new IllegalStateException(
                                                                "Offers temporarily unavailable"));

                assertDoesNotThrow(
                                () -> biwengerSyncService.syncScheduled(
                                                LEAGUE_ID));

                verify(managerService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncCompetitionPlayers(LEAGUE_ID);

                verify(playerService)
                                .syncPlayerOwnership(LEAGUE_ID);

                verify(marketService)
                                .sync(LEAGUE_ID);

                verify(movementService)
                                .sync(LEAGUE_ID);

                verify(playerService)
                                .syncCurrentLineup(LEAGUE_ID);

                verify(matchdayContextService)
                                .syncCurrentMatchday(LEAGUE_ID);

                verify(matchdayRoundSyncService)
                                .syncCurrentMatchday(LEAGUE_ID);

                verify(offerService)
                                .sync(LEAGUE_ID);

                verify(playerMatchReportService)
                                .syncLeagueReports(LEAGUE_ID);
        }

        @Test
        void syncScheduledShouldExecutePlayerReportsBatch() {

                PlayerReportSyncResponse reports = new PlayerReportSyncResponse(
                                587,
                                586,
                                25,
                                25,
                                40,
                                true,
                                null,
                                29L,
                                null);

                when(playerMatchReportService.syncLeagueReports(LEAGUE_ID))
                                .thenReturn(reports);

                assertDoesNotThrow(
                                () -> biwengerSyncService.syncScheduled(
                                                LEAGUE_ID));

                verify(playerMatchReportService)
                                .syncLeagueReports(LEAGUE_ID);
        }

        @Test
        void syncScheduledShouldContinueWhenPlayerReportsHitRateLimit() {

                PlayerReportSyncResponse reports = new PlayerReportSyncResponse(
                                587,
                                586,
                                4,
                                3,
                                12,
                                false,
                                "RATE_LIMIT",
                                28L,
                                29L);

                when(playerMatchReportService.syncLeagueReports(LEAGUE_ID))
                                .thenReturn(reports);

                assertDoesNotThrow(
                                () -> biwengerSyncService.syncScheduled(
                                                LEAGUE_ID));

                verify(playerMatchReportService)
                                .syncLeagueReports(LEAGUE_ID);
        }

}