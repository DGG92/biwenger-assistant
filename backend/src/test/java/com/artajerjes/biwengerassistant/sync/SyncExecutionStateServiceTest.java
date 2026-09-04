package com.artajerjes.biwengerassistant.sync;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SyncExecutionStateServiceTest {

        private static final Long LEAGUE_ID = 1L;

        private SyncExecutionStateRepository repository;
        private SyncExecutionStateService service;

        @BeforeEach
        void setUp() {

                repository = mock(SyncExecutionStateRepository.class);

                service = new SyncExecutionStateService(repository);

                when(repository.save(any(SyncExecutionState.class)))
                                .thenAnswer(invocation -> invocation.getArgument(0));
        }

        @Test
        void markRunningShouldCreateRunningState() {

                when(repository.findByLeagueId(LEAGUE_ID))
                                .thenReturn(Optional.empty());

                SyncExecutionState result = service.markRunning(LEAGUE_ID);

                assertThat(result.getLeagueId())
                                .isEqualTo(LEAGUE_ID);

                assertThat(result.getStatus())
                                .isEqualTo(SyncExecutionStatus.RUNNING);

                assertThat(result.getStartedAt())
                                .isNotNull();

                assertThat(result.getFinishedAt())
                                .isNull();

                assertThat(result.getLastError())
                                .isNull();

                verify(repository).save(result);
        }

        @Test
        void markIdleShouldResetExecutionState() {
                SyncExecutionState state = new SyncExecutionState(LEAGUE_ID);
                state.markRunning(LocalDateTime.now());

                when(repository.findByLeagueId(LEAGUE_ID))
                                .thenReturn(Optional.of(state));

                when(repository.save(state))
                                .thenReturn(state);

                SyncExecutionState result = service.markIdle(LEAGUE_ID);

                assertEquals(SyncExecutionStatus.IDLE, result.getStatus());
                assertNull(result.getStartedAt());
                assertNull(result.getFinishedAt());
                assertNull(result.getLastError());

                verify(repository).save(state);
        }

        @Test
        void markSuccessShouldReuseExistingState() {

                SyncExecutionState existing = new SyncExecutionState(LEAGUE_ID);

                existing.markRunning(
                                java.time.LocalDateTime.now().minusMinutes(1));

                when(repository.findByLeagueId(LEAGUE_ID))
                                .thenReturn(Optional.of(existing));

                SyncExecutionState result = service.markSuccess(LEAGUE_ID);

                assertThat(result)
                                .isSameAs(existing);

                assertThat(result.getStatus())
                                .isEqualTo(SyncExecutionStatus.SUCCESS);

                assertThat(result.getStartedAt())
                                .isNotNull();

                assertThat(result.getFinishedAt())
                                .isNotNull();

                assertThat(result.getLastError())
                                .isNull();

                verify(repository).save(existing);
        }

        @Test
        void markFailedShouldStoreError() {

                SyncExecutionState existing = new SyncExecutionState(LEAGUE_ID);

                existing.markRunning(
                                java.time.LocalDateTime.now().minusMinutes(1));

                when(repository.findByLeagueId(LEAGUE_ID))
                                .thenReturn(Optional.of(existing));

                SyncExecutionState result = service.markFailed(
                                LEAGUE_ID,
                                "Biwenger unavailable");

                assertThat(result.getStatus())
                                .isEqualTo(SyncExecutionStatus.FAILED);

                assertThat(result.getFinishedAt())
                                .isNotNull();

                assertThat(result.getLastError())
                                .isEqualTo("Biwenger unavailable");

                verify(repository).save(existing);
        }

        @Test
        void failInterruptedExecutionsShouldMarkRunningStatesAsFailed() {

                SyncExecutionState first = new SyncExecutionState(1L);

                first.markRunning(
                                LocalDateTime.now().minusMinutes(5));

                SyncExecutionState second = new SyncExecutionState(2L);

                second.markRunning(
                                LocalDateTime.now().minusMinutes(3));

                when(repository.findAllByStatus(
                                SyncExecutionStatus.RUNNING))
                                .thenReturn(List.of(first, second));

                int result = service.failInterruptedExecutions();

                assertEquals(2, result);

                assertEquals(
                                SyncExecutionStatus.FAILED,
                                first.getStatus());

                assertEquals(
                                SyncExecutionStatus.FAILED,
                                second.getStatus());

                assertNotNull(first.getFinishedAt());
                assertNotNull(second.getFinishedAt());

                assertEquals(
                                "Synchronization interrupted by application restart",
                                first.getLastError());

                assertEquals(
                                "Synchronization interrupted by application restart",
                                second.getLastError());

                verify(repository)
                                .saveAll(List.of(first, second));
        }

        @Test
        void findStateShouldReturnExistingState() {

                SyncExecutionState existing = new SyncExecutionState(LEAGUE_ID);

                when(repository.findByLeagueId(LEAGUE_ID))
                                .thenReturn(Optional.of(existing));

                assertThat(service.findState(LEAGUE_ID))
                                .isSameAs(existing);
        }

        @Test
        void findStateShouldReturnNullWhenMissing() {

                when(repository.findByLeagueId(LEAGUE_ID))
                                .thenReturn(Optional.empty());

                assertThat(service.findState(LEAGUE_ID))
                                .isNull();
        }

        @Test
        void failInterruptedExecutionsShouldDoNothingWhenNoRunningStatesExist() {

                when(repository.findAllByStatus(
                                SyncExecutionStatus.RUNNING))
                                .thenReturn(List.of());

                int result = service.failInterruptedExecutions();

                assertEquals(0, result);

                verify(repository)
                                .saveAll(List.of());
        }
}