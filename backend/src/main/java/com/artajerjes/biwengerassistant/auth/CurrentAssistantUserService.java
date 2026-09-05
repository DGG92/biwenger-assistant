package com.artajerjes.biwengerassistant.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.manager.Manager;

@Service
public class CurrentAssistantUserService {

    private final AssistantUserRepository assistantUserRepository;

    public CurrentAssistantUserService(
            AssistantUserRepository assistantUserRepository) {
        this.assistantUserRepository = assistantUserRepository;
    }

    @Transactional(readOnly = true)
    public AssistantUser getCurrentUser() {

        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {

            throw new IllegalStateException(
                    "No authenticated Assistant user");
        }

        return assistantUserRepository
                .findByUsernameIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated Assistant user not found: "
                                + authentication.getName()));
    }

    @Transactional(readOnly = true)
    public Manager getCurrentManager() {

        AssistantUser user = getCurrentUser();

        if (user.getManager() == null) {
            throw new IllegalStateException(
                    "Authenticated Assistant user has no manager assigned");
        }

        return user.getManager();
    }
}