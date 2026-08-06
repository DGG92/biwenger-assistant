package com.artajerjes.biwengerassistant.manager;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ManagerRepository extends JpaRepository<Manager, Long> {

        Optional<Manager> findByBiwengerManagerIdAndLeague_Id(
                        Long biwengerManagerId,
                        Long leagueId);

        List<Manager> findAllByLeague_Id(Long leagueId);

        boolean existsByBiwengerManagerIdAndLeague_Id(
                        Long biwengerManagerId,
                        Long leagueId);

        Optional<Manager> findByIdAndLeague_Id(
                        Long managerId,
                        Long leagueId);
}