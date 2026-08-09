package com.artajerjes.biwengerassistant.market;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.market.dto.MarketListingResponse;
import com.artajerjes.biwengerassistant.market.dto.MarketSyncResponse;

@RestController
@RequestMapping("/api/leagues/{leagueId}/market")
public class MarketController {

    private final MarketService marketService;

    public MarketController(MarketService marketService) {
        this.marketService = marketService;
    }

    @PostMapping("/sync")
    public MarketSyncResponse sync(
            @PathVariable Long leagueId) {
        return marketService.sync(leagueId);
    }

    @GetMapping
    public List<MarketListingResponse> findAll(
            @PathVariable Long leagueId) {
        return marketService.findAll(leagueId);
    }
}