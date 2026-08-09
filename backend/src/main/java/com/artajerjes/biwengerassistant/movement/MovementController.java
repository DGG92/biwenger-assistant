package com.artajerjes.biwengerassistant.movement;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.movement.dto.MovementResponse;
import com.artajerjes.biwengerassistant.movement.dto.MovementSyncResponse;

@RestController
@RequestMapping("/api/leagues/{leagueId}/movements")
public class MovementController {

    private final MovementService movementService;

    public MovementController(
            MovementService movementService) {
        this.movementService = movementService;
    }

    @PostMapping("/sync")
    public MovementSyncResponse sync(
            @PathVariable Long leagueId) {
        return movementService.sync(leagueId);
    }

    @GetMapping
    public List<MovementResponse> findAll(
            @PathVariable Long leagueId) {
        return movementService.findAll(leagueId);
    }
}