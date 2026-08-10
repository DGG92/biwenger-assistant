package com.artajerjes.biwengerassistant.manager;

import java.time.LocalDateTime;

import com.artajerjes.biwengerassistant.league.League;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "managers", uniqueConstraints = {
        @UniqueConstraint(name = "uk_manager_biwenger_id_league", columnNames = {
                "biwenger_manager_id",
                "league_id"
        })
})
public class Manager {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "biwenger_manager_id", nullable = false)
    private Long biwengerManagerId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String icon;

    @Column(nullable = false)
    private Integer points;

    @Column(name = "team_size", nullable = false)
    private Integer teamSize;

    @Column(name = "team_value", nullable = false)
    private Long teamValue;

    @Column(name = "team_value_inc", nullable = false)
    private Long teamValueInc;

    @Column(nullable = false)
    private Integer position;

    @Column(nullable = false, length = 50)
    private String role;

    @Column
    private Long cash;

    @Column(name = "maximum_bid")
    private Long maximumBid;

    @ManyToOne(optional = false)
    @JoinColumn(name = "league_id", nullable = false)
    private League league;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Manager() {
    }

    public Manager(
            Long biwengerManagerId,
            String name,
            String icon,
            Integer points,
            Integer teamSize,
            Long teamValue,
            Long teamValueInc,
            Integer position,
            String role,
            League league) {
        this.biwengerManagerId = biwengerManagerId;
        this.name = name;
        this.icon = icon;
        this.points = points;
        this.teamSize = teamSize;
        this.teamValue = teamValue;
        this.teamValueInc = teamValueInc;
        this.position = position;
        this.role = role;
        this.league = league;
        this.cash = null;
        this.maximumBid = null;
    }

    public void updateFromBiwenger(
            String name,
            String icon,
            Integer points,
            Integer teamSize,
            Long teamValue,
            Long teamValueInc,
            Integer position,
            String role) {
        this.name = name;
        this.icon = icon;
        this.points = points;
        this.teamSize = teamSize;
        this.teamValue = teamValue;
        this.teamValueInc = teamValueInc;
        this.position = position;
        this.role = role;
    }

    public void updateCash(Long cash) {
        this.cash = cash;
    }

    public void updateEconomicStatus(
            Long cash,
            Long maximumBid) {
        this.cash = cash;
        this.maximumBid = maximumBid;
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

    public Long getBiwengerManagerId() {
        return biwengerManagerId;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public Integer getPoints() {
        return points;
    }

    public Integer getTeamSize() {
        return teamSize;
    }

    public Long getTeamValue() {
        return teamValue;
    }

    public Long getTeamValueInc() {
        return teamValueInc;
    }

    public Integer getPosition() {
        return position;
    }

    public String getRole() {
        return role;
    }

    public Long getCash() {
        return cash;
    }

    public Long getMaximumBid() {
        return maximumBid;
    }

    public League getLeague() {
        return league;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isLeagueAdministrator() {
        return "manager".equalsIgnoreCase(role);
    }
}