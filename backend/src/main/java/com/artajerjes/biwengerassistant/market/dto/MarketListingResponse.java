package com.artajerjes.biwengerassistant.market.dto;

import java.time.LocalDateTime;

import com.artajerjes.biwengerassistant.market.MarketListingType;

public record MarketListingResponse(
        Long id,
        MarketListingType type,
        Long playerId,
        String biwengerPlayerId,
        String playerName,
        String teamName,
        Long marketValue,
        Long askingPrice,
        Long sellerId,
        String sellerName,
        LocalDateTime publishedAt,
        LocalDateTime expiresAt,
        boolean extended,
        Long lastBidAmount,
        String lastBidStatus,
        Long lastBidManagerId,
        String lastBidManagerName) {
}