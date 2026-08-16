import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Movement } from '../models/movement.model';

@Injectable({
    providedIn: 'root',
})
export class MovementService {
    private readonly baseUrl = 'http://localhost:8080/api';

    constructor(private readonly http: HttpClient) { }

    getMovements(leagueId: number): Observable<Movement[]> {
        return this.http.get<Movement[]>(
            `${this.baseUrl}/leagues/${leagueId}/movements`,
        );
    }
}