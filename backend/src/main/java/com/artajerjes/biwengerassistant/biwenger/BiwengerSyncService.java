package com.artajerjes.biwengerassistant.biwenger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.biwenger.dto.sync.BiwengerSyncResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.sync.PlayerDetailSyncResponse;
import com.artajerjes.biwengerassistant.history.MarketListingSnapshotService;
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

@Service
public class BiwengerSyncService {

        private static final Logger log = LoggerFactory.getLogger(BiwengerSyncService.class);

        private final Set<Long> leaguesBeingSynced = ConcurrentHashMap.newKeySet();

        private final PlayerService playerService;
        private final MarketService marketService;
        private final OfferService offerService;
        private final MovementService movementService;
        private final ManagerService managerService;
        private final MatchdayContextService matchdayContextService;
        private final MatchdayRoundSyncService matchdayRoundSyncService;
        private final PlayerSnapshotService playerSnapshotService;
        private final MarketListingSnapshotService marketListingSnapshotService;
        private final PlayerDetailSyncService playerDetailSyncService;

        public BiwengerSyncService(
                        PlayerService playerService,
                        MarketService marketService,
                        OfferService offerService,
                        MovementService movementService,
                        ManagerService managerService,
                        MatchdayContextService matchdayContextService,
                        MatchdayRoundSyncService matchdayRoundSyncService,
                        PlayerSnapshotService playerSnapshotService,
                        MarketListingSnapshotService marketListingSnapshotService,
                        PlayerDetailSyncService playerDetailSyncService) {

                this.playerService = playerService;
                this.marketService = marketService;
                this.offerService = offerService;
                this.movementService = movementService;
                this.managerService = managerService;
                this.matchdayContextService = matchdayContextService;
                this.matchdayRoundSyncService = matchdayRoundSyncService;
                this.playerSnapshotService = playerSnapshotService;
                this.marketListingSnapshotService = marketListingSnapshotService;
                this.playerDetailSyncService = playerDetailSyncService;
        }

        public BiwengerSyncResponse syncAll(Long leagueId) {

                if (!leaguesBeingSynced.add(leagueId)) {
                        throw new IllegalStateException(
                                        "League " + leagueId + " is already being synchronized");
                }

                long startedAt = System.currentTimeMillis();

                try {
                        log.info("Starting Biwenger sync for league {}", leagueId);

                        ManagerSyncResponse managers = managerService.sync(leagueId);
                        log.info("Managers synced for league {}", leagueId);

                        log.info("Syncing competition players for league {}", leagueId);
                        PlayerSyncResponse players = playerService.syncCompetitionPlayers(leagueId);
                        log.info("Competition players synced for league {}", leagueId);

                        log.info("Syncing player ownership for league {}", leagueId);
                        PlayerOwnershipSyncResponse ownership = playerService.syncPlayerOwnership(leagueId);
                        log.info("Player ownership synced for league {}", leagueId);

                        log.info("Capturing player snapshots for league {}", leagueId);
                        int snapshots = playerSnapshotService.captureDailySnapshots(leagueId);
                        log.info(
                                        "Player snapshots captured for league {}: {}",
                                        leagueId,
                                        snapshots);

                        log.info("Syncing market for league {}", leagueId);
                        MarketSyncResponse market = marketService.sync(leagueId);
                        log.info("Market synced for league {}", leagueId);

                        log.info("Capturing market listing snapshots for league {}", leagueId);
                        int marketListingSnapshots = marketListingSnapshotService.captureSnapshots(leagueId);
                        log.info("Market listing snapshots captured for league {}: {}", leagueId,
                                        marketListingSnapshots);

                        log.info("Syncing movements for league {}", leagueId);
                        MovementSyncResponse movements = movementService.sync(leagueId);
                        log.info("Movements synced for league {}", leagueId);

                        log.info("Syncing current lineup for league {}", leagueId);
                        PlayerLineupSyncResponse lineup = playerService.syncCurrentLineup(leagueId);
                        log.info("Current lineup synced for league {}", leagueId);

                        log.info("Syncing matchday context for league {}", leagueId);
                        matchdayContextService.syncCurrentMatchday(leagueId);
                        log.info("Matchday context synced for league {}", leagueId);

                        log.info("Syncing matchday round data for league {}", leagueId);

                        MatchdayRoundSyncResult matchdayRound = matchdayRoundSyncService
                                        .syncCurrentMatchday(leagueId);

                        log.info(
                                        "Matchday round data synced for league {}: games={}, teamStandings={}",
                                        leagueId,
                                        matchdayRound.games(),
                                        matchdayRound.teamStandings());

                        log.info("Syncing offers for league {}", leagueId);
                        OfferSyncResponse offers = offerService.sync(leagueId);
                        log.info("Offers synced for league {}", leagueId);

                        long elapsed = System.currentTimeMillis() - startedAt;

                        log.info(
                                        "Biwenger sync completed for league {} in {} ms",
                                        leagueId,
                                        elapsed);

                        return new BiwengerSyncResponse(
                                        managers,
                                        players,
                                        ownership,
                                        market,
                                        offers,
                                        movements,
                                        lineup);

                } catch (Exception exception) {

                        long elapsed = System.currentTimeMillis() - startedAt;

                        log.error(
                                        "Biwenger sync failed for league {} after {} ms",
                                        leagueId,
                                        elapsed,
                                        exception);

                        throw exception;

                } finally {
                        leaguesBeingSynced.remove(leagueId);
                }
        }

