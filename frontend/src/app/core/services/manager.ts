import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';
import { Manager } from '../models/manager.model';

@Injectable({
    providedIn: 'root',
})
export class ManagerService {
    private readonly http = inject(HttpClient);

    getManagers(): Observable<Manager[]> {
        return this.http.get<Manager[]>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/managers`
        );
    }
}