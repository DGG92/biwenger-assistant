package com.artajerjes.biwengerassistant.player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.function.Function;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionPlayer;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionTeam;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerPlayerOwner;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerUserLineup;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerUserPlayer;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerUserResponse;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerNotFoundException;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.player.dto.CreatePlayerRequest;
import com.artajerjes.biwengerassistant.player.dto.PlayerLineupSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerOwnershipSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.UpdatePlayerRequest;

@Service
public class PlayerService {

        private final PlayerRepository playerRepository;
        private final LeagueRepository leagueRepository;
        private final ManagerRepository managerRepository;

        private final BiwengerClient biwengerClient;

        public PlayerService(
                        PlayerRepository playerRepository,
                        LeagueRepository leagueRepository,
                        ManagerRepository managerRepository,
                        BiwengerClient biwengerClient) {
                this.playerRepository = playerRepository;
                this.leagueRepository = leagueRepository;
                this.managerRepository = managerRepository;
                this.biwengerClient = biwengerClient;
        }

        @Transactional
        public PlayerResponse create(
                        Long leagueId,
                        CreatePlayerRequest request) {
                League league = leagueRepository.findById(leagueId)
                                .orElseThrow(
                                                () -> new LeagueNotFoundException(leagueId));

                if (playerRepository.existsByBiwengerPlayerIdAndLeague_Id(
                                request.biwengerPlayerId(),
                                leagueId)) {
                        throw new PlayerAlreadyExistsException(
                                        request.biwengerPlayerId(),
                                        leagueId);
                }

                Player player = new Player(
                                request.biwengerPlayerId(),
                                request.name(),
                                request.positions(),
                                request.teamName(),
                                request.marketValue(),
                                league);

                Player savedPlayer = playerRepository.save(player);

                return toResponse(savedPlayer);
        }

        @Transactional(readOnly = true)
        public List<PlayerResponse> findAll(Long leagueId) {
                ensureLeagueExists(leagueId);

                return playerRepository.findAllByLeague_Id(leagueId)
                                .stream()
                                .map(this::toResponse)
                                .toList();
        }

        @Transactional(readOnly = true)
        public PlayerResponse findById(
                        Long leagueId,
                        Long playerId) {
                Player player = findPlayer(leagueId, playerId);

                return toResponse(player);
        }

        @Transactional
        public PlayerResponse update(
                        Long leagueId,
                        Long playerId,
                        UpdatePlayerRequest request) {
                Player player = findPlayer(leagueId, playerId);

                if (playerRepository
                                .existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                                                request.biwengerPlayerId(),
                                                leagueId,
                                                playerId)) {
                        throw new PlayerAlreadyExistsException(
                                        request.biwengerPlayerId(),
                                        leagueId);
                }

                Manager owner = resolveOwner(
                                request.ownerId(),
                                leagueId);

                player.update(
                                request.biwengerPlayerId(),
                                request.name(),
                                request.positions(),
                                request.points(),
                                request.teamName(),
                                request.marketValue(),
                                request.injured(),
                                request.captain(),
                                request.ram(),
                                request.valueFluctuation(),
                                request.clauseLockedUntil(),
                                request.clauseValue(),
                                owner,
                                request.signedAt());

