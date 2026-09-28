import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';

import { AuthService } from '../services/auth';
import { adminGuard } from './admin.guard';

describe('adminGuard', () => {
    const authServiceMock = {
        isAdmin: vi.fn(),
    };

    const routerMock = {
        createUrlTree: vi.fn(),
    };

    beforeEach(() => {
        authServiceMock.isAdmin.mockReset();
        routerMock.createUrlTree.mockReset();

        TestBed.configureTestingModule({
            providers: [
                { provide: AuthService, useValue: authServiceMock },
                { provide: Router, useValue: routerMock },
            ],
        });
    });

    it('should allow an Assistant ADMIN', () => {
        authServiceMock.isAdmin.mockReturnValue(true);

        const result = TestBed.runInInjectionContext(
            () => adminGuard({} as any, {} as any)
        );

        expect(result).toBe(true);
        expect(routerMock.createUrlTree).not.toHaveBeenCalled();
    });

    it('should redirect a normal Assistant USER to dashboard', () => {
        authServiceMock.isAdmin.mockReturnValue(false);

        const dashboardTree = { dashboard: true } as unknown as UrlTree;
        routerMock.createUrlTree.mockReturnValue(dashboardTree);

        const result = TestBed.runInInjectionContext(
            () => adminGuard({} as any, {} as any)
        );

        expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/dashboard']);
        expect(result).toBe(dashboardTree);
    });
});
