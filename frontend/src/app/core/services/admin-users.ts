import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable, timeout } from 'rxjs';

import { API_CONFIG } from '../config/api.config';

export interface AvailableManager {
    id: number;
    name: string;
    icon: string | null;
    leagueId: number;
}

export interface CreateAssistantUserRequest {
    username: string;
    password: string;
    managerId: number;
}

@Injectable({
    providedIn: 'root',
})
export class AdminUsersService {

    private readonly http = inject(HttpClient);

    getAvailableManagers(): Observable<AvailableManager[]> {
        return this.http.get<AvailableManager[]>(
            `${API_CONFIG.baseUrl}/admin/users/available-managers`,
            { withCredentials: true }
        );
    }

    createUser(
        request: CreateAssistantUserRequest
    ): Observable<void> {
        return this.http.post<void>(
            `${API_CONFIG.baseUrl}/admin/users`,
            request,
            { withCredentials: true }
        ).pipe(
            timeout(10000)
        );
    }
}