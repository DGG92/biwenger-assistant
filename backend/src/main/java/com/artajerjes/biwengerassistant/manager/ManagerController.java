package com.artajerjes.biwengerassistant.manager;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.manager.dto.ManagerSyncResponse;

@RestController
@RequestMapping("/api/leagues/{leagueId}/managers")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @PostMapping("/sync")
    public ManagerSyncResponse sync(
            @PathVariable Long leagueId
    ) {
        return managerService.sync(leagueId);
    }
}