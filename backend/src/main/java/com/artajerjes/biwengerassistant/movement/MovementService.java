package com.artajerjes.biwengerassistant.movement;

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
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerBoardEvent;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerHomeResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerMovementBid;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerMovementItem;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.movement.dto.MovementBidResponse;
import com.artajerjes.biwengerassistant.movement.dto.MovementResponse;
import com.artajerjes.biwengerassistant.movement.dto.MovementSyncResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

import tools.jackson.databind.ObjectMapper;

@Service
public class MovementService {

        private final MovementRepository movementRepository;
        private final LeagueRepository leagueRepository;
        private final PlayerRepository playerRepository;
        private final ManagerRepository managerRepository;
        private final BiwengerClient biwengerClient;
        private final ObjectMapper objectMapper;

        public MovementService(
                        MovementRepository movementRepository,
                        LeagueRepository leagueRepository,
                        PlayerRepository playerRepository,
                        ManagerRepository managerRepository,
                        BiwengerClient biwengerClient,
                        ObjectMapper objectMapper) {
                this.movementRepository = movementRepository;
                this.leagueRepository = leagueRepository;
                this.playerRepository = playerRepository;
                this.managerRepository = managerRepository;
                this.biwengerClient = biwengerClient;
                this.objectMapper = objectMapper;
        }

        @Transactional
        public MovementSyncResponse sync(Long leagueId) {
                League league = leagueRepository.findById(leagueId)
                                .orElseThrow(
                                                () -> new LeagueNotFoundException(leagueId));

                BiwengerHomeResponse response = biwengerClient.getHome();

                if (response == null
                                || response.data() == null
                                || response.data().league() == null
                                || response.data().league().board() == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an invalid home response");
                }

                Map<String, Player> playersByBiwengerId = playerRepository.findAllByLeague_Id(leagueId)
                                .stream()
                                .collect(
                                                Collectors.toMap(
                                                                Player::getBiwengerPlayerId,
                                                                Function.identity()));

                Map<Long, Manager> managersByBiwengerId = managerRepository.findAllByLeague_Id(leagueId)
                                .stream()
                                .collect(
                                                Collectors.toMap(
                                                                Manager::getBiwengerManagerId,
                                                                Function.identity()));

                int processed = 0;
                int created = 0;
                int duplicated = 0;
                int playersNotFound = 0;
                int managersNotFound = 0;

                for (BiwengerBoardEvent event : response.data().league().board()) {

                        if (!"transfer".equals(event.type())
                                        && !"market".equals(event.type())
                                        && !"loan".equals(event.type())) {
                                continue;
                        }

                        if (event.content() == null
                                        || !event.content().isArray()) {
                                continue;
                        }

                        List<BiwengerMovementItem> items = objectMapper.convertValue(
                                        event.content(),
                                        objectMapper
                                                        .getTypeFactory()
                                                        .constructCollectionType(
                                                                        List.class,
                                                                        BiwengerMovementItem.class));

                        for (BiwengerMovementItem item : items) {
                                processed++;

                                if (item.player() == null) {
                                        playersNotFound++;
                                        continue;
                                }

                                Player player = playersByBiwengerId.get(
                                                item.player().toString());

                                if (player == null) {
                                        playersNotFound++;
                                        continue;
                                }

                                Manager fromManager = resolveManager(
                                                item.from(),
                                                managersByBiwengerId);

                                Manager toManager = resolveManager(
                                                item.to(),
                                                managersByBiwengerId);

                                if (item.from() != null
                                                && item.from().id() != null
                                                && fromManager == null) {
                                        managersNotFound++;
                                }

                                if (item.to() != null
                                                && item.to().id() != null
                                                && toManager == null) {
                                        managersNotFound++;
                                }

                                MovementType movementType = resolveMovementType(
                                                event.type(),
                                                item);

                                LocalDateTime occurredAt = toLocalDateTime(event.date());

                                String externalKey = buildExternalKey(
                                                leagueId,
                                                event.date(),
                                                item,
                                                movementType);

                                if (movementRepository
                                                .existsByExternalKey(externalKey)) {
                                        duplicated++;
                                        continue;
                                }

                                Movement movement = new Movement(
                                                externalKey,
                                                movementType,
                                                player,
                                                fromManager,
                                                toManager,
                                                item.amount(),
                                                item.rounds(),
                                                occurredAt,
                                                league);

                                if (item.bids() != null) {
                                        for (BiwengerMovementBid externalBid : item.bids()) {

                                                if (externalBid.user() == null
                                                                || externalBid.user().id() == null) {
                                                        continue;
                                                }

                                                Manager bidder = managersByBiwengerId.get(
                                                                externalBid.user().id());

                                                if (bidder == null) {
                                                        managersNotFound++;
                                                        continue;
                                                }

                                                movement.addBid(
                                                                new MovementBid(
                                                                                bidder,
                                                                                externalBid.amount()));
                                        }
                                }

                                movementRepository.save(movement);
                                created++;
                        }
                }

                return new MovementSyncResponse(
                                processed,
                                created,
                                duplicated,
                                playersNotFound,
                                managersNotFound);
        }

