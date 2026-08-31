package com.artajerjes.biwengerassistant.matchday;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamStandingSnapshotRepository
        extends JpaRepository<TeamStandingSnapshot, Long> {

    Optional<TeamStandingSnapshot> findByLeagueIdAndBiwengerRoundIdAndBiwengerTeamId(
            Long leagueId,
            Long biwengerRoundId,
            Long biwengerTeamId);

    List<TeamStandingSnapshot> findByLeagueIdAndBiwengerRoundId(
            Long leagueId,
            Long biwengerRoundId);
}