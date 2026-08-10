package com.artajerjes.biwengerassistant.offer;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketOffer;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketUser;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.offer.dto.EconomicStatusResponse;
import com.artajerjes.biwengerassistant.offer.dto.OfferResponse;
import com.artajerjes.biwengerassistant.offer.dto.OfferSyncResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final LeagueRepository leagueRepository;
    private final PlayerRepository playerRepository;
    private final ManagerRepository managerRepository;
    private final BiwengerClient biwengerClient;

    public OfferService(
            OfferRepository offerRepository,
            LeagueRepository leagueRepository,
            PlayerRepository playerRepository,
            ManagerRepository managerRepository,
            BiwengerClient biwengerClient) {
        this.offerRepository = offerRepository;
        this.leagueRepository = leagueRepository;
        this.playerRepository = playerRepository;
        this.managerRepository = managerRepository;
        this.biwengerClient = biwengerClient;
    }

    @Transactional
    public OfferSyncResponse sync(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(() -> new LeagueNotFoundException(leagueId));

        BiwengerMarketResponse response = biwengerClient.getMarket();

        if (response == null || response.data() == null) {
            throw new IllegalStateException(
                    "Biwenger returned an invalid market response");
        }

        Map<String, Player> playersByBiwengerId = playerRepository.findAllByLeague_Id(leagueId)
                .stream()
                .collect(Collectors.toMap(
                        Player::getBiwengerPlayerId,
                        Function.identity()));

        Map<Long, Manager> managersByBiwengerId = managerRepository.findAllByLeague_Id(leagueId)
                .stream()
                .collect(Collectors.toMap(
                        Manager::getBiwengerManagerId,
                        Function.identity()));

        List<BiwengerMarketOffer> externalOffers = response.data().offers() == null
                ? List.of()
                : response.data().offers();

        int created = 0;
        int updated = 0;
        int playersNotFound = 0;
        int managersNotFound = 0;

        List<Long> currentExternalIds = new ArrayList<>();

        for (BiwengerMarketOffer externalOffer : externalOffers) {
            if (externalOffer == null || externalOffer.id() == null) {
                continue;
            }

            currentExternalIds.add(externalOffer.id());

            List<Player> requestedPlayers = new ArrayList<>();

            if (externalOffer.requestedPlayers() != null) {
                for (Long playerId : externalOffer.requestedPlayers()) {
                    Player player = playersByBiwengerId.get(
                            String.valueOf(playerId));

                    if (player == null) {
                        playersNotFound++;
                        continue;
                    }

                    requestedPlayers.add(player);
                }
            }

            Manager fromManager = resolveManager(
                    externalOffer.from(),
                    managersByBiwengerId);

            Manager toManager = resolveManager(
                    externalOffer.to(),
                    managersByBiwengerId);

            if (externalOffer.from() != null
                    && externalOffer.from().id() != null
                    && fromManager == null) {
                managersNotFound++;
            }

            if (externalOffer.to() != null
                    && externalOffer.to().id() != null
                    && toManager == null) {
                managersNotFound++;
            }

            Offer existing = offerRepository
                    .findByBiwengerOfferId(externalOffer.id())
                    .orElse(null);

            if (existing == null) {
                Offer offer = new Offer(
                        externalOffer.id(),
                        externalOffer.amount(),
                        externalOffer.status(),
                        externalOffer.type(),
                        fromManager,
                        toManager,
                        toLocalDateTime(externalOffer.created()),
                        toLocalDateTime(externalOffer.until()),
                        requestedPlayers,
                        league);

                offerRepository.save(offer);
                created++;
            } else {
                existing.update(
                        externalOffer.amount(),
                        externalOffer.status(),
                        externalOffer.type(),
                        fromManager,
                        toManager,
                        toLocalDateTime(externalOffer.created()),
                        toLocalDateTime(externalOffer.until()),
                        requestedPlayers);

                updated++;
            }
        }

        List<Offer> existingOffers = offerRepository.findAllByLeague_Id(leagueId);

        for (Offer existingOffer : existingOffers) {
            if (!currentExternalIds.contains(
                    existingOffer.getBiwengerOfferId())) {
                offerRepository.delete(existingOffer);
            }
        }

        return new OfferSyncResponse(
                externalOffers.size(),
                created,
                updated,
                playersNotFound,
                managersNotFound);
    }

    @Transactional(readOnly = true)
    public List<OfferResponse> findAll(Long leagueId) {
        if (!leagueRepository.existsById(leagueId)) {
            throw new LeagueNotFoundException(leagueId);
        }

        return offerRepository
                .findAllByLeague_Id(leagueId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public EconomicStatusResponse getEconomicStatus() {
        BiwengerMarketResponse response = biwengerClient.getMarket();

        if (response == null
                || response.data() == null
                || response.data().status() == null) {
            throw new IllegalStateException(
                    "Biwenger returned an invalid market status response");
        }

        return new EconomicStatusResponse(
                response.data().status().balance(),
                response.data().status().maximumBid());
    }

    private Manager resolveManager(
            BiwengerMarketUser externalManager,
            Map<Long, Manager> managersByBiwengerId) {
        if (externalManager == null
                || externalManager.id() == null) {
            return null;
        }

        return managersByBiwengerId.get(
                externalManager.id());
    }

    private OfferResponse toResponse(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getBiwengerOfferId(),
                offer.getAmount(),
                offer.getStatus(),
                offer.getType(),
                offer.getFromManager() == null
                        ? null
                        : offer.getFromManager().getId(),
                offer.getFromManager() == null
                        ? null
                        : offer.getFromManager().getName(),
                offer.getToManager() == null
                        ? null
                        : offer.getToManager().getId(),
                offer.getToManager() == null
                        ? null
                        : offer.getToManager().getName(),
                offer.getCreatedAt(),
                offer.getExpiresAt(),
                offer.getRequestedPlayers()
                        .stream()
                        .map(Player::getId)
                        .toList(),
                offer.getRequestedPlayers()
                        .stream()
                        .map(Player::getName)
                        .toList());
    }

    private LocalDateTime toLocalDateTime(Long timestamp) {
        if (timestamp == null) {
            return null;
        }

        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp),
                ZoneId.systemDefault());
    }
}