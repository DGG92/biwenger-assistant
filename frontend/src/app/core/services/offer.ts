import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import {
    EconomicStatus,
    Offer,
} from '../models/offer.model';

@Injectable({
    providedIn: 'root',
})
export class OfferService {
    private readonly http = inject(HttpClient);

    getOffers(
        leagueId: number
    ): Observable<Offer[]> {
        return this.http.get<Offer[]>(
            `${API_CONFIG.baseUrl}/leagues/${leagueId}/offers`
        );
    }

    getEconomicStatus(
        leagueId: number
    ): Observable<EconomicStatus> {
        return this.http.get<EconomicStatus>(
            `${API_CONFIG.baseUrl}/leagues/${leagueId}/offers/economic-status`
        );
    }

    syncOffers(
        leagueId: number
    ): Observable<unknown> {
        return this.http.post(
            `${API_CONFIG.baseUrl}/leagues/${leagueId}/offers/sync`,
            {}
        );
    }
}