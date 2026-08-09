package com.artajerjes.biwengerassistant.market.dto;

public record MarketSyncResponse(
        int sales,
        int auctions,
        int playersNotFound,
        int managersNotFound) {
}