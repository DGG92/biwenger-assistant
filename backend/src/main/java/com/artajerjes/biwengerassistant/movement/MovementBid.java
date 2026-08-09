package com.artajerjes.biwengerassistant.movement;

import com.artajerjes.biwengerassistant.manager.Manager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "movement_bids")
public class MovementBid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "movement_id", nullable = false)
    private Movement movement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private Manager manager;

    @Column(nullable = false)
    private Long amount;

    protected MovementBid() {
    }

    public MovementBid(
            Manager manager,
            Long amount) {
        this.manager = manager;
        this.amount = amount;
    }

    void setMovement(Movement movement) {
        this.movement = movement;
    }

    public Long getId() {
        return id;
    }

    public Movement getMovement() {
        return movement;
    }

    public Manager getManager() {
        return manager;
    }

    public Long getAmount() {
        return amount;
    }
}