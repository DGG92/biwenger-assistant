package com.artajerjes.biwengerassistant.recommendation.action;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leagues/{leagueId}/recommendations/actions")
public class ActionRecommendationController {

    private final ActionRecommendationService actionRecommendationService;

    public ActionRecommendationController(
            ActionRecommendationService actionRecommendationService) {

        this.actionRecommendationService = actionRecommendationService;
    }

    @GetMapping("/squad")
    public List<ActionCandidate> getSquadActions(
            @PathVariable Long leagueId) {

        return actionRecommendationService
                .getSquadActions(leagueId);
    }

    @GetMapping("/market")
    public List<ActionCandidate> getMarketActions(
            @PathVariable Long leagueId) {

        return actionRecommendationService
                .getMarketActions(leagueId);
    }
}