        public void syncScheduled(Long leagueId) {

                if (!leaguesBeingSynced.add(leagueId)) {
                        log.warn(
                                        "Skipping scheduled Biwenger sync for league {} because another sync is already running",
                                        leagueId);
                        return;
                }

                long startedAt = System.currentTimeMillis();

                try {
                        log.info(
                                        "Starting scheduled Biwenger sync for league {}",
                                        leagueId);

                        /*
                         * Datos base.
                         *
                         * Si cualquiera de estas fases falla, no continuamos.
                         * Las siguientes sincronizaciones dependen de que managers,
                         * jugadores y propietarios estén correctamente actualizados.
                         */
                        managerService.sync(leagueId);
                        log.info(
                                        "Managers synced for league {}",
                                        leagueId);

                        playerService.syncCompetitionPlayers(leagueId);
                        log.info(
                                        "Competition players synced for league {}",
                                        leagueId);

                        playerService.syncPlayerOwnership(leagueId);
                        log.info(
                                        "Player ownership synced for league {}",
                                        leagueId);

                        playerSnapshotService.captureDailySnapshots(leagueId);
                        log.info(
                                        "Player snapshots captured for league {}",
                                        leagueId);

                        /*
                         * Datos independientes.
                         *
                         * Un fallo puntual en una de estas fases no debe impedir
                         * que las demás se actualicen.
                         */
                        runScheduledPhase(
                                        leagueId,
                                        "market",
                                        () -> {
                                                marketService.sync(leagueId);

                                                int marketListingSnapshots = marketListingSnapshotService
                                                                .captureSnapshots(leagueId);

                                                log.info(
                                                                "Scheduled market listing snapshots captured for league {}: {}",
                                                                leagueId,
                                                                marketListingSnapshots);
                                        });

                        runScheduledPhase(
                                        leagueId,
                                        "movements",
                                        () -> movementService.sync(leagueId));

                        runScheduledPhase(
                                        leagueId,
                                        "current lineup",
                                        () -> playerService.syncCurrentLineup(leagueId));

                        runScheduledPhase(
                                        leagueId,
                                        "matchday context",
                                        () -> matchdayContextService.syncCurrentMatchday(leagueId));

                        runScheduledPhase(
                                        leagueId,
                                        "matchday round data",
                                        () -> matchdayRoundSyncService.syncCurrentMatchday(leagueId));

                        runScheduledPhase(
                                        leagueId,
                                        "offers",
                                        () -> offerService.sync(leagueId));

                        runScheduledPhase(
                                        leagueId,
                                        "player details",
                                        () -> syncPlayerDetailsBatch(leagueId));

                        long elapsed = System.currentTimeMillis() - startedAt;

                        log.info(
                                        "Scheduled Biwenger sync completed for league {} in {} ms",
                                        leagueId,
                                        elapsed);

                } catch (Exception exception) {

                        long elapsed = System.currentTimeMillis() - startedAt;

                        log.error(
                                        "Scheduled Biwenger sync aborted for league {} during base synchronization after {} ms",
                                        leagueId,
                                        elapsed,
                                        exception);

                } finally {
                        leaguesBeingSynced.remove(leagueId);
                }
        }

        private void runScheduledPhase(
                        Long leagueId,
                        String phaseName,
                        Runnable phase) {

                long startedAt = System.currentTimeMillis();

                try {
                        log.info(
                                        "Starting scheduled {} sync for league {}",
                                        phaseName,
                                        leagueId);

                        phase.run();

                        long elapsed = System.currentTimeMillis() - startedAt;

                        log.info(
                                        "Scheduled {} sync completed for league {} in {} ms",
                                        phaseName,
                                        leagueId,
                                        elapsed);

                } catch (Exception exception) {

                        long elapsed = System.currentTimeMillis() - startedAt;

                        log.error(
                                        "Scheduled {} sync failed for league {} after {} ms. Continuing with remaining phases.",
                                        phaseName,
                                        leagueId,
                                        elapsed,
                                        exception);
                }
        }

        private void syncPlayerDetailsBatch(
                        Long leagueId) {

                PlayerDetailSyncResponse result = playerDetailSyncService
                                .syncLeaguePlayerDetails(
                                                leagueId);

                if (result.completed()) {

                        log.info(
                                        "Player details batch completed for league {}: attempted={}, completed={}, pricesProcessed={}, reportsProcessed={}, eligible={}",
                                        leagueId,
                                        result.playersAttempted(),
                                        result.playersCompleted(),
                                        result.pricesProcessed(),
                                        result.reportsProcessed(),
                                        result.playersEligible());

                        return;
                }

                if ("RATE_LIMIT".equals(
                                result.stopReason())) {

                        log.warn(
                                        "Player details batch stopped by Biwenger rate limit for league {}: attempted={}, completed={}, pricesProcessed={}, reportsProcessed={}, rateLimitedPlayerId={}",
                                        leagueId,
                                        result.playersAttempted(),
                                        result.playersCompleted(),
                                        result.pricesProcessed(),
                                        result.reportsProcessed(),
                                        result.rateLimitedPlayerId());

                        return;
                }

                log.warn(
                                "Player details batch finished partially for league {}: attempted={}, completed={}, pricesProcessed={}, reportsProcessed={}, stopReason={}",
                                leagueId,
                                result.playersAttempted(),
                                result.playersCompleted(),
                                result.pricesProcessed(),
                                result.reportsProcessed(),
                                result.stopReason());
        }
}