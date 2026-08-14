import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { combineLatest, map, startWith } from 'rxjs';

import { Player, PlayerProtectionReason, PlayerStatus } from '../../core/models/player.model';
import { SquadNeeds } from '../../core/models/squad-needs.model';
import { PlayerService } from '../../core/services/player';
import { RecommendationService } from '../../core/services/recommendation';

type PositionFilter = 'ALL' | 'PT' | 'DF' | 'MC' | 'DL';

type StatusFilter =
  | 'ALL'
  | 'STARTER'
  | 'RESERVE'
  | 'DOUBT'
  | 'INJURED'
  | 'SANCTIONED'
  | 'WARNED'
  | 'DISCARDED'
  | 'ALERT';

interface SquadData {
  manager: SquadNeeds | null;
  players: Player[];
}

const INITIAL_SQUAD_DATA: SquadData = {
  manager: null,
  players: [],
};

@Component({
  selector: 'app-squad',
  imports: [CurrencyPipe],
  templateUrl: './squad.html',
  styleUrl: './squad.scss',
})
export class Squad {
  private readonly playerService = inject(PlayerService);
  private readonly route = inject(ActivatedRoute);
  private readonly recommendationService = inject(RecommendationService);

  statusLabel(
    status: PlayerStatus
  ): string {
    switch (status) {
      case 'DOUBT':
        return 'Duda';

      case 'INJURED':
        return 'Lesionado';

      case 'SANCTIONED':
        return 'Sancionado';

      case 'WARNED':
        return 'Apercibido';

      case 'DISCARDED':
        return 'No convocado';

      case 'UNKNOWN':
        return 'Estado desconocido';

      case 'OK':
      default:
        return 'Disponible';
    }
  }

  lineupWarningText(player: Player): string {
    switch (player.status) {
      case 'DOUBT':
        return `${player.name} está en duda y lo tienes de titular.`;

      case 'INJURED':
        return `${player.name} está lesionado y lo tienes de titular.`;

      case 'SANCTIONED':
        return `${player.name} está sancionado y lo tienes de titular.`;

      case 'DISCARDED':
        return `${player.name} no está convocado y lo tienes de titular.`;

      default:
        return '';
    }
  }

  private readonly squadData = toSignal(
    combineLatest({
      players: this.playerService.getPlayers(),
      squad: this.recommendationService.getSquadNeeds(),
    }).pipe(
      map(({ players, squad }): SquadData => ({
        manager: squad,
        players: players.filter(
          player => player.ownerId === squad.managerId
        ),
      })),
      startWith(INITIAL_SQUAD_DATA)
    ),
    {
      requireSync: true,
    }
  );

  readonly positionFilter =
    signal<PositionFilter>('ALL');

  readonly search =
    signal('');

  readonly statusFilter =
    signal<StatusFilter>('ALL');

  readonly manager = computed(
    () => this.squadData().manager
  );

  readonly players = computed(
    () => this.squadData().players
  );

  readonly filteredPlayers = computed(() => {
    const search =
      this.search().trim().toLowerCase();

    const position =
      this.positionFilter();

    const status =
      this.statusFilter();

    let result = [...this.players()];

    if (search) {
      result = result.filter(player =>
        player.name
          .toLowerCase()
          .includes(search)
      );
    }

    if (position !== 'ALL') {
      result = result.filter(player =>
        player.positions.includes(position)
      );
    }

    if (status === 'STARTER') {
      result = result.filter(
        player => player.starter
      );
    }

    if (status === 'RESERVE') {
      result = result.filter(
        player => player.reserve
      );
    }

    if (status === 'DOUBT') {
      result = result.filter(
        player => player.status === 'DOUBT'
      );
    }

    if (status === 'INJURED') {
      result = result.filter(
        player => player.status === 'INJURED'
      );
    }

    if (status === 'SANCTIONED') {
      result = result.filter(
        player => player.status === 'SANCTIONED'
      );
    }

    if (status === 'WARNED') {
      result = result.filter(
        player => player.status === 'WARNED'
      );
    }

    if (status === 'DISCARDED') {
      result = result.filter(
        player => player.status === 'DISCARDED'
      );
    }

    if (status === 'ALERT') {
      result = result.filter(
        player =>
          player.playerProtectionAlert &&
          player.playerProtectionAlert.level !== 'NONE'
      );
    }

    return result;
  });

