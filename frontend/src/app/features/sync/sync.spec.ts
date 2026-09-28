import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, Subject, throwError } from 'rxjs';

import {
    SyncService,
    SyncStatusResponse,
} from '../../core/services/sync';
import { Sync } from './sync';

describe('Sync', () => {
    let fixture: ComponentFixture<Sync>;
    let component: Sync;

    const syncServiceMock = {
        getStatus: vi.fn(),
        syncNow: vi.fn(),
    };

    const createStatus = (
        executionStatus: string
    ): SyncStatusResponse => ({
        leagueId: 1,

        scheduler: {
            enabled: true,
            intervalMs: 300000,
        },

        execution: {
            status: executionStatus,
            startedAt: '2026-09-28T08:00:00Z',
            finishedAt:
                executionStatus === 'RUNNING'
                    ? null
                    : '2026-09-28T08:05:00Z',
            lastError:
                executionStatus === 'FAILED'
                    ? 'Test error'
                    : null,
        },

        details: {
            state: 'READY',
            lastRateLimitAt: null,
            rateLimitedPlayerId: null,
            retryAfterSeconds: null,
            cooldownUntil: null,
        },

        players: {
            total: 628,
            eligible: 549,

            reports: {
                completed: 549,
                pending: 0,
                coveragePercent: 100,
                oldestSuccessAt: '2026-09-28T06:00:00Z',
                lastSuccessAt: '2026-09-28T08:00:00Z',
                lastAttemptAt: '2026-09-28T08:00:00Z',
            },

            priceHistory: {
                completed: 549,
                pending: 0,
                coveragePercent: 100,
            },
        },
    });

    beforeEach(async () => {
        syncServiceMock.getStatus.mockReset();
        syncServiceMock.syncNow.mockReset();

        await TestBed.configureTestingModule({
            imports: [Sync],
            providers: [
                {
                    provide: SyncService,
                    useValue: syncServiceMock,
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(Sync);
        component = fixture.componentInstance;
    });

    afterEach(() => {
        component.ngOnDestroy();
        vi.useRealTimers();
    });

    it('should load the current sync status', () => {
        const status = createStatus('SUCCESS');
        syncServiceMock.getStatus.mockReturnValue(of(status));

        component.loadStatus();

        expect(syncServiceMock.getStatus).toHaveBeenCalledOnce();
        expect(component.status()).toEqual(status);
        expect(component.loading()).toBe(false);
        expect(component.loadError()).toBe(false);
        expect(component.syncing()).toBe(false);
    });

    it('should expose a load error when status cannot be loaded', () => {
        syncServiceMock.getStatus.mockReturnValue(
            throwError(() => new Error('Status unavailable'))
        );

        component.loadStatus();

        expect(component.loading()).toBe(false);
        expect(component.loadError()).toBe(true);
    });

    it('should not start another manual sync while one is already running', () => {
        syncServiceMock.getStatus.mockReturnValue(
            of(createStatus('RUNNING'))
        );

        component.loadStatus();

        expect(component.syncing()).toBe(true);

        component.syncNow();

        expect(syncServiceMock.syncNow).not.toHaveBeenCalled();
    });

    it('should show that a manual synchronization has started', () => {
        const runningStatus = createStatus('RUNNING');

        syncServiceMock.syncNow.mockReturnValue(of({
            leagueId: 1,
            started: true,
            status: 'RUNNING',
        }));

        syncServiceMock.getStatus.mockReturnValue(of(runningStatus));

        component.syncNow();

        expect(syncServiceMock.syncNow).toHaveBeenCalledOnce();
        expect(component.syncing()).toBe(true);
        expect(component.successMessage()).toBe(
            'Sincronización iniciada correctamente.'
        );
        expect(component.errorMessage()).toBe('');
    });

    it('should report SUCCESS when polling detects manual sync completion', () => {
        vi.useFakeTimers();

        const pollingStatus$ = new Subject<SyncStatusResponse>();

        syncServiceMock.syncNow.mockReturnValue(of({
            leagueId: 1,
            started: true,
            status: 'RUNNING',
        }));

        syncServiceMock.getStatus
            .mockReturnValueOnce(of(createStatus('RUNNING')))
            .mockReturnValueOnce(pollingStatus$.asObservable());

        component.syncNow();

        expect(component.successMessage()).toBe(
            'Sincronización iniciada correctamente.'
        );

        vi.advanceTimersByTime(3000);

        pollingStatus$.next(createStatus('SUCCESS'));
        pollingStatus$.complete();

        expect(component.syncing()).toBe(false);
        expect(component.successMessage()).toBe(
            'Sincronización completada correctamente.'
        );
        expect(component.errorMessage()).toBe('');
    });

    it('should report PARTIAL when polling detects partial manual completion', () => {
        vi.useFakeTimers();

        const pollingStatus$ = new Subject<SyncStatusResponse>();

        syncServiceMock.syncNow.mockReturnValue(of({
            leagueId: 1,
            started: true,
            status: 'RUNNING',
        }));

        syncServiceMock.getStatus
            .mockReturnValueOnce(of(createStatus('RUNNING')))
            .mockReturnValueOnce(pollingStatus$.asObservable());

        component.syncNow();

        vi.advanceTimersByTime(3000);

        pollingStatus$.next(createStatus('PARTIAL'));
        pollingStatus$.complete();

        expect(component.syncing()).toBe(false);
        expect(component.successMessage()).toBe(
            'Sincronización completada parcialmente.'
        );
        expect(component.errorMessage()).toBe('');
    });

    it('should report FAILED when polling detects failed manual completion', () => {
        vi.useFakeTimers();

        const pollingStatus$ = new Subject<SyncStatusResponse>();

        syncServiceMock.syncNow.mockReturnValue(of({
            leagueId: 1,
            started: true,
            status: 'RUNNING',
        }));

        syncServiceMock.getStatus
            .mockReturnValueOnce(of(createStatus('RUNNING')))
            .mockReturnValueOnce(pollingStatus$.asObservable());

        component.syncNow();

        vi.advanceTimersByTime(3000);

        pollingStatus$.next(createStatus('FAILED'));
        pollingStatus$.complete();

        expect(component.syncing()).toBe(false);
        expect(component.successMessage()).toBe('');
        expect(component.errorMessage()).toBe(
            'La sincronización ha finalizado con errores.'
        );
    });

    it('should recover when starting a manual synchronization fails', () => {
        syncServiceMock.syncNow.mockReturnValue(
            throwError(() => new Error('Sync unavailable'))
        );

        component.syncNow();

        expect(component.syncing()).toBe(false);
        expect(component.successMessage()).toBe('');
        expect(component.errorMessage()).toBe(
            'No se ha podido iniciar la sincronización.'
        );
    });

    it('should format execution and detail states', () => {
        expect(component.formatExecutionStatus('IDLE')).toBe('Pendiente');
        expect(component.formatExecutionStatus('RUNNING')).toBe('En curso');
        expect(component.formatExecutionStatus('SUCCESS')).toBe('Correcta');
        expect(component.formatExecutionStatus('PARTIAL')).toBe('Parcial');
        expect(component.formatExecutionStatus('FAILED')).toBe('Con errores');

        expect(component.formatDetailStatus('READY')).toBe('Disponible');
        expect(component.formatDetailStatus('RATE_LIMITED')).toBe(
            'Limitado temporalmente'
        );
    });

    it('should format the dynamic scheduler interval', () => {
        expect(component.formatInterval(300000)).toBe('5 min');
        expect(component.formatInterval(3600000)).toBe('1 h');
        expect(component.formatInterval(5400000)).toBe('1 h 30 min');
    });
});


