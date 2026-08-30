import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import { SquadNeeds } from '../models/squad-needs.model';
import { EconomicStatus } from '../models/economic-status.model';
import { MarketRecommendation } from '../models/market-recommendation.model';
import { ActionRecommendation } from '../models/action-recommendation.model';
import { RecommendedLineup } from '../models/recommended-lineup.model';

@Injectable({
    providedIn: 'root',
})
export class RecommendationService {
    private readonly http = inject(HttpClient);

    getSquadNeeds(): Observable<SquadNeeds> {
        return this.http.get<SquadNeeds>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/recommendations/squad-needs`
        );
    }

    getEconomicStatus(): Observable<EconomicStatus> {
        return this.http.get<EconomicStatus>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/offers/economic-status`
        );
    }

    getMarketRecommendations(): Observable<MarketRecommendation[]> {
        return this.http.get<MarketRecommendation[]>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/recommendations/market`
        );
    }

    getActions(): Observable<ActionRecommendation[]> {
        return this.http.get<ActionRecommendation[]>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/recommendations/actions`
        );
    }

    getRecommendedLineup(): Observable<RecommendedLineup> {
        return this.http.get<RecommendedLineup>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/recommendations/lineup`
        );
    }
}