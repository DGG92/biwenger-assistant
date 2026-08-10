package com.artajerjes.biwengerassistant.offer;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.offer.dto.EconomicStatusResponse;
import com.artajerjes.biwengerassistant.offer.dto.OfferResponse;
import com.artajerjes.biwengerassistant.offer.dto.OfferSyncResponse;

@RestController
@RequestMapping("/api/leagues/{leagueId}/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(
            OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/sync")
    public OfferSyncResponse sync(
            @PathVariable Long leagueId) {
        return offerService.sync(leagueId);
    }

    @GetMapping
    public List<OfferResponse> findAll(
            @PathVariable Long leagueId) {
        return offerService.findAll(leagueId);
    }

    @GetMapping("/economic-status")
    public EconomicStatusResponse getEconomicStatus(
            @PathVariable Long leagueId) {
        return offerService.getEconomicStatus();
    }
}