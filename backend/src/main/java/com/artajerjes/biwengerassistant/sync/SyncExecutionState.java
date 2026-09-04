package com.artajerjes.biwengerassistant.sync;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "sync_execution_states", uniqueConstraints = {
        @UniqueConstraint(name = "uk_sync_execution_states_league", columnNames = "league_id")
})
public class SyncExecutionState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private SyncExecutionStatus status;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    protected SyncExecutionState() {
    }

    public SyncExecutionState(Long leagueId) {
        this.leagueId = leagueId;
        this.status = SyncExecutionStatus.IDLE;
    }

    public void markRunning(LocalDateTime startedAt) {
        this.status = SyncExecutionStatus.RUNNING;
        this.startedAt = startedAt;
        this.finishedAt = null;
        this.lastError = null;
    }

    public void markIdle() {
        this.status = SyncExecutionStatus.IDLE;
        this.startedAt = null;
        this.finishedAt = null;
        this.lastError = null;
    }

    public void markSuccess(LocalDateTime finishedAt) {
        this.status = SyncExecutionStatus.SUCCESS;
        this.finishedAt = finishedAt;
        this.lastError = null;
    }

    public void markFailed(
            LocalDateTime finishedAt,
            String lastError) {

        this.status = SyncExecutionStatus.FAILED;
        this.finishedAt = finishedAt;
        this.lastError = lastError;
    }

    public Long getId() {
        return id;
    }

    public Long getLeagueId() {
        return leagueId;
    }

    public SyncExecutionStatus getStatus() {
        return status;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public String getLastError() {
        return lastError;
    }
}