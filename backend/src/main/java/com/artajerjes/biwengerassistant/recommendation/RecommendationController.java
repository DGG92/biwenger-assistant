package com.artajerjes.biwengerassistant.recommendation;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.recommendation.action.ActionCandidate;
import com.artajerjes.biwengerassistant.recommendation.action.ActionRecommendationService;
import com.artajerjes.biwengerassistant.recommendation.dto.FormationRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.MarketRecommendationResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendationOverviewResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.RecommendedLineupResponse;
import com.artajerjes.biwengerassistant.recommendation.dto.SquadNeedsResponse;

@RestController
@RequestMapping("/api/leagues/{leagueId}/recommendations")
public class RecommendationController {

        private final RecommendationService recommendationService;
        private final ActionRecommendationService actionRecommendationService;

        public RecommendationController(
                        RecommendationService recommendationService,
                        ActionRecommendationService actionRecommendationService) {

                this.recommendationService = recommendationService;
                this.actionRecommendationService = actionRecommendationService;
        }

        @GetMapping("/market")
        public List<MarketRecommendationResponse> getMarketRecommendations(
                        @PathVariable Long leagueId) {
                return recommendationService
                                .getMarketRecommendations(leagueId);
        }

        @GetMapping("/overview")
        public RecommendationOverviewResponse getOverview(
                        @PathVariable Long leagueId) {

                List<MarketRecommendationResponse> market = recommendationService
                                .getMarketRecommendations(leagueId);

                List<ActionCandidate> actions = actionRecommendationService
                                .getAllActions(
                                                leagueId,
                                                market);

                return new RecommendationOverviewResponse(
                                market,
                                actions);
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

        @GetMapping("/lineup")
        public RecommendedLineupResponse getRecommendedLineup(
                        @PathVariable Long leagueId) {

                return recommendationService
                                .getRecommendedLineup(leagueId);
        }
}