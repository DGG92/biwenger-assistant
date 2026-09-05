package com.artajerjes.biwengerassistant.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "assistant.bootstrap.enabled", havingValue = "true")
public class AssistantAdminBootstrap implements ApplicationRunner {

    private final AssistantUserService assistantUserService;
    private final AssistantUserRepository assistantUserRepository;

    private final String username;
    private final String password;
    private final Long managerId;

    public AssistantAdminBootstrap(
            AssistantUserService assistantUserService,
            AssistantUserRepository assistantUserRepository,
            @Value("${assistant.bootstrap.username}") String username,
            @Value("${assistant.bootstrap.password}") String password,
            @Value("${assistant.bootstrap.manager-id}") Long managerId) {
        this.assistantUserService = assistantUserService;
        this.assistantUserRepository = assistantUserRepository;
        this.username = username;
        this.password = password;
        this.managerId = managerId;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (assistantUserRepository
                .existsByUsernameIgnoreCase(username)) {
            return;
        }

        assistantUserService.create(
                username,
                password,
                AssistantRole.ADMIN,
                managerId);
    }
}