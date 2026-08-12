import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { combineLatest, map } from 'rxjs';

import { RecommendationService } from '../../core/services/recommendation';
import { PlayerService } from '../../core/services/player';
import { MarketRecommendationReason } from '../../core/models/market-recommendation.model';

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
  }).pipe(
    map((data) => {
      const squadPlayers = data.players.filter(
        player => player.ownerId === data.squad.managerId
      );

      const injuredPlayers = squadPlayers.filter(
        player => player.injured
      );

      const protectionAlerts = squadPlayers.filter(
        player =>
          player.playerProtectionAlert &&
          player.playerProtectionAlert.level !== 'NONE'
      );

      const allStrongBuys = data.market.filter(
        recommendation =>
          recommendation.recommendation === 'STRONG_BUY'
      );

      const strongBuys = allStrongBuys.slice(0, 3);

      const highNeeds = Object.entries(
        data.squad.needScoreByPosition
      ).filter(
        ([, score]) => score >= 50
      );

      return {
        ...data,
        squadPlayers,
        injuredPlayers,
        protectionAlerts,
        strongBuys,
        strongBuyCount: allStrongBuys.length,
        highNeeds,
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

      case 'SQUAD_POSITION_NEEDED':
        return 'Posición necesaria';

      case 'INJURED':
        return 'Jugador lesionado';

      case 'UNAFFORDABLE':
        return 'Fuera de presupuesto';
    }
  }
}