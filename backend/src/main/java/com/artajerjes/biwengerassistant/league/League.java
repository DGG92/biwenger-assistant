package com.artajerjes.biwengerassistant.league;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "leagues")
public class League {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "biwenger_league_id", unique = true)
    private String biwengerLeagueId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected League() {
        // Constructor requerido por JPA.
    }

    public League(String name, String biwengerLeagueId) {
        this.name = name;
        this.biwengerLeagueId = biwengerLeagueId;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBiwengerLeagueId() {
        return biwengerLeagueId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}