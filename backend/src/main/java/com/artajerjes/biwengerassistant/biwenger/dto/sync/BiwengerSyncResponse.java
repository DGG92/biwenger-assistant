package com.artajerjes.biwengerassistant.biwenger.dto.sync;

import com.artajerjes.biwengerassistant.market.dto.MarketSyncResponse;
import com.artajerjes.biwengerassistant.movement.dto.MovementSyncResponse;
import com.artajerjes.biwengerassistant.offer.dto.OfferSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerLineupSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerOwnershipSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerSyncResponse;

public record BiwengerSyncResponse(
                PlayerSyncResponse players,
                PlayerOwnershipSyncResponse ownership,
                MarketSyncResponse market,
                OfferSyncResponse offers,
                MovementSyncResponse movements,
                PlayerLineupSyncResponse lineup) {
}