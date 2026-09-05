package com.artajerjes.biwengerassistant.auth;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssistantUserRepository
        extends JpaRepository<AssistantUser, Long> {

    @EntityGraph(attributePaths = {
            "manager",
            "manager.league"
    })
    Optional<AssistantUser> findByUsernameIgnoreCase(String username);

    boolean existsByUsernameIgnoreCase(String username);
}