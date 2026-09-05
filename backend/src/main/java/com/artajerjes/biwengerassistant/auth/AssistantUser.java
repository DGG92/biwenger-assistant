package com.artajerjes.biwengerassistant.auth;

import java.time.LocalDateTime;

import com.artajerjes.biwengerassistant.manager.Manager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "assistant_users")
public class AssistantUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AssistantRole role;

    @Column(nullable = false)
    private boolean enabled;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "manager_id", unique = true)
    private Manager manager;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AssistantUser() {
    }

    public AssistantUser(
            String username,
            String passwordHash,
            AssistantRole role,
            Manager manager) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.manager = manager;
        this.enabled = true;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public AssistantRole getRole() {
        return role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public Manager getManager() {
        return manager;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}