import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { of, throwError } from 'rxjs';

import { AuthService, CurrentUser } from '../services/auth';
import { authGuard } from './auth.guard';

describe('authGuard', () => {
    const currentUser = signal<CurrentUser | null>(null);

    const authServiceMock = {
        currentUser: currentUser.asReadonly(),
        loadCurrentUser: vi.fn(),
    };

    const routerMock = {
        createUrlTree: vi.fn(
            (commands: string[]) => ({ commands }) as unknown as UrlTree
        ),
    };

    beforeEach(() => {
        currentUser.set(null);
        authServiceMock.loadCurrentUser.mockReset();
        routerMock.createUrlTree.mockClear();

        TestBed.configureTestingModule({
            providers: [
                { provide: AuthService, useValue: authServiceMock },
                { provide: Router, useValue: routerMock },
            ],
        });
    });

    it('should allow an already authenticated user', () => {
        currentUser.set({
            id: 1,
            username: 'diego',
            role: 'ADMIN',
            managerId: 10,
            leagueId: 1,
        });

        const result = TestBed.runInInjectionContext(
            () => authGuard({} as any, {} as any)
        );

        expect(result).toBe(true);
        expect(authServiceMock.loadCurrentUser).not.toHaveBeenCalled();
    });

    it('should restore the session when no user is loaded yet', () => {
        authServiceMock.loadCurrentUser.mockReturnValue(of({
            id: 2,
            username: 'jordi',
            role: 'USER',
            managerId: 20,
            leagueId: 1,
        }));

        const result = TestBed.runInInjectionContext(
            () => authGuard({} as any, {} as any)
        );

        let allowed: boolean | UrlTree | undefined;

        if (typeof result !== 'boolean' && !(result instanceof UrlTree)) {
            (result as any).subscribe((value: boolean | UrlTree) => {
                allowed = value;
            });
        }

        expect(allowed).toBe(true);
        expect(authServiceMock.loadCurrentUser).toHaveBeenCalledOnce();
    });

    it('should redirect to login when session restoration fails', () => {
        authServiceMock.loadCurrentUser.mockReturnValue(
            throwError(() => new Error('Unauthorized'))
        );

        const loginTree = { login: true } as unknown as UrlTree;
        routerMock.createUrlTree.mockReturnValue(loginTree);

        const result = TestBed.runInInjectionContext(
            () => authGuard({} as any, {} as any)
        );

        let redirected: boolean | UrlTree | undefined;

        (result as any).subscribe((value: boolean | UrlTree) => {
            redirected = value;
        });

        expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/login']);
        expect(redirected).toBe(loginTree);
    });
});