        private Manager resolveManager(
                        com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerMovementUser externalManager,
                        Map<Long, Manager> managersByBiwengerId) {
                if (externalManager == null
                                || externalManager.id() == null) {
                        return null;
                }

                return managersByBiwengerId.get(
                                externalManager.id());
        }

        private MovementType resolveMovementType(
                        String eventType,
                        BiwengerMovementItem item) {

                if ("loan".equals(eventType)) {
                        return MovementType.LOAN;
                }

                if ("market".equals(eventType)) {
                        return MovementType.MARKET_PURCHASE;
                }

                if ("auction".equals(item.type())) {
                        return MovementType.AUCTION_PURCHASE;
                }

                if ("immediateSale".equals(item.type())) {
                        return MovementType.IMMEDIATE_SALE;
                }

                if ("transfer".equals(eventType)
                                && item.from() != null
                                && item.to() == null) {
                        return MovementType.MARKET_SALE;
                }

                return MovementType.TRANSFER;
        }

        private String buildExternalKey(
                        Long leagueId,
                        Long eventDate,
                        BiwengerMovementItem item,
                        MovementType type) {
                Long fromId = item.from() == null
                                ? null
                                : item.from().id();

                Long toId = item.to() == null
                                ? null
                                : item.to().id();

                return String.join(
                                "|",
                                String.valueOf(leagueId),
                                String.valueOf(eventDate),
                                String.valueOf(item.player()),
                                type.name(),
                                String.valueOf(fromId),
                                String.valueOf(toId),
                                String.valueOf(item.amount()));
        }

        private LocalDateTime toLocalDateTime(Long timestamp) {
                if (timestamp == null) {
                        return null;
                }

                return LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(timestamp),
                                ZoneId.systemDefault());
        }

        @Transactional(readOnly = true)
        public List<MovementResponse> findAll(Long leagueId) {
                if (!leagueRepository.existsById(leagueId)) {
                        throw new LeagueNotFoundException(leagueId);
                }

                return movementRepository
                                .findAllByLeague_IdOrderByOccurredAtDesc(leagueId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        private MovementResponse toResponse(Movement movement) {
                List<MovementBidResponse> bids = movement.getBids()
                                .stream()
                                .map(bid -> new MovementBidResponse(
                                                bid.getManager().getId(),
                                                bid.getManager().getName(),
                                                bid.getAmount()))
                                .toList();

                return new MovementResponse(
                                movement.getId(),
                                movement.getType(),
                                movement.getPlayer().getId(),
                                movement.getPlayer().getBiwengerPlayerId(),
                                movement.getPlayer().getName(),
                                movement.getFromManager() == null
                                                ? null
                                                : movement.getFromManager().getId(),
                                movement.getFromManager() == null
                                                ? null
                                                : movement.getFromManager().getName(),
                                movement.getToManager() == null
                                                ? null
                                                : movement.getToManager().getId(),
                                movement.getToManager() == null
                                                ? null
                                                : movement.getToManager().getName(),
                                movement.getAmount(),
                                movement.getRounds(),
                                movement.getOccurredAt(),
                                bids);
        }
}