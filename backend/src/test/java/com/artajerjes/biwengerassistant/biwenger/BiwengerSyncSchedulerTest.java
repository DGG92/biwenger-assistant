package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.sync.SyncExecutionService;
import com.artajerjes.biwengerassistant.sync.SyncExecutionStatus;
import com.artajerjes.biwengerassistant.sync.SyncNowResponse;

@ExtendWith(MockitoExtension.class)
class BiwengerSyncSchedulerTest {

        @Mock
        private SyncExecutionService syncExecutionService;

        @Mock
        private LeagueRepository leagueRepository;

        @InjectMocks
        private BiwengerSyncScheduler biwengerSyncScheduler;

        @Test
        void syncShouldExecuteSyncForConfiguredLeague() {

                League league = leagueWithId(1L);

                when(leagueRepository.findAll())
                                .thenReturn(List.of(league));

                when(syncExecutionService.syncScheduled(1L))
                                .thenReturn(
                                                new SyncNowResponse(
                                                                1L,
                                                                true,
                                                                SyncExecutionStatus.SUCCESS));

                biwengerSyncScheduler.sync();

                verify(leagueRepository).findAll();

                verify(syncExecutionService)
                                .syncScheduled(1L);

                verify(syncExecutionService, never())
                                .syncNow(1L);
        }

        @Test
        void syncShouldSkipWhenAnotherSyncIsAlreadyRunning() {

                League league = leagueWithId(1L);

                when(leagueRepository.findAll())
                                .thenReturn(List.of(league));

                when(syncExecutionService.syncScheduled(1L))
                                .thenReturn(
                                                new SyncNowResponse(
                                                                1L,
                                                                false,
                                                                SyncExecutionStatus.RUNNING));

                assertDoesNotThrow(
                                () -> biwengerSyncScheduler.sync());

                verify(syncExecutionService)
                                .syncScheduled(1L);

                verify(syncExecutionService, never())
                                .syncNow(1L);
        }

        @Test
        void syncShouldAbsorbUnexpectedExceptionAndNotPropagateIt() {

                League league = leagueWithId(1L);

                when(leagueRepository.findAll())
                                .thenReturn(List.of(league));

                doThrow(
                                new IllegalStateException(
                                                "Unexpected scheduled sync failure"))
                                .when(syncExecutionService)
                                .syncScheduled(1L);

                assertDoesNotThrow(
                                () -> biwengerSyncScheduler.sync());

                verify(syncExecutionService)
                                .syncScheduled(1L);

                verify(syncExecutionService, never())
                                .syncNow(1L);
        }

        @Test
        void syncShouldDoNothingWhenNoLeaguesAreConfigured() {

                when(leagueRepository.findAll())
                                .thenReturn(List.of());

                assertDoesNotThrow(
                                () -> biwengerSyncScheduler.sync());

                verify(leagueRepository).findAll();

                verify(syncExecutionService, never())
                                .syncScheduled(org.mockito.ArgumentMatchers.anyLong());
        }

        @Test
        void syncShouldExecuteSyncForAllConfiguredLeagues() {

                League firstLeague = leagueWithId(1L);
                League secondLeague = leagueWithId(2L);

                when(leagueRepository.findAll())
                                .thenReturn(
                                                List.of(
                                                                firstLeague,
                                                                secondLeague));

                when(syncExecutionService.syncScheduled(1L))
                                .thenReturn(
                                                new SyncNowResponse(
                                                                1L,
                                                                true,
                                                                SyncExecutionStatus.SUCCESS));

                when(syncExecutionService.syncScheduled(2L))
                                .thenReturn(
                                                new SyncNowResponse(
                                                                2L,
                                                                true,
                                                                SyncExecutionStatus.SUCCESS));

                biwengerSyncScheduler.sync();

                verify(syncExecutionService)
                                .syncScheduled(1L);

                verify(syncExecutionService)
                                .syncScheduled(2L);
        }

        private League leagueWithId(Long id) {

                League league = new League(
                                "League " + id,
                                "biwenger-" + id);

                try {
                        var idField = League.class.getDeclaredField("id");
                        idField.setAccessible(true);
                        idField.set(league, id);

                        return league;

                } catch (ReflectiveOperationException exception) {
                        throw new IllegalStateException(
                                        "Could not configure League id for test",
                                        exception);
                }
        }
}