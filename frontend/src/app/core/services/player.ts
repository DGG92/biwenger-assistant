import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import { LeagueStatistics } from '../models/league-statistics.model';
import { Player } from '../models/player.model';
import { LeagueContextService } from './league-context';

@Injectable({
    providedIn: 'root',
})
export class PlayerService {

    private readonly http = inject(HttpClient);
    private readonly leagueContext = inject(LeagueContextService);

    getPlayers(): Observable<Player[]> {
        const leagueId = this.leagueContext.requireLeagueId();

        return this.http.get<Player[]>(
            `${API_CONFIG.baseUrl}/leagues/${leagueId}/players`
        );
    }

    getStatistics(): Observable<LeagueStatistics> {
        const leagueId = this.leagueContext.requireLeagueId();

        return this.http.get<LeagueStatistics>(
            `${API_CONFIG.baseUrl}/leagues/${leagueId}/players/statistics`
        );
    }
}