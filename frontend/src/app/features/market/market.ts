import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';

import { RecommendationService } from '../../core/services/recommendation';
import { MarketRecommendation, MarketRecommendationReason } from '../../core/models/market-recommendation.model';
import { PlayerStatus } from '../../core/models/player.model';

type RecommendationFilter = 'ALL' | 'STRONG_BUY' | 'BUY' | 'WATCH' | 'AVOID';
type PositionFilter = 'ALL' | 'PT' | 'DF' | 'MC' | 'DL' | 'E';
type OriginFilter = 'ALL' | 'FREE' | 'MANAGER';
type SortOption =
  | 'SCORE_DESC'
  | 'PRICE_ASC'
  | 'PRICE_DESC'
  | 'VALUE_FLUCTUATION_DESC'
  | 'POINTS_DESC';

@Component({
  selector: 'app-market',
  imports: [AsyncPipe, CurrencyPipe],
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
  readonly sortOption =
    signal<SortOption>('SCORE_DESC');

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

  setSortOption(value: SortOption): void {
    this.sortOption.set(value);
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