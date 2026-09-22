package com.artajerjes.biwengerassistant.credential;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BiwengerCredentialRepository
        extends JpaRepository<BiwengerCredential, Long> {

    Optional<BiwengerCredential> findByAssistantUser_Id(Long assistantUserId);

    boolean existsByAssistantUser_Id(Long assistantUserId);
}