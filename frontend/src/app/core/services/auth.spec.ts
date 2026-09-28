import {
    provideHttpClient,
    withInterceptorsFromDi,
} from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_CONFIG } from '../config/api.config';
import { AuthService, CurrentUser } from './auth';
import { CsrfTokenService } from './csrf-token';

describe('AuthService', () => {
    let service: AuthService;
    let httpTesting: HttpTestingController;
    let csrfTokenService: CsrfTokenService;

    const user: CurrentUser = {
        id: 7,
        username: 'jordi',
        role: 'USER',
        managerId: 12,
        leagueId: 42,
    };

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                AuthService,
                CsrfTokenService,
                provideHttpClient(withInterceptorsFromDi()),
                provideHttpClientTesting(),
            ],
        });

        service = TestBed.inject(AuthService);
        httpTesting = TestBed.inject(HttpTestingController);
        csrfTokenService = TestBed.inject(CsrfTokenService);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('should login, load CSRF and expose the authenticated user', () => {
        let result: CurrentUser | undefined;

        service.login({
            username: 'jordi',
            password: 'secret',
        }).subscribe((value) => result = value);

        const loginRequest = httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/auth/login`
        );

        expect(loginRequest.request.method).toBe('POST');
        expect(loginRequest.request.withCredentials).toBe(true);
        expect(loginRequest.request.body).toEqual({
            username: 'jordi',
            password: 'secret',
        });

        loginRequest.flush(user);

        const csrfRequest = httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/auth/csrf`
        );

        expect(csrfRequest.request.method).toBe('GET');
        expect(csrfRequest.request.withCredentials).toBe(true);

        csrfRequest.flush({
            token: 'csrf-login',
            headerName: 'X-XSRF-TOKEN',
            parameterName: '_csrf',
        });

        expect(result).toEqual(user);
        expect(service.currentUser()).toEqual(user);
        expect(csrfTokenService.token()).toBe('csrf-login');
    });

    it('should restore the current user and refresh CSRF', () => {
        service.loadCurrentUser().subscribe();

        const meRequest = httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/auth/me`
        );

        expect(meRequest.request.method).toBe('GET');
        expect(meRequest.request.withCredentials).toBe(true);

        meRequest.flush(user);

        const csrfRequest = httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/auth/csrf`
        );

        csrfRequest.flush({
            token: 'csrf-restored',
            headerName: 'X-XSRF-TOKEN',
            parameterName: '_csrf',
        });

        expect(service.currentUser()).toEqual(user);
        expect(csrfTokenService.token()).toBe('csrf-restored');
    });

    it('should clear the authenticated user and CSRF on logout', () => {
        service.loadCurrentUser().subscribe();

        httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/auth/me`
        ).flush(user);

        httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/auth/csrf`
        ).flush({
            token: 'csrf-before-logout',
            headerName: 'X-XSRF-TOKEN',
            parameterName: '_csrf',
        });

        expect(service.currentUser()).toEqual(user);
        expect(csrfTokenService.token()).toBe('csrf-before-logout');

        service.logout().subscribe();

        const logoutRequest = httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/auth/logout`
        );

        expect(logoutRequest.request.method).toBe('POST');
        expect(logoutRequest.request.withCredentials).toBe(true);

        logoutRequest.flush(null);

        expect(service.currentUser()).toBeNull();
        expect(csrfTokenService.token()).toBeNull();
    });
});
