package com.artajerjes.biwengerassistant.market;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketListingRepository
        extends JpaRepository<MarketListing, Long> {

    List<MarketListing> findAllByLeague_Id(Long leagueId);

    void deleteAllByLeague_Id(Long leagueId);
}