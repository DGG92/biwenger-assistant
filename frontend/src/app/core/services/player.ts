import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import { Player } from '../models/player.model';

@Injectable({
    providedIn: 'root',
})
export class PlayerService {
    private readonly http = inject(HttpClient);

    getPlayers(): Observable<Player[]> {
        return this.http.get<Player[]>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/players`
        );
    }
}