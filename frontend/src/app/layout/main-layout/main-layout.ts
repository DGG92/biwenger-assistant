import {
  Component,
  OnDestroy,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';

import { AuthService } from '../../core/services/auth';
import {
  SyncService,
  SyncStatusResponse,
} from '../../core/services/sync';

@Component({
  selector: 'app-main-layout',
  imports: [
    RouterOutlet,
    RouterLink,
    RouterLinkActive,
  ],
  templateUrl: './main-layout.html',
  styleUrl: './main-layout.scss',
})
export class MainLayout implements OnInit, OnDestroy {

  readonly authService = inject(AuthService);

  readonly mobileMenuOpen = signal(false);
  readonly syncStatus = signal<SyncStatusResponse | null>(null);
  readonly syncStatusUnavailable = signal(false);

  readonly syncState = computed(() => {
    const status = this.syncStatus();

    if (!status) {
      return this.syncStatusUnavailable()
        ? 'unavailable'
        : 'loading';
    }

    if (status.execution.status === 'RUNNING') {
      return 'running';
    }

    if (status.execution.status === 'FAILED') {
      return 'failed';
    }

    if (
      status.execution.status === 'PARTIAL' ||
      status.players.reports.coveragePercent < 100 ||
      status.players.priceHistory.coveragePercent < 100 ||
      status.details.state === 'RATE_LIMITED'
    ) {
      return 'warning';
    }

    return 'success';
  });

  readonly syncLabel = computed(() => {
    const status = this.syncStatus();

    if (!status) {
      return this.syncStatusUnavailable()
        ? 'Estado no disponible'
        : 'Comprobando datos...';
    }

    if (status.execution.status === 'RUNNING') {
      return 'Actualizando datos...';
    }

    if (status.execution.status === 'FAILED') {
      return 'Error en la última actualización';
    }

    if (status.execution.status === 'PARTIAL') {
      return 'Datos actualizados parcialmente';
    }

    if (status.details.state === 'RATE_LIMITED') {
      return 'Actualización temporalmente limitada';
    }

    if (
      status.players.reports.coveragePercent < 100 ||
      status.players.priceHistory.coveragePercent < 100
    ) {
      return 'Datos todavía incompletos';
    }

    return this.formatLastSync(status.execution.finishedAt);
  });

  readonly syncTitle = computed(() => {
    const status = this.syncStatus();

    if (!status) {
      return this.syncStatusUnavailable()
        ? 'No se ha podido consultar el estado de los datos.'
        : 'Consultando el estado de los datos.';
    }

    const parts = [
      `Estado: ${status.execution.status}`,
      `Informes: ${status.players.reports.coveragePercent}%`,
      `Precios: ${status.players.priceHistory.coveragePercent}%`,
    ];

    if (status.execution.finishedAt) {
      parts.push(
        `Última ejecución: ${this.formatDateTime(status.execution.finishedAt)}`
      );
    }

    return parts.join(' · ');
  });

  private readonly router = inject(Router);
  private readonly syncService = inject(SyncService);

  private pollingId: ReturnType<typeof setInterval> | null = null;

  ngOnInit(): void {
    this.loadSyncStatus();

    this.pollingId = setInterval(
      () => this.loadSyncStatus(),
      60_000
    );
  }

  ngOnDestroy(): void {
    if (this.pollingId !== null) {
      clearInterval(this.pollingId);
    }
  }

  toggleMobileMenu(): void {
    this.mobileMenuOpen.update(open => !open);
  }

  closeMobileMenu(): void {
    this.mobileMenuOpen.set(false);
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.mobileMenuOpen.set(false);
        this.router.navigate(['/login']);
      },
    });
  }

  private loadSyncStatus(): void {
    this.syncService.getStatus().subscribe({
      next: status => {
        this.syncStatus.set(status);
        this.syncStatusUnavailable.set(false);
      },
      error: () => {
        this.syncStatusUnavailable.set(true);
      },
    });
  }

  private formatLastSync(value: string | null): string {
    if (!value) {
      return 'Pendiente de primera actualización';
    }

    const date = new Date(value);
    const diffMs = Date.now() - date.getTime();

    if (!Number.isFinite(diffMs) || diffMs < 0) {
      return `Actualizado ${this.formatDateTime(value)}`;
    }

    const minutes = Math.floor(diffMs / 60_000);

    if (minutes < 1) {
      return 'Actualizado ahora';
    }

    if (minutes < 60) {
      return `Actualizado hace ${minutes} min`;
    }

    const hours = Math.floor(minutes / 60);

    if (hours < 24) {
      return `Actualizado hace ${hours} h`;
    }

    return `Actualizado ${this.formatDateTime(value)}`;
  }

  private formatDateTime(value: string): string {
    return new Intl.DateTimeFormat('es-ES', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    }).format(new Date(value));
  }
}