  readonly totalValue = computed(() =>
    this.players().reduce(
      (total, player) => total + player.marketValue,
      0
    )
  );

  readonly totalPoints = computed(() =>
    this.players().reduce(
      (total, player) => total + player.points,
      0
    )
  );

  readonly injuredPlayers = computed(() =>
    this.players().filter(
      player => player.status === 'INJURED'
    ).length
  );

  readonly starters = computed(() =>
    this.players().filter(player => player.starter)
  );

  readonly lineupWarnings = computed(() =>
    this.starters().filter(
      player =>
        player.status === 'DOUBT' ||
        player.status === 'INJURED' ||
        player.status === 'SANCTIONED' ||
        player.status === 'DISCARDED'
    )
  );

  readonly reserves = computed(() =>
    this.players().filter(player => player.reserve)
  );

  readonly goalkeeper = computed(() =>
    this.starters().filter(
      player => player.lineupPosition === 'PT'
    )
  );

  readonly defenders = computed(() =>
    this.starters().filter(
      player => player.lineupPosition === 'DF'
    )
  );

  readonly coach = computed(() =>
    this.players().find(player => player.coach) ?? null
  );

  readonly discardedPlayers = computed(() =>
    this.players().filter(
      player =>
        !player.starter &&
        !player.reserve &&
        !player.coach
    )
  );

  readonly goalkeeperReserve = computed(() =>
    this.reserves().find(
      player => player.benchPosition === 'PT'
    ) ?? null
  );

  readonly defenderReserve = computed(() =>
    this.reserves().find(
      player => player.benchPosition === 'DF'
    ) ?? null
  );

  readonly midfielderReserve = computed(() =>
    this.reserves().find(
      player => player.benchPosition === 'MC'
    ) ?? null
  );

  readonly forwardReserve = computed(() =>
    this.reserves().find(
      player => player.benchPosition === 'DL'
    ) ?? null
  );

  readonly midfielders = computed(() =>
    this.starters().filter(
      player => player.lineupPosition === 'MC'
    )
  );

  readonly forwards = computed(() =>
    this.starters().filter(
      player => player.lineupPosition === 'DL'
    )
  );

  protectionAlertTitle(player: Player): string {
    switch (player.playerProtectionAlert.level) {
      case 'PROTECT':
        return 'Protégelo';

      case 'WATCH':
        return 'Vigila su cláusula';

      default:
        return '';
    }
  }

  protectionReasonLabel(
    reason: PlayerProtectionReason
  ): string {
    switch (reason) {
      case 'VALUE_RISING_FAST':
        return 'Valor subiendo muy rápido';

      case 'VALUE_RISING':
        return 'Valor en subida';

      case 'EXCELLENT_RECENT_FORM':
        return 'Excelente estado de forma';

      case 'GOOD_RECENT_FORM':
        return 'Buen estado de forma';

      case 'HIGH_PROFITABILITY':
        return 'Alta rentabilidad';

      case 'INJURED':
        return 'Actualmente lesionado';
    }
  }

  private isStatusFilter(
    value: string
  ): value is StatusFilter {
    return [
      'ALL',
      'STARTER',
      'RESERVE',
      'DOUBT',
      'INJURED',
      'SANCTIONED',
      'WARNED',
      'DISCARDED',
      'ALERT'
    ].includes(value);
  }

  constructor() {
    const params =
      this.route.snapshot.queryParamMap;

    const search =
      params.get('search');

    const status =
      params.get('status');

    if (search) {
      this.search.set(search);
    }

    if (
      status &&
      this.isStatusFilter(status)
    ) {
      this.statusFilter.set(status);
    }
  }

  setSearch(
    value: string
  ): void {
    this.search.set(value);
  }

  setStatusFilter(
    status: StatusFilter
  ): void {
    this.statusFilter.set(status);
  }

  setPositionFilter(
    position: PositionFilter
  ): void {
    this.positionFilter.set(position);
  }
}