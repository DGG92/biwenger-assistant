import { Component, inject } from '@angular/core';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { combineLatest, map } from 'rxjs';

import { RecommendationService } from '../../core/services/recommendation';

@Component({
  selector: 'app-dashboard',
  imports: [
    AsyncPipe,
    CurrencyPipe,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly recommendationService =
    inject(RecommendationService);

  readonly dashboard$ = combineLatest({
    squad: this.recommendationService.getSquadNeeds(),
    economy: this.recommendationService.getEconomicStatus(),
    market: this.recommendationService.getMarketRecommendations(),
  }).pipe(
    map((data) => ({
      ...data,
      topRecommendations: data.market.slice(0, 5),
    }))
  );
}