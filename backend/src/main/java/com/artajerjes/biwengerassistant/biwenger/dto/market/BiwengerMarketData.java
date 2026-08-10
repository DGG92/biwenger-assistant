package com.artajerjes.biwengerassistant.biwenger.dto.market;

import java.util.List;

public record BiwengerMarketData(
                BiwengerMarketStatus status,
                List<BiwengerMarketListing> sales,
                List<BiwengerMarketOffer> offers,
                List<BiwengerMarketListing> auctions) {
}