package com.artajerjes.biwengerassistant.sync;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SyncExecutionStateRepository
                extends JpaRepository<SyncExecutionState, Long> {

        Optional<SyncExecutionState> findByLeagueId(
                        Long leagueId);

        List<SyncExecutionState> findAllByStatus(
                        SyncExecutionStatus status);
}