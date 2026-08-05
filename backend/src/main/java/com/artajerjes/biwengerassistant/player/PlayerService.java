package com.artajerjes.biwengerassistant.player;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerNotFoundException;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;
import com.artajerjes.biwengerassistant.player.dto.CreatePlayerRequest;
import com.artajerjes.biwengerassistant.player.dto.PlayerResponse;
import com.artajerjes.biwengerassistant.player.dto.UpdatePlayerRequest;

@Service
public class PlayerService {

    private final PlayerRepository playerRepository;
    private final LeagueRepository leagueRepository;
    private final ManagerRepository managerRepository;

    public PlayerService(
            PlayerRepository playerRepository,
            LeagueRepository leagueRepository,
            ManagerRepository managerRepository
    ) {
        this.playerRepository = playerRepository;
        this.leagueRepository = leagueRepository;
        this.managerRepository = managerRepository;
    }

    @Transactional
    public PlayerResponse create(
            Long leagueId,
            CreatePlayerRequest request
    ) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(
                        () -> new LeagueNotFoundException(leagueId)
                );

        if (
                playerRepository.existsByBiwengerPlayerIdAndLeague_Id(
                        request.biwengerPlayerId(),
                        leagueId
                )
        ) {
            throw new PlayerAlreadyExistsException(
                    request.biwengerPlayerId(),
                    leagueId
            );
        }

        Player player = new Player(
                request.biwengerPlayerId(),
                request.name(),
                request.positions(),
                request.teamName(),
                request.marketValue(),
                league
        );

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
            Long playerId
    ) {
        Player player = findPlayer(leagueId, playerId);

        return toResponse(player);
    }

    @Transactional
    public PlayerResponse update(
            Long leagueId,
            Long playerId,
            UpdatePlayerRequest request
    ) {
        Player player = findPlayer(leagueId, playerId);

        if (
                playerRepository
                        .existsByBiwengerPlayerIdAndLeague_IdAndIdNot(
                                request.biwengerPlayerId(),
                                leagueId,
                                playerId
                        )
        ) {
            throw new PlayerAlreadyExistsException(
                    request.biwengerPlayerId(),
                    leagueId
            );
        }

        Manager owner = resolveOwner(
                request.ownerId(),
                leagueId
        );

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
                request.blockedClause(),
                request.clauseValue(),
                owner,
                request.signedAt()
        );

        return toResponse(player);
    }

    @Transactional
    public void delete(
            Long leagueId,
            Long playerId
    ) {
        Player player = findPlayer(leagueId, playerId);

        playerRepository.delete(player);
    }

    private Player findPlayer(
            Long leagueId,
            Long playerId
    ) {
        ensureLeagueExists(leagueId);

        return playerRepository
                .findByIdAndLeague_Id(playerId, leagueId)
                .orElseThrow(
                        () -> new PlayerNotFoundException(
                                playerId,
                                leagueId
                        )
                );
    }

    private void ensureLeagueExists(Long leagueId) {
        if (!leagueRepository.existsById(leagueId)) {
            throw new LeagueNotFoundException(leagueId);
        }
    }

    private Manager resolveOwner(
            Long ownerId,
            Long leagueId
    ) {
        if (ownerId == null) {
            return null;
        }

        return managerRepository
                .findByIdAndLeague_Id(ownerId, leagueId)
                .orElseThrow(
                        () -> new ManagerNotFoundException(
                                ownerId,
                                leagueId
                        )
                );
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
                player.getValueFluctuation(),
                player.isBlockedClause(),
                player.getClauseValue(),
                owner == null ? null : owner.getId(),
                owner == null ? null : owner.getName(),
                player.isFreePlayer(),
                player.getSignedAt(),
                player.getLeague().getId(),
                player.getCreatedAt()
        );
    }
}