package com.artajerjes.biwengerassistant.recommendation.dto;

import java.util.List;

import com.artajerjes.biwengerassistant.market.MarketListingType;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.recommendation.RecommendationType;

public record MarketRecommendationResponse(
                Long playerId,
                String biwengerPlayerId,
                String playerName,
                String teamName,
                List<PlayerPosition> positions,
                MarketListingType marketType,
                Long marketValue,
                Long askingPrice,
                Long currentBid,
                Long maximumRecommendedBid,
                Long priceDifference,
                double priceDifferencePercentage,
                Long valueFluctuation,
                int points,
                boolean injured,
                boolean affordable,
                int score,
                RecommendationType recommendation,
                Long sellerId,
                String sellerName) {
}