                return toResponse(player);
        }

        @Transactional
        public void delete(
                        Long leagueId,
                        Long playerId) {
                Player player = findPlayer(leagueId, playerId);

                playerRepository.delete(player);
        }

        private Player findPlayer(
                        Long leagueId,
                        Long playerId) {
                ensureLeagueExists(leagueId);

                return playerRepository
                                .findByIdAndLeague_Id(playerId, leagueId)
                                .orElseThrow(
                                                () -> new PlayerNotFoundException(
                                                                playerId,
                                                                leagueId));
        }

        private void ensureLeagueExists(Long leagueId) {
                if (!leagueRepository.existsById(leagueId)) {
                        throw new LeagueNotFoundException(leagueId);
                }
        }

        private Manager resolveOwner(
                        Long ownerId,
                        Long leagueId) {
                if (ownerId == null) {
                        return null;
                }

                return managerRepository
                                .findByIdAndLeague_Id(ownerId, leagueId)
                                .orElseThrow(
                                                () -> new ManagerNotFoundException(
                                                                ownerId,
                                                                leagueId));
        }

        private PlayerResponse toResponse(Player player) {
                Manager owner = player.getOwner();

                return new PlayerResponse(
                                player.getId(),
                                player.getBiwengerPlayerId(),
                                player.getName(),
                                player.getPositions(),
                                player.getPoints(),
                                player.getTeamName(),
                                player.getMarketValue(),
                                player.isInjured(),
                                player.isCaptain(),
                                player.isRam(),
                                player.isStarter(),
                                player.isReserve(),
                                player.getLineupPosition(),
                                player.getValueFluctuation(),
                                player.isBlockedClause(),
                                player.getClauseLockedUntil(),
                                player.getClauseValue(),
                                owner == null ? null : owner.getId(),
                                owner == null ? null : owner.getName(),
                                player.isFreePlayer(),
                                player.getSignedAt(),
                                player.getLeague().getId(),
                                player.getCreatedAt());
        }

        private PlayerPosition mapPosition(Integer position) {
                if (position == null) {
                        throw new IllegalArgumentException(
                                        "Biwenger player position cannot be null");
                }

                return switch (position) {
                        case 1 -> PlayerPosition.PT;
                        case 2 -> PlayerPosition.DF;
                        case 3 -> PlayerPosition.MC;
                        case 4 -> PlayerPosition.DL;
                        case 5 -> PlayerPosition.E;
                        default -> throw new IllegalArgumentException(
                                        "Unknown Biwenger position: " + position);
                };
        }

        private List<PlayerPosition> mapPositions(
                        Integer mainPosition,
                        List<Integer> altPositions) {
                List<PlayerPosition> result = new ArrayList<>();

                result.add(mapPosition(mainPosition));

                if (altPositions != null) {
                        for (Integer altPosition : altPositions) {
                                PlayerPosition mapped = mapPosition(altPosition);

                                if (!result.contains(mapped)) {
                                        result.add(mapped);
                                }
                        }
                }

                return result;
        }

        private boolean isInjured(String status) {
                return status != null
                                && !"ok".equalsIgnoreCase(status);
        }

        @Transactional
        public PlayerSyncResponse syncCompetitionPlayers(Long leagueId) {
                League league = leagueRepository.findById(leagueId)
                                .orElseThrow(
                                                () -> new LeagueNotFoundException(leagueId));

                BiwengerCompetitionResponse response = biwengerClient.getCompetition();

                if (response == null
                                || response.data() == null
                                || response.data().players() == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an empty competition player catalogue");
                }

                int created = 0;
                int updated = 0;
                int skipped = 0;

                for (BiwengerCompetitionPlayer externalPlayer : response.data().players().values()) {
                        if (externalPlayer.id() == null
                                        || externalPlayer.name() == null
                                        || externalPlayer.position() == null
                                        || externalPlayer.price() == null) {
                                skipped++;
                                continue;
                        }

                        String biwengerPlayerId = externalPlayer.id().toString();

                        List<PlayerPosition> positions = mapPositions(
                                        externalPlayer.position(),
                                        externalPlayer.altPositions());

                        String teamName = resolveTeamName(
                                        response,
                                        externalPlayer.teamId());

                        boolean injured = isInjured(
                                        externalPlayer.status());

                        Player player = playerRepository
                                        .findByBiwengerPlayerIdAndLeague_Id(
                                                        biwengerPlayerId,
                                                        leagueId)
                                        .orElse(null);

                        if (player == null) {
                                Player newPlayer = new Player(
                                                biwengerPlayerId,
                                                externalPlayer.name(),
                                                positions,
                                                teamName,
                                                externalPlayer.price(),
                                                league);

                                newPlayer.updateCompetitionData(
                                                externalPlayer.name(),
                                                positions,
                                                externalPlayer.points() == null
                                                                ? 0
                                                                : externalPlayer.points(),
                                                teamName,
                                                externalPlayer.price(),
                                                injured,
                                                externalPlayer.priceIncrement() == null
                                                                ? 0L
                                                                : externalPlayer.priceIncrement());

                                playerRepository.save(newPlayer);
                                created++;
                        } else {
                                player.updateCompetitionData(
                                                externalPlayer.name(),
                                                positions,
                                                externalPlayer.points() == null
                                                                ? 0
                                                                : externalPlayer.points(),
                                                teamName,
                                                externalPlayer.price(),
                                                injured,
                                                externalPlayer.priceIncrement() == null
                                                                ? 0L
                                                                : externalPlayer.priceIncrement());

                                updated++;
                        }
                }

                return new PlayerSyncResponse(
                                response.data().players().size(),
                                created,
                                updated,
                                skipped);
        }

        private String resolveTeamName(
                        BiwengerCompetitionResponse response,
                        Long teamId) {
                if (teamId == null) {
                        return null;
                }

                BiwengerCompetitionTeam team = response.data().teams().get(teamId.toString());

                return team == null
                                ? null
                                : team.name();
        }

        @Transactional
        public PlayerOwnershipSyncResponse syncPlayerOwnership(Long leagueId) {
                League league = leagueRepository.findById(leagueId)
                                .orElseThrow(
                                                () -> new LeagueNotFoundException(leagueId));

                List<Manager> managers = managerRepository.findAllByLeague_Id(leagueId);

                List<Player> players = playerRepository.findAllByLeague_Id(leagueId);

                Map<String, Player> playersByBiwengerId = players.stream()
                                .collect(
                                                Collectors.toMap(
                                                                Player::getBiwengerPlayerId,
                                                                Function.identity()));

                /*
                 * Empezamos suponiendo que todos están libres.
                 *
                 * Después asignaremos únicamente aquellos que Biwenger
                 * indique actualmente como propiedad de algún manager.
                 */
                players.forEach(Player::clearOwnership);

                int playersAssigned = 0;
                int playersNotFound = 0;

                for (Manager manager : managers) {
                        BiwengerUserResponse response = biwengerClient.getUser(
                                        manager.getBiwengerManagerId());

                        if (response == null
                                        || response.data() == null
                                        || response.data().players() == null) {
                                continue;
                        }

                        for (BiwengerUserPlayer externalPlayer : response.data().players()) {

                                if (externalPlayer.id() == null) {
                                        continue;
                                }

                                Player player = playersByBiwengerId.get(
                                                externalPlayer.id().toString());

                                if (player == null) {
                                        playersNotFound++;
                                        continue;
                                }

                                BiwengerPlayerOwner ownership = externalPlayer.owner();

                                player.updateOwnership(
                                                manager,
                                                ownership == null
                                                                ? null
                                                                : toLocalDateTime(
                                                                                ownership.date()),
                                                ownership == null
                                                                ? null
                                                                : ownership.clause(),
                                                ownership == null
                                                                ? null
                                                                : toLocalDateTime(
                                                                                ownership.clauseLockedUntil()));

                                playersAssigned++;
                        }
                }

                return new PlayerOwnershipSyncResponse(
                                managers.size(),
                                playersAssigned,
                                playersNotFound);
        }

        private LocalDateTime toLocalDateTime(Long timestamp) {
                if (timestamp == null) {
                        return null;
                }

                return LocalDateTime.ofInstant(
                                Instant.ofEpochSecond(timestamp),
                                ZoneId.systemDefault());
        }

        private List<PlayerPosition> buildLineupPositions(String formation) {
                if (formation == null || formation.isBlank()) {
                        return List.of();
                }

                String[] lines = formation.split("-");

                if (lines.length < 2) {
                        throw new IllegalArgumentException(
                                        "Invalid Biwenger formation: " + formation);
                }

                List<Integer> counts = new ArrayList<>();

                for (String line : lines) {
                        try {
                                counts.add(Integer.valueOf(line));
                        } catch (NumberFormatException exception) {
                                throw new IllegalArgumentException(
                                                "Invalid Biwenger formation: " + formation,
                                                exception);
                        }
                }

                List<PlayerPosition> positions = new ArrayList<>();

                // Siempre hay un portero.
                positions.add(PlayerPosition.PT);

                // Primera línea de la formación: defensas.
                for (int i = 0; i < counts.get(0); i++) {
                        positions.add(PlayerPosition.DF);
                }

                // Todas las líneas intermedias se consideran centrocampistas.
                for (int lineIndex = 1; lineIndex < counts.size() - 1; lineIndex++) {
                        for (int i = 0; i < counts.get(lineIndex); i++) {
                                positions.add(PlayerPosition.MC);
                        }
                }

                // Última línea: delanteros.
                int forwards = counts.get(counts.size() - 1);

                for (int i = 0; i < forwards; i++) {
                        positions.add(PlayerPosition.DL);
                }

                return positions;
        }

        @Transactional
        public PlayerLineupSyncResponse syncCurrentLineup(Long leagueId) {
                League league = leagueRepository.findById(leagueId)
                                .orElseThrow(
                                                () -> new LeagueNotFoundException(leagueId));

                BiwengerUserResponse response = biwengerClient.getCurrentUser();

                if (response == null
                                || response.data() == null
                                || response.data().id() == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an invalid current user response");
                }

                Manager manager = managerRepository
                                .findByBiwengerManagerIdAndLeague_Id(
                                                response.data().id(),
                                                league.getId())
                                .orElseThrow(
                                                () -> new IllegalStateException(
                                                                "Current Biwenger manager does not exist in local database"));

                List<Player> players = playerRepository.findAllByLeague_Id(leagueId);

                players.stream()
                                .filter(player -> manager.equals(player.getOwner()))
                                .forEach(Player::clearLineupRoles);

                BiwengerUserLineup lineup = response.data().lineup();

                if (lineup == null) {
                        return new PlayerLineupSyncResponse(
                                        manager.getId(),
                                        null,
                                        null,
                                        null,
                                        null);
                }

                Long captainId = lineup.captain() == null
                                ? null
                                : lineup.captain().id();

                Long ramId = lineup.striker() == null
                                ? null
                                : lineup.striker().id();

                Long coachId = lineup.coach() == null
                                ? null
                                : lineup.coach().id();

                List<Long> starterIds = lineup.playersID() == null
                                ? List.of()
                                : lineup.playersID();

                List<Long> reserveIds = lineup.reservesID() == null
                                ? List.of()
                                : lineup.reservesID();

                List<PlayerPosition> lineupPositions = buildLineupPositions(lineup.type());

                if (starterIds.size() != lineupPositions.size()) {
                        throw new IllegalStateException(
                                        "Biwenger lineup does not match formation "
                                                        + lineup.type()
                                                        + ": expected "
                                                        + lineupPositions.size()
                                                        + " starters but received "
                                                        + starterIds.size());
                }

                Map<Long, PlayerPosition> lineupPositionByPlayerId = new java.util.HashMap<>();

                for (int i = 0; i < starterIds.size(); i++) {
                        lineupPositionByPlayerId.put(
                                        starterIds.get(i),
                                        lineupPositions.get(i));
                }

                for (Player player : players) {
                        if (!manager.equals(player.getOwner())) {
                                continue;
                        }

                        Long biwengerPlayerId = Long.valueOf(player.getBiwengerPlayerId());

                        boolean starter = starterIds.contains(biwengerPlayerId);

                        boolean reserve = reserveIds.contains(biwengerPlayerId);

                        PlayerPosition lineupPosition = lineupPositionByPlayerId.get(biwengerPlayerId);

                        player.updateLineupRoles(
                                        biwengerPlayerId.equals(captainId),
                                        biwengerPlayerId.equals(ramId),
                                        starter,
                                        reserve,
                                        lineupPosition);
                }

                return new PlayerLineupSyncResponse(
                                manager.getId(),
                                lineup.type(),
                                captainId,
                                ramId,
                                coachId);
        }
}