import { computed, inject, Injectable } from '@angular/core';

import { AuthService } from './auth';

@Injectable({
    providedIn: 'root',
})
export class LeagueContextService {

    private readonly authService = inject(AuthService);

    readonly leagueId = computed(
        () => this.authService.currentUser()?.leagueId ?? null
    );

    requireLeagueId(): number {
        const leagueId = this.leagueId();

        if (leagueId === null) {
            throw new Error(
                'The authenticated user has no league assigned'
            );
        }

        return leagueId;
    }
}