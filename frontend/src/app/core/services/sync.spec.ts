import { provideHttpClient } from '@angular/common/http';
import {
    HttpTestingController,
    provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { API_CONFIG } from '../config/api.config';
import { LeagueContextService } from './league-context';
import { SyncService } from './sync';

describe('SyncService', () => {
    let service: SyncService;
    let httpTesting: HttpTestingController;

    const leagueContextMock = {
        requireLeagueId: vi.fn(),
    };

    beforeEach(() => {
        leagueContextMock.requireLeagueId.mockReset();
        leagueContextMock.requireLeagueId.mockReturnValue(73);

        TestBed.configureTestingModule({
            providers: [
                SyncService,
                {
                    provide: LeagueContextService,
                    useValue: leagueContextMock,
                },
                provideHttpClient(),
                provideHttpClientTesting(),
            ],
        });

        service = TestBed.inject(SyncService);
        httpTesting = TestBed.inject(HttpTestingController);
    });

    afterEach(() => {
        httpTesting.verify();
    });

    it('should request sync status for the current user league', () => {
        service.getStatus().subscribe();

        const request = httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/leagues/73/sync/status`
        );

        expect(leagueContextMock.requireLeagueId).toHaveBeenCalledOnce();
        expect(request.request.method).toBe('GET');
        expect(request.request.withCredentials).toBe(true);

        request.flush({});
    });

    it('should start manual sync for the current user league', () => {
        service.syncNow().subscribe();

        const request = httpTesting.expectOne(
            `${API_CONFIG.baseUrl}/leagues/73/sync/now`
        );

        expect(leagueContextMock.requireLeagueId).toHaveBeenCalledOnce();
        expect(request.request.method).toBe('POST');
        expect(request.request.body).toEqual({});
        expect(request.request.withCredentials).toBe(true);

        request.flush({
            leagueId: 73,
            started: true,
            status: 'RUNNING',
        });
    });
});
