package com.artajerjes.biwengerassistant.biwenger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.biwenger.dto.sync.BiwengerSyncResponse;
import com.artajerjes.biwengerassistant.manager.ManagerService;
import com.artajerjes.biwengerassistant.manager.dto.ManagerSyncResponse;
import com.artajerjes.biwengerassistant.market.MarketService;
import com.artajerjes.biwengerassistant.market.dto.MarketSyncResponse;
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

    public BiwengerSyncService(
            PlayerService playerService,
            MarketService marketService,
            OfferService offerService,
            MovementService movementService,
            ManagerService managerService) {

        this.playerService = playerService;
        this.marketService = marketService;
        this.offerService = offerService;
        this.movementService = movementService;
        this.managerService = managerService;
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

            log.info("Syncing market for league {}", leagueId);
            MarketSyncResponse market = marketService.sync(leagueId);
            log.info("Market synced for league {}", leagueId);

            log.info("Syncing movements for league {}", leagueId);
            MovementSyncResponse movements = movementService.sync(leagueId);
            log.info("Movements synced for league {}", leagueId);

            log.info("Syncing current lineup for league {}", leagueId);
            PlayerLineupSyncResponse lineup = playerService.syncCurrentLineup(leagueId);
            log.info("Current lineup synced for league {}", leagueId);

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
}