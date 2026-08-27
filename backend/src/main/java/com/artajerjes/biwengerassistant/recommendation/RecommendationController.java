package com.artajerjes.biwengerassistant.recommendation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.recommendation.dto.FormationRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;

@RestController
@RequestMapping("/api/leagues/{leagueId}/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(
            RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping("/market")
    public List<MarketRecommendationResponse> getMarketRecommendations(
            @PathVariable Long leagueId) {
        return recommendationService
                .getMarketRecommendations(leagueId);
    }

    @GetMapping("/squad-needs")
    public SquadNeedsResponse getSquadNeeds(
            @PathVariable Long leagueId) {
        return recommendationService
                .getSquadNeeds(leagueId);
    }

    @GetMapping("/formation")
    public FormationRecommendationResponse getFormationRecommendation(
            @PathVariable Long leagueId) {

        return recommendationService
                .getFormationRecommendation(leagueId);
    }
}