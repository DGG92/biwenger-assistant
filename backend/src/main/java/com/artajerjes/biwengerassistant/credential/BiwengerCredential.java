package com.artajerjes.biwengerassistant.credential;

import java.time.LocalDateTime;

import com.artajerjes.biwengerassistant.auth.AssistantUser;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "biwenger_credentials")
public class BiwengerCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assistant_user_id", nullable = false, unique = true)
    private AssistantUser assistantUser;

    @Column(name = "biwenger_user_id", nullable = false)
    private Long biwengerUserId;

    @Column(name = "encrypted_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "last_validated_at")
    private LocalDateTime lastValidatedAt;

    protected BiwengerCredential() {
    }

    public BiwengerCredential(
            AssistantUser assistantUser,
            Long biwengerUserId,
            String encryptedToken) {

        this.assistantUser = assistantUser;
        this.biwengerUserId = biwengerUserId;
        this.encryptedToken = encryptedToken;
    }

    @PrePersist
    private void prePersist() {
        LocalDateTime now = LocalDateTime.now();

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public AssistantUser getAssistantUser() {
        return assistantUser;
    }

    public Long getBiwengerUserId() {
        return biwengerUserId;
    }

    public String getEncryptedToken() {
        return encryptedToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public LocalDateTime getLastValidatedAt() {
        return lastValidatedAt;
    }

    public void updateToken(String encryptedToken) {
        this.encryptedToken = encryptedToken;
    }

    public void markValidated() {
        this.lastValidatedAt = LocalDateTime.now();
    }
}