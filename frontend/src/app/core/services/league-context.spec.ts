import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';

import { AuthService, CurrentUser } from './auth';
import { LeagueContextService } from './league-context';

describe('LeagueContextService', () => {
    const currentUser = signal<CurrentUser | null>(null);

    let service: LeagueContextService;

    beforeEach(() => {
        currentUser.set(null);

        TestBed.configureTestingModule({
            providers: [
                LeagueContextService,
                {
                    provide: AuthService,
                    useValue: {
                        currentUser: currentUser.asReadonly(),
                    },
                },
            ],
        });

        service = TestBed.inject(LeagueContextService);
    });

    it('should expose the league assigned to the authenticated user', () => {
        currentUser.set({
            id: 1,
            username: 'diego',
            role: 'ADMIN',
            managerId: 10,
            leagueId: 27,
        });

        expect(service.leagueId()).toBe(27);
        expect(service.requireLeagueId()).toBe(27);
    });

    it('should react when the authenticated user league changes', () => {
        currentUser.set({
            id: 1,
            username: 'diego',
            role: 'ADMIN',
            managerId: 10,
            leagueId: 27,
        });

        expect(service.leagueId()).toBe(27);

        currentUser.set({
            id: 2,
            username: 'jordi',
            role: 'USER',
            managerId: 20,
            leagueId: 84,
        });

        expect(service.leagueId()).toBe(84);
        expect(service.requireLeagueId()).toBe(84);
    });

    it('should fail clearly when the authenticated user has no league', () => {
        currentUser.set({
            id: 1,
            username: 'diego',
            role: 'ADMIN',
            managerId: null,
            leagueId: null,
        });

        expect(service.leagueId()).toBeNull();
        expect(() => service.requireLeagueId()).toThrowError(
            'The authenticated user has no league assigned'
        );
    });
});
