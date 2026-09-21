import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Movement } from '../models/movement.model';
import { API_CONFIG } from '../config/api.config';

@Injectable({
    providedIn: 'root',
})
export class MovementService {
    constructor(private readonly http: HttpClient) { }

    getMovements(leagueId: number): Observable<Movement[]> {
        return this.http.get<Movement[]>(
            `${API_CONFIG.baseUrl}/leagues/${leagueId}/movements`,
        );
    }
}