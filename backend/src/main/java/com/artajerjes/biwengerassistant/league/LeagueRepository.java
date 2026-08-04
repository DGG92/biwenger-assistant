package com.artajerjes.biwengerassistant.league;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Long> {
    boolean existsByBiwengerLeagueId(String biwengerLeagueId);
    boolean existsByBiwengerLeagueIdAndIdNot(String biwengerLeagueId, Long id);
}