package com.artajerjes.biwengerassistant.matchday;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchdayContextRepository
        extends JpaRepository<MatchdayContext, Long> {

    Optional<MatchdayContext> findByLeagueIdAndBiwengerRoundId(
            Long leagueId,
            Long biwengerRoundId);
}