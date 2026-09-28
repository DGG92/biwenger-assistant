import {
    HttpClient,
    provideHttpClient,
    withInterceptors,
} from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { CsrfTokenService } from '../services/csrf-token';
import { credentialsInterceptor } from './credentials.interceptor';

describe('credentialsInterceptor', () => {
    let http: HttpClient;
    let httpTesting: HttpTestingController;
    let csrfTokenService: CsrfTokenService;

    beforeEach(() => {
        TestBed.configureTestingModule({
            providers: [
                CsrfTokenService,
                provideHttpClient(
                    withInterceptors([credentialsInterceptor])
                ),
                provideHttpClientTesting(),
            ],
        });

        http = TestBed.inject(HttpClient);
        httpTesting = TestBed.inject(HttpTestingController);
        csrfTokenService = TestBed.inject(CsrfTokenService);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('should send credentials on GET without a CSRF header', () => {
        csrfTokenService.setToken('csrf-token');

        http.get('/api/test').subscribe();

        const request = httpTesting.expectOne('/api/test');

        expect(request.request.withCredentials).toBe(true);
        expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);

        request.flush({});
    });

    it('should add CSRF to mutating requests', () => {
        csrfTokenService.setToken('csrf-token');

        http.post('/api/test', { value: 1 }).subscribe();

        const request = httpTesting.expectOne('/api/test');

        expect(request.request.withCredentials).toBe(true);
        expect(
            request.request.headers.get('X-XSRF-TOKEN')
        ).toBe('csrf-token');

        request.flush({});
    });

    it('should not invent a CSRF header when no token exists', () => {
        http.put('/api/test', { value: 1 }).subscribe();

        const request = httpTesting.expectOne('/api/test');

        expect(request.request.withCredentials).toBe(true);
        expect(request.request.headers.has('X-XSRF-TOKEN')).toBe(false);

        request.flush({});
    });
});
