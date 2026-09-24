package com.artajerjes.biwengerassistant.auth;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeagueAccessService {

    private final CurrentAssistantUserService currentAssistantUserService;

    public LeagueAccessService(
            CurrentAssistantUserService currentAssistantUserService) {
        this.currentAssistantUserService = currentAssistantUserService;
    }

    @Transactional(readOnly = true)
    public void validateAccess(Long leagueId) {
        AssistantUser user = currentAssistantUserService.getCurrentUser();

        if (user.getRole() == AssistantRole.ADMIN) {
            return;
        }

        if (user.getManager() == null
                || user.getManager().getLeague() == null
                || !leagueId.equals(user.getManager().getLeague().getId())) {
            throw new LeagueAccessDeniedException(leagueId);
        }
    }
}