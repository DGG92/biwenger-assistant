import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { combineLatest, map, startWith } from 'rxjs';

import { Player } from '../../core/models/player.model';
import { SquadNeeds } from '../../core/models/squad-needs.model';
import { PlayerService } from '../../core/services/player';
import { RecommendationService } from '../../core/services/recommendation';

type PositionFilter = 'ALL' | 'PT' | 'DF' | 'MC' | 'DL';

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

  private readonly recommendationService =
    inject(RecommendationService);

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

  readonly manager = computed(
    () => this.squadData().manager
  );

  readonly players = computed(
    () => this.squadData().players
  );

  readonly filteredPlayers = computed(() => {
    const position = this.positionFilter();

    if (position === 'ALL') {
      return this.players();
    }

    return this.players().filter(player =>
      player.positions.includes(position)
    );
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
      player => player.injured
    ).length
  );

  readonly starters = computed(() =>
    this.players().filter(player => player.starter)
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

  setPositionFilter(
    position: PositionFilter
  ): void {
    this.positionFilter.set(position);
  }
}