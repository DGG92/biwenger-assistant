package com.artajerjes.biwengerassistant.league;

import java.util.List;

import com.artajerjes.biwengerassistant.league.dto.CreateLeagueRequest;
import com.artajerjes.biwengerassistant.league.dto.LeagueResponse;
import com.artajerjes.biwengerassistant.league.dto.UpdateLeagueRequest;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leagues")
public class LeagueController {

    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LeagueResponse create(
            @Valid @RequestBody CreateLeagueRequest request
    ) {
        return leagueService.create(request);
    }

    @GetMapping
    public List<LeagueResponse> findAll() {
        return leagueService.findAll();
    }

    @GetMapping("/{id}")
    public LeagueResponse findById(@PathVariable Long id) {
        return leagueService.findById(id);
    }

    @PutMapping("/{id}")
    public LeagueResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateLeagueRequest request
    ) {
        return leagueService.update(id, request);
    }
    
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        leagueService.delete(id);
    }
}