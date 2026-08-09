package com.artajerjes.biwengerassistant.biwenger.dto.market;

public record BiwengerMarketListing(
        Long date,
        Long until,
        Boolean extended,
        Long price,
        BiwengerMarketPlayer player,
        BiwengerMarketUser user,
        BiwengerMarketLastBid lastBid) {
}