import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { combineLatest, map } from 'rxjs';

import { RecommendationService } from '../../core/services/recommendation';
import { PlayerService } from '../../core/services/player';
import { MarketRecommendationReason } from '../../core/models/market-recommendation.model';
import { ActionRecommendation, ActionType } from '../../core/models/action-recommendation.model';

@Component({
  selector: 'app-dashboard',
  imports: [
    AsyncPipe,
    CurrencyPipe,
    RouterLink
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard {
  private readonly recommendationService =
    inject(RecommendationService);

  private readonly playerService =
    inject(PlayerService);

  readonly dashboard$ = combineLatest({
    squad: this.recommendationService.getSquadNeeds(),
    economy: this.recommendationService.getEconomicStatus(),
    market: this.recommendationService.getMarketRecommendations(),
    players: this.playerService.getPlayers(),
    actions: this.recommendationService.getActions(),
  }).pipe(
    map((data) => {
      return {
        ...data,
        topActions: data.actions.slice(0, 5),
        highPriorityActionCount: data.actions.filter(
          action => action.priority === 'HIGH'
        ).length,
        topRecommendations: data.market.slice(0, 5),
      };
    })
  );

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

      case 'STRONG_HISTORICAL_PERFORMANCE':
        return 'Rendimiento histórico sólido';

      case 'POOR_HISTORICAL_PERFORMANCE':
        return 'Rendimiento histórico bajo';

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
      STRONG_HISTORICAL_PERFORMANCE: 2,
      VALUE_RISING_FAST: 3,
      SQUAD_POSITION_NEEDED: 4,
      GOOD_RECENT_FORM: 5,
      VALUE_RISING: 6,
      PRICE_BELOW_MARKET: 7,
      PRICE_ABOVE_MARKET: 8,
      VALUE_FALLING: 9,
      POOR_HISTORICAL_PERFORMANCE: 10,
      UNAFFORDABLE: 11,
      INJURED: 12
    };

  topReasons(
    reasons: MarketRecommendationReason[],
    limit = 2
  ): MarketRecommendationReason[] {
    return [...reasons]
      .sort(
        (a, b) =>
          this.reasonPriority[a] -
          this.reasonPriority[b]
      )
      .slice(0, limit);
  }

  actionLabel(type: ActionType): string {
    switch (type) {
      case 'BUY': return 'Comprar';
      case 'BID': return 'Pujar';
      case 'SELL': return 'Vender';
      case 'HOLD': return 'Mantener';
      case 'WATCH': return 'Vigilar';
      case 'REPLACE_STARTER': return 'Cambiar titular';
      case 'CHANGE_FORMATION': return 'Cambiar formación';
      case 'PROTECT': return 'Proteger';
    }
  }

  priorityLabel(priority: ActionRecommendation['priority']): string {
    switch (priority) {
      case 'HIGH': return 'Alta';
      case 'MEDIUM': return 'Media';
      case 'LOW': return 'Baja';
    }
  }

  actionRoute(action: ActionRecommendation): string {
    switch (action.type) {
      case 'BUY':
      case 'BID':
        return '/market';

      default:
        return '/squad';
    }
  }

  actionQueryParams(action: ActionRecommendation): Record<string, string> {
    if (!action.playerName) {
      return {};
    }

    return {
      search: action.playerName
    };
  }
}