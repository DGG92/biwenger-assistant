import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import {
  Component,
  computed,
  inject,
  signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';

import {
  EconomicStatus,
  Offer,
  OfferPlayer,
} from '../../core/models/offer.model';
import { OfferService } from '../../core/services/offer';

type OfferTab = 'received' | 'sent';

@Component({
  selector: 'app-offers',
  imports: [
    CurrencyPipe,
    DatePipe,
    DecimalPipe,
  ],
  templateUrl: './offers.html',
  styleUrl: './offers.scss',
})
export class Offers {
  private readonly offerService: OfferService =
    inject(OfferService);

  private readonly leagueId = 1;

  readonly activeTab =
    signal<OfferTab>('received');

  private readonly data = toSignal(
    forkJoin({
      offers:
        this.offerService.getOffers(
          this.leagueId
        ),
      economicStatus:
        this.offerService.getEconomicStatus(
          this.leagueId
        ),
    })
  );

  readonly offers = computed(
    () => this.data()?.offers ?? []
  );

  readonly economicStatus = computed<
    EconomicStatus | null
  >(
    () =>
      this.data()?.economicStatus ??
      null
  );

  readonly receivedOffers = computed(
    () =>
      this.offers().filter(
        (offer: Offer) =>
          offer.toManagerId !== null
      )
  );

  readonly sentOffers = computed(
    () =>
      this.offers().filter(
        (offer: Offer) =>
          offer.fromManagerId !== null
      )
  );

  readonly visibleOffers = computed(
    () =>
      this.activeTab() === 'received'
        ? this.receivedOffers()
        : this.sentOffers()
  );

  setActiveTab(tab: OfferTab): void {
    this.activeTab.set(tab);
  }

  counterpartyName(
    offer: Offer
  ): string {
    if (
      this.activeTab() === 'received'
    ) {
      return (
        offer.fromManagerName ??
        'Mercado'
      );
    }

    return (
      offer.toManagerName ??
      'Mercado'
    );
  }

  mainPlayer(
    offer: Offer
  ): OfferPlayer | null {
    return (
      offer.requestedPlayers[0] ??
      null
    );
  }

  mainPlayerName(
    offer: Offer
  ): string {
    return (
      this.mainPlayer(offer)?.name ??
      'Jugador desconocido'
    );
  }

  marketDifference(
    offer: Offer
  ): number | null {
    const player =
      this.mainPlayer(offer);

    if (!player) {
      return null;
    }

    return (
      offer.amount -
      player.marketValue
    );
  }

  marketDifferencePercentage(
    offer: Offer
  ): number | null {
    const player =
      this.mainPlayer(offer);

    if (
      !player ||
      player.marketValue <= 0
    ) {
      return null;
    }

    return (
      (
        (
          offer.amount -
          player.marketValue
        ) /
        player.marketValue
      ) *
      100
    );
  }

  profitability(
    offer: Offer
  ): number | null {
    const player =
      this.mainPlayer(offer);

    if (
      !player ||
      player.purchasePrice === null
    ) {
      return null;
    }

    return (
      offer.amount -
      player.purchasePrice
    );
  }

  profitabilityPercentage(
    offer: Offer
  ): number | null {
    const player =
      this.mainPlayer(offer);

    if (
      !player ||
      player.purchasePrice === null ||
      player.purchasePrice <= 0
    ) {
      return null;
    }

    return (
      (
        (
          offer.amount -
          player.purchasePrice
        ) /
        player.purchasePrice
      ) *
      100
    );
  }

  valueClass(
    value: number | null
  ): string {
    if (value === null || value === 0) {
      return 'neutral';
    }

    return value > 0
      ? 'positive'
      : 'negative';
  }

  statusLabel(
    status: string
  ): string {
    switch (
    status.toLowerCase()
    ) {
      case 'waiting':
        return 'Pendiente';

      case 'accepted':
        return 'Aceptada';

      case 'rejected':
        return 'Rechazada';

      case 'expired':
        return 'Caducada';

      default:
        return status;
    }
  }

  statusClass(
    status: string
  ): string {
    return status.toLowerCase();
  }

  offerDirectionLabel(): string {
    return this.activeTab() ===
      'received'
      ? 'Oferta recibida'
      : 'Oferta enviada';
  }
}