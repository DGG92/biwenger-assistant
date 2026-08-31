package com.artajerjes.biwengerassistant.matchday;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchdayGameRepository
                extends JpaRepository<MatchdayGame, Long> {

        Optional<MatchdayGame> findByLeagueIdAndBiwengerGameId(
                        Long leagueId,
                        Long biwengerGameId);

        List<MatchdayGame> findByLeagueIdAndBiwengerRoundId(
                        Long leagueId,
                        Long biwengerRoundId);
}