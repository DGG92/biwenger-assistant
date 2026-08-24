import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AsyncPipe, CurrencyPipe } from '@angular/common';
import { combineLatest, map } from 'rxjs';

import { Player } from '../../core/models/player.model'
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

      const lineupWarnings = squadPlayers.filter(
        player =>
          player.starter &&
          (
            player.status === 'DOUBT' ||
            player.status === 'INJURED' ||
            player.status === 'SANCTIONED' ||
            player.status === 'DISCARDED'
          )
      );

      const injuredPlayers = squadPlayers.filter(
        player => player.status === 'INJURED'
      );

      const nonStarterInjuredPlayers = injuredPlayers.filter(
        player => !player.starter
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
        lineupWarnings,
        nonStarterInjuredPlayers,
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

  lineupWarningTitle(player: Player): string {
    switch (player.status) {
      case 'DOUBT':
        return `${player.name} está en duda`;

      case 'INJURED':
        return `${player.name} está lesionado`;

      case 'SANCTIONED':
        return `${player.name} está sancionado`;

      case 'DISCARDED':
        return `${player.name} no está convocado`;

      default:
        return player.name;
    }
  }

  lineupWarningDescription(player: Player): string {
    switch (player.status) {
      case 'DOUBT':
        return 'Lo tienes de titular · conviene vigilar su estado';

      case 'INJURED':
        return 'Lo tienes de titular · revisa tu alineación';

      case 'SANCTIONED':
        return 'Lo tienes de titular · no podrá disputar la jornada';

      case 'DISCARDED':
        return 'Lo tienes de titular · no está convocado';

      default:
        return '';
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
}