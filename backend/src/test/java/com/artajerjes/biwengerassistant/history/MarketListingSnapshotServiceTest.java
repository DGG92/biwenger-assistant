package com.artajerjes.biwengerassistant.history;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.test.util.ReflectionTestUtils;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.market.MarketListing;
import com.artajerjes.biwengerassistant.market.MarketListingRepository;
import com.artajerjes.biwengerassistant.market.MarketListingType;
import com.artajerjes.biwengerassistant.player.Player;

class MarketListingSnapshotServiceTest {

        private MarketListingRepository marketListingRepository;
        private MarketListingSnapshotRepository marketListingSnapshotRepository;

        private MarketListingSnapshotService marketListingSnapshotService;

        @BeforeEach
        void setUp() {

                marketListingRepository = mock(MarketListingRepository.class);
                marketListingSnapshotRepository = mock(MarketListingSnapshotRepository.class);

                marketListingSnapshotService = new MarketListingSnapshotService(
                                marketListingRepository,
                                marketListingSnapshotRepository);
        }

        @Test
        void captureSnapshotsShouldCreateSnapshotWhenAppearanceDoesNotExist() {

                Long leagueId = 1L;

                LocalDateTime publishedAt = LocalDateTime.of(
                                2026,
                                9,
                                1,
                                10,
                                0);

                LocalDateTime capturedAt = LocalDateTime.of(
                                2026,
                                9,
                                1,
                                10,
                                15);

                MarketListing listing = createListing(
                                leagueId,
                                publishedAt,
                                5_000_000L,
                                4_500_000L);

                when(marketListingRepository.findAllByLeague_Id(leagueId))
                                .thenReturn(List.of(listing));

                when(marketListingSnapshotRepository
                                .findByLeagueIdAndPlayerIdAndTypeAndPublishedAt(
                                                leagueId,
                                                listing.getPlayer().getId(),
                                                listing.getType(),
                                                publishedAt))
                                .thenReturn(Optional.empty());

                int captured = marketListingSnapshotService.captureSnapshots(
                                leagueId,
                                capturedAt);

                ArgumentCaptor<MarketListingSnapshot> snapshotCaptor = ArgumentCaptor
                                .forClass(MarketListingSnapshot.class);

                verify(marketListingSnapshotRepository)
                                .save(snapshotCaptor.capture());

                MarketListingSnapshot snapshot = snapshotCaptor.getValue();

                assertEquals(1, captured);
                assertEquals(leagueId, snapshot.getLeagueId());
                assertEquals(listing.getPlayer().getId(), snapshot.getPlayerId());
                assertEquals(MarketListingType.SALE, snapshot.getType());
                assertEquals(5_000_000L, snapshot.getAskingPrice());
                assertEquals(4_500_000L, snapshot.getPlayerMarketValue());
                assertEquals(publishedAt, snapshot.getPublishedAt());
                assertEquals(capturedAt, snapshot.getFirstCapturedAt());
                assertEquals(capturedAt, snapshot.getLastCapturedAt());
        }

        @Test
        void captureSnapshotsShouldUpdateExistingSnapshotFromSameAppearance() {

                Long leagueId = 1L;

                LocalDateTime publishedAt = LocalDateTime.of(
                                2026,
                                9,
                                1,
                                10,
                                0);

                LocalDateTime firstCapturedAt = LocalDateTime.of(
                                2026,
                                9,
                                1,
                                10,
                                15);

                LocalDateTime secondCapturedAt = LocalDateTime.of(
                                2026,
                                9,
                                1,
                                11,
                                0);

                MarketListing firstListing = createListing(
                                leagueId,
                                publishedAt,
                                5_000_000L,
                                4_500_000L);

                MarketListingSnapshot existingSnapshot = new MarketListingSnapshot(
                                firstListing,
                                firstCapturedAt);

                MarketListing updatedListing = createListing(
                                leagueId,
                                publishedAt,
                                5_500_000L,
                                4_700_000L);

                when(marketListingRepository.findAllByLeague_Id(leagueId))
                                .thenReturn(List.of(updatedListing));

                when(marketListingSnapshotRepository
                                .findByLeagueIdAndPlayerIdAndTypeAndPublishedAt(
                                                leagueId,
                                                updatedListing.getPlayer().getId(),
                                                updatedListing.getType(),
                                                publishedAt))
                                .thenReturn(Optional.of(existingSnapshot));

                int captured = marketListingSnapshotService.captureSnapshots(
                                leagueId,
                                secondCapturedAt);

                assertEquals(1, captured);

                assertEquals(
                                firstCapturedAt,
                                existingSnapshot.getFirstCapturedAt());

                assertEquals(
                                secondCapturedAt,
                                existingSnapshot.getLastCapturedAt());

                assertEquals(
                                5_500_000L,
                                existingSnapshot.getAskingPrice());

                assertEquals(
                                4_500_000L,
                                existingSnapshot.getPlayerMarketValue());

                verify(marketListingSnapshotRepository)
                                .save(existingSnapshot);
        }

        @Test
        void captureSnapshotsShouldCreateDifferentSnapshotWhenPublishedAtChanges() {

                Long leagueId = 1L;

                LocalDateTime secondPublishedAt = LocalDateTime.of(
                                2026,
                                9,
                                2,
                                9,
                                0);

                LocalDateTime capturedAt = LocalDateTime.of(
                                2026,
                                9,
                                2,
                                9,
                                15);

                MarketListing listing = createListing(
                                leagueId,
                                secondPublishedAt,
                                5_000_000L,
                                4_500_000L);

                when(marketListingRepository.findAllByLeague_Id(leagueId))
                                .thenReturn(List.of(listing));

                when(marketListingSnapshotRepository
                                .findByLeagueIdAndPlayerIdAndTypeAndPublishedAt(
                                                leagueId,
                                                listing.getPlayer().getId(),
                                                listing.getType(),
                                                secondPublishedAt))
                                .thenReturn(Optional.empty());

                int captured = marketListingSnapshotService.captureSnapshots(
                                leagueId,
                                capturedAt);

                ArgumentCaptor<MarketListingSnapshot> snapshotCaptor = ArgumentCaptor
                                .forClass(MarketListingSnapshot.class);

                verify(marketListingSnapshotRepository)
                                .save(snapshotCaptor.capture());

                MarketListingSnapshot newSnapshot = snapshotCaptor.getValue();

                assertEquals(1, captured);
                assertEquals(secondPublishedAt, newSnapshot.getPublishedAt());
                assertEquals(capturedAt, newSnapshot.getFirstCapturedAt());
                assertEquals(capturedAt, newSnapshot.getLastCapturedAt());
        }

        private MarketListing createListing(
                        Long leagueId,
                        LocalDateTime publishedAt,
                        Long askingPrice,
                        Long marketValue) {

                League league = mock(League.class);
                when(league.getId()).thenReturn(leagueId);

                Manager seller = mock(Manager.class);
                when(seller.getId()).thenReturn(10L);

                Player player = mock(Player.class);
                when(player.getId()).thenReturn(100L);
                when(player.getMarketValue()).thenReturn(marketValue);

                MarketListing listing = new MarketListing(
                                MarketListingType.SALE,
                                player,
                                seller,
                                askingPrice,
                                publishedAt,
                                publishedAt.plusDays(2),
                                false,
                                null,
                                null,
                                null,
                                league);

                ReflectionTestUtils.setField(
                                listing,
                                "id",
                                500L);

                return listing;
        }
}