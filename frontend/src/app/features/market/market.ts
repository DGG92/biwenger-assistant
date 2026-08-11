import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { RecommendationService } from '../../core/services/recommendation';
import { MarketRecommendation } from '../../core/models/market-recommendation.model';

type RecommendationFilter = 'ALL' | 'STRONG_BUY' | 'BUY' | 'WATCH' | 'AVOID';
type PositionFilter = 'ALL' | 'PT' | 'DF' | 'MC' | 'DL' | 'E';
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
  readonly sortOption =
    signal<SortOption>('SCORE_DESC');

  readonly filteredRecommendations = computed(() => {
    const search = this.search().trim().toLowerCase();
    const recommendationFilter =
      this.recommendationFilter();
    const positionFilter =
      this.positionFilter();
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

  setSortOption(value: SortOption): void {
    this.sortOption.set(value);
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