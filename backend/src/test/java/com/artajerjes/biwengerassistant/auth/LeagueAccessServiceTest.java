package com.artajerjes.biwengerassistant.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.manager.Manager;

class LeagueAccessServiceTest {

    private CurrentAssistantUserService currentAssistantUserService;
    private LeagueAccessService leagueAccessService;

    @BeforeEach
    void setUp() {
        currentAssistantUserService = mock(CurrentAssistantUserService.class);
        leagueAccessService = new LeagueAccessService(
                currentAssistantUserService);
    }

    @Test
    void userShouldAccessOwnLeague() {

        AssistantUser user = mock(AssistantUser.class);
        Manager manager = mock(Manager.class);
        League league = mock(League.class);

        when(currentAssistantUserService.getCurrentUser())
                .thenReturn(user);

        when(user.getRole())
                .thenReturn(AssistantRole.USER);

        when(user.getManager())
                .thenReturn(manager);

        when(manager.getLeague())
                .thenReturn(league);

        when(league.getId())
                .thenReturn(1L);

        assertDoesNotThrow(
                () -> leagueAccessService.validateAccess(1L));
    }

    @Test
    void userShouldNotAccessAnotherLeague() {

        AssistantUser user = mock(AssistantUser.class);
        Manager manager = mock(Manager.class);
        League league = mock(League.class);

        when(currentAssistantUserService.getCurrentUser())
                .thenReturn(user);

        when(user.getRole())
                .thenReturn(AssistantRole.USER);

        when(user.getManager())
                .thenReturn(manager);

        when(manager.getLeague())
                .thenReturn(league);

        when(league.getId())
                .thenReturn(1L);

        assertThrows(
                LeagueAccessDeniedException.class,
                () -> leagueAccessService.validateAccess(2L));
    }

    @Test
    void adminShouldAccessAnyLeague() {

        AssistantUser user = mock(AssistantUser.class);

        when(currentAssistantUserService.getCurrentUser())
                .thenReturn(user);

        when(user.getRole())
                .thenReturn(AssistantRole.ADMIN);

        assertDoesNotThrow(
                () -> leagueAccessService.validateAccess(999L));
    }

    @Test
    void userWithoutManagerShouldNotAccessLeague() {

        AssistantUser user = mock(AssistantUser.class);

        when(currentAssistantUserService.getCurrentUser())
                .thenReturn(user);

        when(user.getRole())
                .thenReturn(AssistantRole.USER);

        when(user.getManager())
                .thenReturn(null);

        assertThrows(
                LeagueAccessDeniedException.class,
                () -> leagueAccessService.validateAccess(1L));
    }
}