package com.artajerjes.biwengerassistant.history;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.market.MarketListing;
import com.artajerjes.biwengerassistant.market.MarketListingRepository;

@Service
public class MarketListingSnapshotService {

    private final MarketListingRepository marketListingRepository;
    private final MarketListingSnapshotRepository marketListingSnapshotRepository;

    public MarketListingSnapshotService(
            MarketListingRepository marketListingRepository,
            MarketListingSnapshotRepository marketListingSnapshotRepository) {

        this.marketListingRepository = marketListingRepository;
        this.marketListingSnapshotRepository = marketListingSnapshotRepository;
    }

    @Transactional
    public int captureSnapshots(
            Long leagueId) {

        LocalDateTime capturedAt = LocalDateTime.now();

        return captureSnapshots(
                leagueId,
                capturedAt);
    }

    int captureSnapshots(
            Long leagueId,
            LocalDateTime capturedAt) {

        List<MarketListing> listings = marketListingRepository
                .findAllByLeague_Id(leagueId);

        for (MarketListing listing : listings) {

            MarketListingSnapshot snapshot = marketListingSnapshotRepository
                    .findByLeagueIdAndPlayerIdAndTypeAndPublishedAt(
                            leagueId,
                            listing.getPlayer().getId(),
                            listing.getType(),
                            listing.getPublishedAt())
                    .orElseGet(
                            () -> new MarketListingSnapshot(
                                    listing,
                                    capturedAt));

            snapshot.updateFrom(
                    listing,
                    capturedAt);

            marketListingSnapshotRepository.save(snapshot);
        }

        return listings.size();
    }
}