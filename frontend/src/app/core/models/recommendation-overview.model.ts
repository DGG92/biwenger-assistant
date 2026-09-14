import { ActionRecommendation } from './action-recommendation.model';
import { MarketRecommendation } from './market-recommendation.model';

export interface RecommendationOverview {
    market: MarketRecommendation[];
    actions: ActionRecommendation[];
}