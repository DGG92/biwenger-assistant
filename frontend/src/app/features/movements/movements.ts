import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  Movement,
  MovementBid,
  MovementType,
} from '../../core/models/movement.model';
import { MovementService } from '../../core/services/movement';

type MovementFilter =
  | 'ALL'
  | 'PURCHASES'
  | 'SALES'
  | 'TRANSFERS'
  | 'AUCTIONS'
  | 'LOANS';

interface ManagerOption {
  id: number;
  name: string;
}

@Component({
  selector: 'app-movements',
  imports: [CommonModule, FormsModule],
  templateUrl: './movements.html',
  styleUrl: './movements.scss',
})
export class Movements {
  private readonly movementService = inject(MovementService);

  private readonly leagueId = 1;

  readonly movements = signal<Movement[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly selectedFilter = signal<MovementFilter>('ALL');
  readonly selectedManagerId = signal<number | null>(null);
  readonly searchTerm = signal('');

  readonly expandedMovements = signal<Set<number>>(new Set());

  readonly filters: {
    value: MovementFilter;
    label: string;
  }[] = [
      { value: 'ALL', label: 'Todos' },
      { value: 'PURCHASES', label: 'Compras' },
      { value: 'SALES', label: 'Ventas' },
      { value: 'TRANSFERS', label: 'Traspasos' },
      { value: 'AUCTIONS', label: 'Subastas' },
      { value: 'LOANS', label: 'Cesiones' },
    ];

  readonly managers = computed<ManagerOption[]>(() => {
    const managers = new Map<number, string>();

    for (const movement of this.movements()) {
      if (
        movement.fromManagerId !== null &&
        movement.fromManagerName !== null
      ) {
        managers.set(
          movement.fromManagerId,
          movement.fromManagerName,
        );
      }

      if (
        movement.toManagerId !== null &&
        movement.toManagerName !== null
      ) {
        managers.set(
          movement.toManagerId,
          movement.toManagerName,
        );
      }

      for (const bid of movement.bids ?? []) {
        managers.set(bid.managerId, bid.managerName);
      }
    }

    return Array.from(managers.entries())
      .map(([id, name]) => ({ id, name }))
      .sort((a, b) =>
        a.name.localeCompare(b.name, 'es', {
          sensitivity: 'base',
        }),
      );
  });

  readonly filteredMovements = computed(() => {
    const filter = this.selectedFilter();
    const managerId = this.selectedManagerId();
    const search = this.searchTerm()
      .trim()
      .toLocaleLowerCase('es');

    return this.movements()
      .filter((movement) =>
        this.matchesTypeFilter(movement, filter),
      )
      .filter((movement) =>
        this.matchesManagerFilter(movement, managerId),
      )
      .filter((movement) =>
        this.matchesSearch(movement, search),
      )
      .sort(
        (a, b) =>
          new Date(b.occurredAt).getTime() -
          new Date(a.occurredAt).getTime(),
      );
  });

  constructor() {
    this.loadMovements();
  }

  loadMovements(): void {
    this.loading.set(true);
    this.error.set(null);

    this.movementService
      .getMovements(this.leagueId)
      .subscribe({
        next: (movements) => {
          this.movements.set(movements);
          this.loading.set(false);
        },
        error: (error) => {
          console.error(
            'Error loading movements',
            error,
          );

          this.error.set(
            'No se han podido cargar los movimientos.',
          );

          this.loading.set(false);
        },
      });
  }

  selectFilter(filter: MovementFilter): void {
    this.selectedFilter.set(filter);
  }

  onManagerChange(value: string): void {
    if (!value) {
      this.selectedManagerId.set(null);
      return;
    }

    this.selectedManagerId.set(Number(value));
  }

  onSearchChange(value: string): void {
    this.searchTerm.set(value);
  }

  clearFilters(): void {
    this.selectedFilter.set('ALL');
    this.selectedManagerId.set(null);
    this.searchTerm.set('');
  }

  hasActiveFilters(): boolean {
    return (
      this.selectedFilter() !== 'ALL' ||
      this.selectedManagerId() !== null ||
      this.searchTerm().trim().length > 0
    );
  }

  toggleBids(movementId: number): void {
    const updated = new Set(this.expandedMovements());

    if (updated.has(movementId)) {
      updated.delete(movementId);
    } else {
      updated.add(movementId);
    }

    this.expandedMovements.set(updated);
  }

  isExpanded(movementId: number): boolean {
    return this.expandedMovements().has(movementId);
  }

  movementTypeLabel(type: MovementType): string {
    switch (type) {
      case 'MARKET_PURCHASE':
        return 'Compra de mercado';

      case 'MARKET_SALE':
        return 'Venta al mercado';

      case 'AUCTION_PURCHASE':
        return 'Compra en subasta';

      case 'IMMEDIATE_SALE':
        return 'Venta inmediata';

      case 'TRANSFER':
        return 'Traspaso';

      case 'LOAN':
        return 'Cesión';

      default:
        return 'Movimiento';
    }
  }

  movementTypeIcon(type: MovementType): string {
    switch (type) {
      case 'MARKET_PURCHASE':
        return '🛒';

      case 'MARKET_SALE':
        return '💵';

      case 'AUCTION_PURCHASE':
        return '🔨';

      case 'IMMEDIATE_SALE':
        return '⚡';

      case 'TRANSFER':
        return '🔄';

      case 'LOAN':
        return '🤝';

      default:
        return '⚽';
    }
  }

  movementTypeClass(type: MovementType): string {
    switch (type) {
      case 'MARKET_PURCHASE':
        return 'movement-type--purchase';

      case 'MARKET_SALE':
        return 'movement-type--market-sale';

      case 'AUCTION_PURCHASE':
        return 'movement-type--auction';

      case 'IMMEDIATE_SALE':
        return 'movement-type--sale';

      case 'TRANSFER':
        return 'movement-type--transfer';

      case 'LOAN':
        return 'movement-type--loan';

      default:
        return '';
    }
  }

  fromLabel(movement: Movement): string {
    return movement.fromManagerName ?? 'Mercado';
  }

  toLabel(movement: Movement): string {
    return movement.toManagerName ?? 'Mercado';
  }

  hasBids(movement: Movement): boolean {
    return (movement.bids?.length ?? 0) > 0;
  }

  bidsCountLabel(movement: Movement): string {
    const count = movement.bids?.length ?? 0;

    if (count === 1) {
      return '1 puja rival';
    }

    return `${count} pujas rivales`;
  }

  sortedBids(movement: Movement): MovementBid[] {
    return [...(movement.bids ?? [])].sort(
      (a, b) => b.amount - a.amount,
    );
  }

  loanRoundsLabel(rounds: number | null): string {
    if (rounds === null) {
      return 'Duración no disponible';
    }

    if (rounds === 1) {
      return '1 jornada';
    }

    return `${rounds} jornadas`;
  }

  formatMoney(value: number): string {
    return new Intl.NumberFormat('es-ES', {
      style: 'currency',
      currency: 'EUR',
      maximumFractionDigits: 0,
    }).format(value);
  }

  private matchesTypeFilter(
    movement: Movement,
    filter: MovementFilter,
  ): boolean {
    switch (filter) {
      case 'ALL':
        return true;

      case 'PURCHASES':
        return movement.type === 'MARKET_PURCHASE';

      case 'SALES':
        return (
          movement.type === 'MARKET_SALE' ||
          movement.type === 'IMMEDIATE_SALE'
        );

      case 'TRANSFERS':
        return movement.type === 'TRANSFER';

      case 'AUCTIONS':
        return movement.type === 'AUCTION_PURCHASE';

      case 'LOANS':
        return movement.type === 'LOAN';

      default:
        return true;
    }
  }

  private matchesManagerFilter(
    movement: Movement,
    managerId: number | null,
  ): boolean {
    if (managerId === null) {
      return true;
    }

    return (
      movement.fromManagerId === managerId ||
      movement.toManagerId === managerId ||
      (movement.bids ?? []).some(
        (bid) => bid.managerId === managerId,
      )
    );
  }

  private matchesSearch(
    movement: Movement,
    search: string,
  ): boolean {
    if (!search) {
      return true;
    }

    const searchableValues = [
      movement.playerName,
      movement.fromManagerName,
      movement.toManagerName,
      ...(movement.bids ?? []).map(
        (bid) => bid.managerName,
      ),
    ]
      .filter(
        (value): value is string =>
          value !== null && value !== undefined,
      )
      .map((value) =>
        value.toLocaleLowerCase('es'),
      );

    return searchableValues.some((value) =>
      value.includes(search),
    );
  }
}