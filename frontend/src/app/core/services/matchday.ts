import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import { MatchdayResponse, MatchdayRoundOption } from '../models/matchday.model';

@Injectable({
    providedIn: 'root',
})
export class MatchdayService {
    private readonly http = inject(HttpClient);

    getCurrentMatchday(): Observable<MatchdayResponse> {
        return this.http.get<MatchdayResponse>(
            `${API_CONFIG.baseUrl}/matchday`
        );
    }

    getAvailableRounds(): Observable<MatchdayRoundOption[]> {
        return this.http.get<MatchdayRoundOption[]>(
            `${API_CONFIG.baseUrl}/matchday/rounds`
        );
    }

    getMatchday(roundId: number): Observable<MatchdayResponse> {
        return this.http.get<MatchdayResponse>(
            `${API_CONFIG.baseUrl}/matchday/${roundId}`
        );
    }
}