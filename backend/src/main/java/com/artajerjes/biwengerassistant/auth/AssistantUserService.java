package com.artajerjes.biwengerassistant.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.manager.Manager;
import com.artajerjes.biwengerassistant.manager.ManagerRepository;

@Service
public class AssistantUserService {

    private final AssistantUserRepository assistantUserRepository;
    private final ManagerRepository managerRepository;
    private final PasswordEncoder passwordEncoder;

    public AssistantUserService(
            AssistantUserRepository assistantUserRepository,
            ManagerRepository managerRepository,
            PasswordEncoder passwordEncoder) {
        this.assistantUserRepository = assistantUserRepository;
        this.managerRepository = managerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AssistantUser create(
            String username,
            String rawPassword,
            AssistantRole role,
            Long managerId) {

        if (assistantUserRepository.existsByUsernameIgnoreCase(username)) {
            throw new IllegalArgumentException(
                    "Username already exists: " + username);
        }

        Manager manager = null;

        if (managerId != null) {
            manager = managerRepository.findById(managerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Manager not found: " + managerId));
        }

        AssistantUser user = new AssistantUser(
                username,
                passwordEncoder.encode(rawPassword),
                role,
                manager);

        return assistantUserRepository.save(user);
    }
}