package com.artajerjes.biwengerassistant.manager;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.manager.dto.ManagerResponse;
import com.artajerjes.biwengerassistant.manager.dto.ManagerSyncResponse;
import com.artajerjes.biwengerassistant.manager.dto.SquadProfitabilityResponse;

@RestController
@RequestMapping("/api/leagues/{leagueId}/managers")
public class ManagerController {

    private final ManagerService managerService;
    private final SquadProfitabilityService squadProfitabilityService;

    public ManagerController(
            ManagerService managerService,
            SquadProfitabilityService squadProfitabilityService) {

        this.managerService = managerService;
        this.squadProfitabilityService = squadProfitabilityService;
    }

    @PostMapping("/sync")
    public ManagerSyncResponse sync(
            @PathVariable Long leagueId) {
        return managerService.sync(leagueId);
    }

    @GetMapping
    public List<ManagerResponse> findAll(
            @PathVariable Long leagueId) {
        return managerService.findAll(leagueId);
    }

    @GetMapping("/{managerId}/profitability")
    public SquadProfitabilityResponse getSquadProfitability(
            @PathVariable Long leagueId,
            @PathVariable Long managerId) {

        return squadProfitabilityService.getSquadProfitability(
                leagueId,
                managerId);
    }
}