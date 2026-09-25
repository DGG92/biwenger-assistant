import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import { ActionRecommendation } from '../models/action-recommendation.model';
import { EconomicStatus } from '../models/economic-status.model';
import { MarketRecommendation } from '../models/market-recommendation.model';
import { RecommendedLineup } from '../models/recommended-lineup.model';
import { RecommendationOverview } from '../models/recommendation-overview.model';
import { SquadNeeds } from '../models/squad-needs.model';
import { SquadProfitability } from '../models/squad-profitability.model';
import { LeagueContextService } from './league-context';

@Injectable({
    providedIn: 'root',
})
export class RecommendationService {

    private readonly http = inject(HttpClient);
    private readonly leagueContext = inject(LeagueContextService);

    getSquadNeeds(): Observable<SquadNeeds> {
        return this.http.get<SquadNeeds>(
            `${this.leagueUrl()}/recommendations/squad-needs`
        );
    }

    getEconomicStatus(): Observable<EconomicStatus> {
        return this.http.get<EconomicStatus>(
            `${this.leagueUrl()}/offers/economic-status`
        );
    }

    getMarketRecommendations(): Observable<MarketRecommendation[]> {
        return this.http.get<MarketRecommendation[]>(
            `${this.leagueUrl()}/recommendations/market`
        );
    }

    getActions(): Observable<ActionRecommendation[]> {
        return this.http.get<ActionRecommendation[]>(
            `${this.leagueUrl()}/recommendations/actions`
        );
    }

    getOverview(): Observable<RecommendationOverview> {
        return this.http.get<RecommendationOverview>(
            `${this.leagueUrl()}/recommendations/overview`
        );
    }

    getRecommendedLineup(): Observable<RecommendedLineup> {
        return this.http.get<RecommendedLineup>(
            `${this.leagueUrl()}/recommendations/lineup`
        );
    }

    getSquadProfitability(
        managerId: number
    ): Observable<SquadProfitability> {
        return this.http.get<SquadProfitability>(
            `${this.leagueUrl()}/managers/${managerId}/profitability`
        );
    }

    private leagueUrl(): string {
        const leagueId = this.leagueContext.requireLeagueId();

        return `${API_CONFIG.baseUrl}/leagues/${leagueId}`;
    }
}