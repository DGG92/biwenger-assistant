package com.artajerjes.biwengerassistant.sync;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SyncExecutionStateService {

    private final SyncExecutionStateRepository repository;

    public SyncExecutionStateService(
            SyncExecutionStateRepository repository) {

        this.repository = repository;
    }

    @Transactional
    public SyncExecutionState markRunning(
            Long leagueId) {

        SyncExecutionState state = findOrCreate(leagueId);

        state.markRunning(LocalDateTime.now());

        return repository.save(state);
    }

    @Transactional
    public SyncExecutionState markIdle(Long leagueId) {
        SyncExecutionState state = findOrCreate(leagueId);
        state.markIdle();
        return repository.save(state);
    }

    @Transactional
    public SyncExecutionState markSuccess(
            Long leagueId) {

        SyncExecutionState state = findOrCreate(leagueId);

        state.markSuccess(LocalDateTime.now());

        return repository.save(state);
    }

    @Transactional
    public SyncExecutionState markFailed(
            Long leagueId,
            String error) {

        SyncExecutionState state = findOrCreate(leagueId);

        state.markFailed(
                LocalDateTime.now(),
                error);

        return repository.save(state);
    }

    @Transactional
    public int failInterruptedExecutions() {

        List<SyncExecutionState> runningStates = repository.findAllByStatus(
                SyncExecutionStatus.RUNNING);

        LocalDateTime finishedAt = LocalDateTime.now();

        for (SyncExecutionState state : runningStates) {
            state.markFailed(
                    finishedAt,
                    "Synchronization interrupted by application restart");
        }

        repository.saveAll(runningStates);

        return runningStates.size();
    }

    @Transactional(readOnly = true)
    public SyncExecutionState findState(
            Long leagueId) {

        return repository.findByLeagueId(leagueId)
                .orElse(null);
    }

    private SyncExecutionState findOrCreate(
            Long leagueId) {

        return repository.findByLeagueId(leagueId)
                .orElseGet(() -> new SyncExecutionState(leagueId));
    }
}