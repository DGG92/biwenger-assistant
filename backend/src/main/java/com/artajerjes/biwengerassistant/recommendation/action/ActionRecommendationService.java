package com.artajerjes.biwengerassistant.recommendation.action;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerRepository;

@Service
public class ActionRecommendationService {

    private final LeagueRepository leagueRepository;
    private final PlayerRepository playerRepository;

    @Value("${biwenger.user-id}")
    private Long biwengerUserId;

    public ActionRecommendationService(
            LeagueRepository leagueRepository,
            PlayerRepository playerRepository) {

        this.leagueRepository = leagueRepository;
        this.playerRepository = playerRepository;
    }

    @Transactional(readOnly = true)
    public List<ActionCandidate> getSquadActions(
            Long leagueId) {

        if (!leagueRepository.existsById(leagueId)) {
            throw new LeagueNotFoundException(leagueId);
        }

        List<Player> squadPlayers = playerRepository
                .findAllByLeague_Id(leagueId)
                .stream()
                .filter(player -> player.getOwner() != null)
                .filter(player -> biwengerUserId.equals(
                        player.getOwner()
                                .getBiwengerManagerId()))
                .toList();

        List<ActionCandidate> actions = new ArrayList<>();

        for (Player player : squadPlayers) {
            ActionCandidate action = evaluatePlayer(player);

            if (action != null) {
                actions.add(action);
            }
        }

        return actions.stream()
                .sorted(
                        Comparator.comparing(
                                ActionCandidate::priority))
                .toList();
    }

    private ActionCandidate evaluatePlayer(
            Player player) {

        return null;
    }
}