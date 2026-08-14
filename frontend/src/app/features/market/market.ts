import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';

import { RecommendationService } from '../../core/services/recommendation';
import { MarketRecommendation, MarketRecommendationReason } from '../../core/models/market-recommendation.model';
import { PlayerStatus } from '../../core/models/player.model';

type RecommendationFilter = 'ALL' | 'STRONG_BUY' | 'BUY' | 'WATCH' | 'AVOID';
type PositionFilter = 'ALL' | 'PT' | 'DF' | 'MC' | 'DL' | 'E';
type OriginFilter = 'ALL' | 'FREE' | 'MANAGER';
type StatusFilter =
  | 'ALL'
  | 'OK'
  | 'DOUBT'
  | 'INJURED'
  | 'SANCTIONED'
  | 'WARNED'
  | 'DISCARDED';
type SortOption =
  | 'SCORE_DESC'
  | 'PRICE_ASC'
  | 'PRICE_DESC'
  | 'VALUE_FLUCTUATION_DESC'
  | 'POINTS_DESC';

@Component({
  selector: 'app-market',
  imports: [CurrencyPipe, DecimalPipe],
  templateUrl: './market.html',
  styleUrl: './market.scss',
})
export class Market {
  private readonly recommendationService =
    inject(RecommendationService);

  private readonly route =
    inject(ActivatedRoute);

  private readonly recommendations =
    toSignal(
      this.recommendationService.getMarketRecommendations(),
      { initialValue: [] }
    );

  readonly search = signal('');
  readonly recommendationFilter =
    signal<RecommendationFilter>('ALL');
  readonly positionFilter =
    signal<PositionFilter>('ALL');
  readonly originFilter =
    signal<OriginFilter>('ALL');
  readonly statusFilter =
    signal<StatusFilter>('ALL');
  readonly sortOption =
    signal<SortOption>('SCORE_DESC');
  readonly expandedScorePlayerId =
    signal<number | null>(null);

  constructor() {
    const params =
      this.route.snapshot.queryParamMap;

    const search =
      params.get('search');

    const recommendation =
      params.get('recommendation');

    const position =
      params.get('position');

    if (search) {
      this.search.set(search);
    }

    if (
      recommendation &&
      this.isRecommendationFilter(recommendation)
    ) {
      this.recommendationFilter.set(
        recommendation
      );
    }

    if (
      position &&
      this.isPositionFilter(position)
    ) {
      this.positionFilter.set(position);
    }
  }

  readonly filteredRecommendations = computed(() => {
    const search = this.search().trim().toLowerCase();
    const recommendationFilter =
      this.recommendationFilter();
    const positionFilter =
      this.positionFilter();
    const originFilter =
      this.originFilter();
    const statusFilter =
      this.statusFilter();
    const sortOption =
      this.sortOption();

    let result = [...this.recommendations()];

    if (search) {
      result = result.filter((player) =>
        player.playerName.toLowerCase().includes(search)
      );
    }

    if (recommendationFilter !== 'ALL') {
      result = result.filter(
        (player) =>
          player.recommendation === recommendationFilter
      );
    }

    if (positionFilter !== 'ALL') {
      result = result.filter((player) =>
        player.positions.includes(positionFilter)
      );
    }

    if (originFilter === 'FREE') {
      result = result.filter(
        (player) => player.sellerId === null
      );
    }

    if (originFilter === 'MANAGER') {
      result = result.filter(
        (player) => player.sellerId !== null
      );
    }

    if (statusFilter !== 'ALL') {
      result = result.filter(
        player => player.status === statusFilter
      );
    }

    return result.sort(
      (a, b) =>
        this.comparePlayers(a, b, sortOption)
    );
  });

  setSearch(value: string): void {
    this.search.set(value);
  }

  setRecommendationFilter(
    value: RecommendationFilter
  ): void {
    this.recommendationFilter.set(value);
  }

  setPositionFilter(
    value: PositionFilter
  ): void {
    this.positionFilter.set(value);
  }

  setOriginFilter(
    value: OriginFilter
  ): void {
    this.originFilter.set(value);
  }

  setStatusFilter(
    value: StatusFilter
  ): void {
    this.statusFilter.set(value);
  }

  setSortOption(value: SortOption): void {
    this.sortOption.set(value);
  }

  toggleScoreBreakdown(
    playerId: number
  ): void {
    this.expandedScorePlayerId.update(
      current =>
        current === playerId
          ? null
          : playerId
    );
  }

  isScoreBreakdownOpen(
    playerId: number
  ): boolean {
    return this.expandedScorePlayerId() === playerId;
  }

  reasonLabel(
    reason: MarketRecommendationReason
  ): string {
    switch (reason) {
      case 'PRICE_BELOW_MARKET':
        return 'Precio por debajo del mercado';

      case 'PRICE_ABOVE_MARKET':
        return 'Precio por encima del mercado';

      case 'VALUE_RISING':
        return 'Valor en subida';

      case 'VALUE_RISING_FAST':
        return 'Valor subiendo con fuerza';

      case 'VALUE_FALLING':
        return 'Valor en descenso';

      case 'GOOD_RECENT_FORM':
        return 'Buen estado de forma';

      case 'EXCELLENT_RECENT_FORM':
        return 'Excelente estado de forma';

      case 'SQUAD_POSITION_NEEDED':
        return 'Refuerza una posición necesaria';

      case 'INJURED':
        return 'Jugador lesionado';

      case 'UNAFFORDABLE':
        return 'Fuera de presupuesto';
    }
  }

  private readonly reasonPriority: Record<
    MarketRecommendationReason,
    number
  > = {
      EXCELLENT_RECENT_FORM: 1,
      VALUE_RISING_FAST: 2,
      SQUAD_POSITION_NEEDED: 3,
      GOOD_RECENT_FORM: 4,
      VALUE_RISING: 5,
      PRICE_BELOW_MARKET: 6,
      PRICE_ABOVE_MARKET: 7,
      VALUE_FALLING: 8,
      UNAFFORDABLE: 9,
      INJURED: 10
    };

  orderedReasons(
    reasons: MarketRecommendationReason[]
  ): MarketRecommendationReason[] {
    return [...reasons].sort(
      (a, b) =>
        this.reasonPriority[a] -
        this.reasonPriority[b]
    );
  }

  statusLabel(
    status: PlayerStatus
  ): string {
    switch (status) {
      case 'OK':
        return 'Disponible';

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
      default:
        return 'Estado desconocido';
    }
  }

  private isRecommendationFilter(
    value: string
  ): value is RecommendationFilter {
    return [
      'ALL',
      'STRONG_BUY',
      'BUY',
      'WATCH',
      'AVOID'
    ].includes(value);
  }

  private isPositionFilter(
    value: string
  ): value is PositionFilter {
    return [
      'ALL',
      'PT',
      'DF',
      'MC',
      'DL',
      'E'
    ].includes(value);
  }

  private comparePlayers(
    a: MarketRecommendation,
    b: MarketRecommendation,
    sortOption: SortOption
  ): number {
    switch (sortOption) {
      case 'PRICE_ASC':
        return a.askingPrice - b.askingPrice;

      case 'PRICE_DESC':
        return b.askingPrice - a.askingPrice;

      case 'VALUE_FLUCTUATION_DESC':
        return b.valueFluctuation - a.valueFluctuation;

      case 'POINTS_DESC':
        return b.points - a.points;

      case 'SCORE_DESC':
      default:
        return b.score - a.score;
    }
  }
}