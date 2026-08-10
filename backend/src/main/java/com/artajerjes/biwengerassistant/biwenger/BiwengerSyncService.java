package com.artajerjes.biwengerassistant.biwenger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.market.MarketService;
import com.artajerjes.biwengerassistant.market.dto.MarketSyncResponse;
import com.artajerjes.biwengerassistant.movement.MovementService;
import com.artajerjes.biwengerassistant.movement.dto.MovementSyncResponse;
import com.artajerjes.biwengerassistant.player.PlayerService;
import com.artajerjes.biwengerassistant.player.dto.PlayerLineupSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerOwnershipSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerSyncResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.sync.BiwengerSyncResponse;

@Service
public class BiwengerSyncService {

    private final PlayerService playerService;
    private final MarketService marketService;
    private final MovementService movementService;

    public BiwengerSyncService(
            PlayerService playerService,
            MarketService marketService,
            MovementService movementService) {
        this.playerService = playerService;
        this.marketService = marketService;
        this.movementService = movementService;
    }

    @Transactional
    public BiwengerSyncResponse syncAll(Long leagueId) {
        PlayerSyncResponse players = playerService.syncCompetitionPlayers(leagueId);

        PlayerOwnershipSyncResponse ownership = playerService.syncPlayerOwnership(leagueId);

        MarketSyncResponse market = marketService.sync(leagueId);

        MovementSyncResponse movements = movementService.sync(leagueId);

        PlayerLineupSyncResponse lineup = playerService.syncCurrentLineup(leagueId);

        return new BiwengerSyncResponse(
                players,
                ownership,
                market,
                movements,
                lineup);
    }
}