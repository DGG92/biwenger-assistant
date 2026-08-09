package com.artajerjes.biwengerassistant.market;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketData;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketLastBid;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketListing;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketUser;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.market.dto.MarketListingResponse;
import com.artajerjes.biwengerassistant.market.dto.MarketSyncResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@Service
public class MarketService {

        private final MarketListingRepository marketListingRepository;
        private final LeagueRepository leagueRepository;
        private final PlayerRepository playerRepository;
        private final ManagerRepository managerRepository;
        private final BiwengerClient biwengerClient;

        public MarketService(
                        MarketListingRepository marketListingRepository,
                        LeagueRepository leagueRepository,
                        PlayerRepository playerRepository,
                        ManagerRepository managerRepository,
                        BiwengerClient biwengerClient) {
                this.marketListingRepository = marketListingRepository;
                this.leagueRepository = leagueRepository;
                this.playerRepository = playerRepository;
                this.managerRepository = managerRepository;
                this.biwengerClient = biwengerClient;
        }

        @Transactional
        public MarketSyncResponse sync(Long leagueId) {
                League league = leagueRepository.findById(leagueId)
                                .orElseThrow(
                                                () -> new LeagueNotFoundException(leagueId));

                BiwengerMarketResponse response = biwengerClient.getMarket();

                if (response == null || response.data() == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an invalid market response");
                }

                List<Player> players = playerRepository.findAllByLeague_Id(leagueId);

                Map<String, Player> playersByBiwengerId = players.stream()
                                .collect(
                                                Collectors.toMap(
                                                                Player::getBiwengerPlayerId,
                                                                Function.identity()));

                List<Manager> managers = managerRepository.findAllByLeague_Id(leagueId);

                Map<Long, Manager> managersByBiwengerId = managers.stream()
                                .collect(
                                                Collectors.toMap(
                                                                Manager::getBiwengerManagerId,
                                                                Function.identity()));

                marketListingRepository.deleteAllByLeague_Id(leagueId);

                BiwengerMarketData data = response.data();

                int sales = 0;
                int auctions = 0;
                int playersNotFound = 0;
                int managersNotFound = 0;

                if (data.sales() != null) {
                        for (BiwengerMarketListing externalListing : data.sales()) {
                                MarketBuildResult result = buildListing(
                                                externalListing,
                                                MarketListingType.SALE,
                                                league,
                                                playersByBiwengerId,
                                                managersByBiwengerId);

                                if (result.playerNotFound()) {
                                        playersNotFound++;
                                        continue;
                                }

                                managersNotFound += result.managersNotFound();

                                marketListingRepository.save(result.listing());
                                sales++;
                        }
                }

                if (data.auctions() != null) {
                        for (BiwengerMarketListing externalListing : data.auctions()) {
                                MarketBuildResult result = buildListing(
                                                externalListing,
                                                MarketListingType.AUCTION,
                                                league,
                                                playersByBiwengerId,
                                                managersByBiwengerId);

                                if (result.playerNotFound()) {
                                        playersNotFound++;
                                        continue;
                                }

                                managersNotFound += result.managersNotFound();

                                marketListingRepository.save(result.listing());
                                auctions++;
                        }
                }

                return new MarketSyncResponse(
                                sales,
                                auctions,
                                playersNotFound,
                                managersNotFound);
        }

        private MarketBuildResult buildListing(
                        BiwengerMarketListing externalListing,
                        MarketListingType type,
                        League league,
                        Map<String, Player> playersByBiwengerId,
                        Map<Long, Manager> managersByBiwengerId) {
                if (externalListing == null
                                || externalListing.player() == null
                                || externalListing.player().id() == null) {
                        return MarketBuildResult.missingPlayer();
                }

                Player player = playersByBiwengerId.get(
                                externalListing.player().id().toString());

                if (player == null) {
                        return MarketBuildResult.missingPlayer();
                }

                int managersNotFound = 0;

                Manager seller = null;

                BiwengerMarketUser externalSeller = externalListing.user();

                if (externalSeller != null && externalSeller.id() != null) {
                        seller = managersByBiwengerId.get(
                                        externalSeller.id());

                        if (seller == null) {
                                managersNotFound++;
                        }
                }

                Long lastBidAmount = null;
                String lastBidStatus = null;
                Manager lastBidManager = null;

                BiwengerMarketLastBid lastBid = externalListing.lastBid();

                if (lastBid != null) {
                        lastBidAmount = lastBid.amount();
                        lastBidStatus = lastBid.status();

                        if (lastBid.from() != null
                                        && lastBid.from().id() != null) {
                                lastBidManager = managersByBiwengerId.get(
                                                lastBid.from().id());

                                if (lastBidManager == null) {
                                        managersNotFound++;
                                }
                        }
                }

                MarketListing listing = new MarketListing(
                                type,
                                player,
                                seller,
                                externalListing.price(),
                                toLocalDateTime(externalListing.date()),
                                toLocalDateTime(externalListing.until()),
                                Boolean.TRUE.equals(externalListing.extended()),
                                lastBidAmount,
                                lastBidStatus,
                                lastBidManager,
                                league);

                return new MarketBuildResult(
                                listing,
                                false,
                                managersNotFound);
        }

        private LocalDateTime toLocalDateTime(Long timestamp) {
                if (timestamp == null) {
                        return null;
                }

                return LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(timestamp),
                                ZoneId.systemDefault());
        }

        private record MarketBuildResult(
                        MarketListing listing,
                        boolean playerNotFound,
                        int managersNotFound) {

                private static MarketBuildResult missingPlayer() {
                        return new MarketBuildResult(
                                        null,
                                        true,
                                        0);
                }
        }

        @Transactional(readOnly = true)
        public List<MarketListingResponse> findAll(Long leagueId) {
                if (!leagueRepository.existsById(leagueId)) {
                        throw new LeagueNotFoundException(leagueId);
                }

                return marketListingRepository
                                .findAllByLeague_Id(leagueId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        private MarketListingResponse toResponse(
                        MarketListing listing) {
                return new MarketListingResponse(
                                listing.getId(),
                                listing.getType(),
                                listing.getPlayer().getId(),
                                listing.getPlayer().getBiwengerPlayerId(),
                                listing.getPlayer().getName(),
                                listing.getPlayer().getTeamName(),
                                listing.getPlayer().getMarketValue(),
                                listing.getPrice(),
                                listing.getSeller() == null
                                                ? null
                                                : listing.getSeller().getId(),
                                listing.getSeller() == null
                                                ? null
                                                : listing.getSeller().getName(),
                                listing.getPublishedAt(),
                                listing.getExpiresAt(),
                                listing.isExtended(),
                                listing.getLastBidAmount(),
                                listing.getLastBidStatus(),
                                listing.getLastBidManager() == null
                                                ? null
                                                : listing.getLastBidManager().getId(),
                                listing.getLastBidManager() == null
                                                ? null
                                                : listing.getLastBidManager().getName());
        }
}