import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { API_CONFIG } from '../config/api.config';

export interface SyncStatusResponse {
    leagueId: number;

    scheduler: {
        enabled: boolean;
        intervalMs: number;
    };

    execution: {
        status: string;
        startedAt: string | null;
        finishedAt: string | null;
        lastError: string | null;
    };

    details: {
        state: string;
        lastRateLimitAt: string | null;
        rateLimitedPlayerId: number | null;
        retryAfterSeconds: number | null;
        cooldownUntil: string | null;
    };

    players: {
        total: number;
        eligible: number;

        reports: {
            completed: number;
            pending: number;
            coveragePercent: number;
            oldestSuccessAt: string | null;
            lastSuccessAt: string | null;
            lastAttemptAt: string | null;
        };

        priceHistory: {
            completed: number;
            pending: number;
            coveragePercent: number;
        };
    };
}

@Injectable({
    providedIn: 'root',
})
export class SyncService {

    private readonly http = inject(HttpClient);

    getStatus(): Observable<SyncStatusResponse> {
        return this.http.get<SyncStatusResponse>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/sync/status`,
            { withCredentials: true }
        );
    }

    syncNow(): Observable<SyncNowResponse> {
        return this.http.post<SyncNowResponse>(
            `${API_CONFIG.baseUrl}/leagues/${API_CONFIG.leagueId}/sync/now`,
            {},
            { withCredentials: true }
        );
    }
}

export interface SyncNowResponse {
    leagueId: number;
    started: boolean;
    status: string;
}