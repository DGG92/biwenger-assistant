import { HttpClient } from '@angular/common/http';
import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap, timeout } from 'rxjs';

import { API_CONFIG } from '../config/api.config';

export type AssistantRole = 'ADMIN' | 'USER';

export interface CurrentUser {
    id: number;
    username: string;
    role: AssistantRole;
    managerId: number | null;
    leagueId: number | null;
}

export interface LoginRequest {
    username: string;
    password: string;
}

@Injectable({
    providedIn: 'root',
})
export class AuthService {

    private readonly http = inject(HttpClient);

    private readonly currentUserSignal = signal<CurrentUser | null>(null);

    readonly currentUser = this.currentUserSignal.asReadonly();

    login(request: LoginRequest): Observable<CurrentUser> {
        return this.http.post<CurrentUser>(
            `${API_CONFIG.baseUrl}/auth/login`,
            request,
            { withCredentials: true }
        ).pipe(
            timeout(10000),
            tap((user) => this.currentUserSignal.set(user))
        );
    }

    loadCurrentUser(): Observable<CurrentUser> {
        return this.http.get<CurrentUser>(
            `${API_CONFIG.baseUrl}/auth/me`,
            { withCredentials: true }
        ).pipe(
            tap((user) => this.currentUserSignal.set(user))
        );
    }

    logout(): Observable<void> {
        return this.http.post<void>(
            `${API_CONFIG.baseUrl}/auth/logout`,
            {},
            { withCredentials: true }
        ).pipe(
            tap(() => this.currentUserSignal.set(null))
        );
    }

    isAdmin(): boolean {
        return this.currentUserSignal()?.role === 'ADMIN';
    }
}