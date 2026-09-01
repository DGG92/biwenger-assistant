package com.artajerjes.biwengerassistant.history;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.artajerjes.biwengerassistant.market.MarketListingType;

public interface MarketListingSnapshotRepository
        extends JpaRepository<MarketListingSnapshot, Long> {

    Optional<MarketListingSnapshot> findByLeagueIdAndPlayerIdAndTypeAndPublishedAt(
            Long leagueId,
            Long playerId,
            MarketListingType type,
            LocalDateTime publishedAt);

    List<MarketListingSnapshot> findAllByLeagueIdOrderByPublishedAtDesc(
            Long leagueId);

    List<MarketListingSnapshot> findAllByPlayerIdOrderByPublishedAtDesc(
            Long playerId);
}