import {
    Component,
    OnDestroy,
    OnInit,
    inject,
    signal,
} from '@angular/core';

import {
    SyncService,
    SyncStatusResponse,
} from '../../core/services/sync';

@Component({
    selector: 'app-sync',
    imports: [],
    templateUrl: './sync.html',
    styleUrl: './sync.scss',
})
export class Sync implements OnInit, OnDestroy {

    private readonly syncService = inject(SyncService);

    readonly status = signal<SyncStatusResponse | null>(null);
    readonly loading = signal(true);
    readonly syncing = signal(false);
    readonly errorMessage = signal('');
    readonly successMessage = signal('');

    private pollingId: ReturnType<typeof setInterval> | null = null;

    ngOnInit(): void {
        this.loadStatus();
    }

    ngOnDestroy(): void {
        this.stopPolling();
    }

    loadStatus(): void {
        this.loading.set(true);
        this.errorMessage.set('');

        this.syncService.getStatus().subscribe({
            next: (status) => {
                this.status.set(status);
                this.loading.set(false);

                if (status.execution.status === 'RUNNING') {
                    this.syncing.set(true);
                    this.startPolling();
                } else {
                    this.syncing.set(false);
                    this.stopPolling();
                }
            },
            error: () => {
                this.errorMessage.set(
                    'No se ha podido cargar el estado de sincronización.'
                );
                this.loading.set(false);
            },
        });
    }

    syncNow(): void {
        if (this.syncing()) {
            return;
        }

        this.syncing.set(true);
        this.errorMessage.set('');
        this.successMessage.set('');

        this.syncService.syncNow().subscribe({
            next: (response) => {
                if (response.started) {
                    this.successMessage.set(
                        'Sincronización iniciada correctamente.'
                    );
                } else if (response.status === 'RUNNING') {
                    this.successMessage.set(
                        'Ya había una sincronización en curso.'
                    );
                }

                this.loadStatus();
            },
            error: () => {
                this.syncing.set(false);
                this.errorMessage.set(
                    'No se ha podido iniciar la sincronización.'
                );
            },
        });
    }

    private startPolling(): void {
        if (this.pollingId !== null) {
            return;
        }

        this.pollingId = setInterval(() => {
            this.syncService.getStatus().subscribe({
                next: (status) => {
                    this.status.set(status);

                    if (status.execution.status !== 'RUNNING') {
                        this.syncing.set(false);
                        this.stopPolling();

                        if (status.execution.status === 'SUCCESS') {
                            this.successMessage.set(
                                'Sincronización completada correctamente.'
                            );
                        }
                    }
                },
                error: () => {
                    this.errorMessage.set(
                        'No se ha podido actualizar el estado de sincronización.'
                    );
                    this.stopPolling();
                },
            });
        }, 3000);
    }

    formatDateTime(value: string | null): string {
        if (!value) {
            return 'Nunca';
        }

        return new Intl.DateTimeFormat('es-ES', {
            day: '2-digit',
            month: '2-digit',
            year: 'numeric',
            hour: '2-digit',
            minute: '2-digit',
        }).format(new Date(value));
    }

    formatInterval(intervalMs: number): string {
        const minutes = Math.round(intervalMs / 60000);

        if (minutes < 60) {
            return `${minutes} min`;
        }

        const hours = Math.floor(minutes / 60);
        const remainingMinutes = minutes % 60;

        if (remainingMinutes === 0) {
            return `${hours} h`;
        }

        return `${hours} h ${remainingMinutes} min`;
    }

    private stopPolling(): void {
        if (this.pollingId !== null) {
            clearInterval(this.pollingId);
            this.pollingId = null;
        }
    }
}