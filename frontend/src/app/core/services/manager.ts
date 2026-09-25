import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import { Manager } from '../models/manager.model';
import { LeagueContextService } from './league-context';

@Injectable({
    providedIn: 'root',
})
export class ManagerService {

    private readonly http = inject(HttpClient);
    private readonly leagueContext = inject(LeagueContextService);

    getManagers(): Observable<Manager[]> {
        const leagueId = this.leagueContext.requireLeagueId();

        return this.http.get<Manager[]>(
            `${API_CONFIG.baseUrl}/leagues/${leagueId}/managers`
        );
    }
}