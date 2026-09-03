import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import {
    ActionPriority,
    ActionRecommendation,
    ActionType
} from '../../core/models/action-recommendation.model';
import { RecommendationService } from '../../core/services/recommendation';

type PriorityFilter =
    | 'ALL'
    | ActionPriority;

type ActionTypeFilter =
    | 'ALL'
    | ActionType;

@Component({
    selector: 'app-recommendations',
    imports: [
        CurrencyPipe
    ],
    templateUrl: './recommendations.html',
    styleUrl: './recommendations.scss',
})
export class Recommendations {
    private readonly recommendationService =
        inject(RecommendationService);

    private readonly actions =
        toSignal(
            this.recommendationService.getActions(),
            { initialValue: [] }
        );

    readonly priorityFilter =
        signal<PriorityFilter>('ALL');

    readonly actionTypeFilter =
        signal<ActionTypeFilter>('ALL');

    readonly filteredActions = computed(() => {
        let result = [...this.actions()];

        if (this.priorityFilter() !== 'ALL') {
            result = result.filter(
                action =>
                    action.priority === this.priorityFilter()
            );
        }

        if (this.actionTypeFilter() !== 'ALL') {
            result = result.filter(
                action =>
                    action.type === this.actionTypeFilter()
            );
        }

        return result.sort(
            (a, b) =>
                this.priorityWeight(a.priority)
                - this.priorityWeight(b.priority)
                || (b.confidence ?? 0)
                - (a.confidence ?? 0)
        );
    });

    readonly highPriorityCount = computed(
        () =>
            this.actions().filter(
                action => action.priority === 'HIGH'
            ).length
    );

    readonly mediumPriorityCount = computed(
        () =>
            this.actions().filter(
                action => action.priority === 'MEDIUM'
            ).length
    );

    readonly lowPriorityCount = computed(
        () =>
            this.actions().filter(
                action => action.priority === 'LOW'
            ).length
    );

    setPriorityFilter(
        priority: PriorityFilter
    ): void {
        this.priorityFilter.set(priority);
    }

    setActionTypeFilter(
        type: ActionTypeFilter
    ): void {
        this.actionTypeFilter.set(type);
    }

    actionLabel(
        type: ActionType
    ): string {
        switch (type) {
            case 'BUY':
                return 'Comprar';

            case 'BID':
                return 'Pujar';

            case 'SELL':
                return 'Vender';

            case 'HOLD':
                return 'Mantener';

            case 'WATCH':
                return 'Vigilar';

            case 'REPLACE_STARTER':
                return 'Cambiar titular';

            case 'CHANGE_FORMATION':
                return 'Cambiar formación';

            case 'PROTECT':
                return 'Proteger';
        }
    }

    priorityLabel(
        priority: ActionPriority
    ): string {
        switch (priority) {
            case 'HIGH':
                return 'Alta';

            case 'MEDIUM':
                return 'Media';

            case 'LOW':
                return 'Baja';
        }
    }

    signalLabel(
        signal: string
    ): string {
        switch (signal) {
            case 'PRICE_BELOW_MARKET':
                return 'Precio por debajo de mercado';

            case 'PRICE_ABOVE_MARKET':
                return 'Precio por encima de mercado';

            case 'VALUE_RISING':
                return 'Valor en subida';

            case 'VALUE_RISING_FAST':
                return 'Valor subiendo con fuerza';

            case 'VALUE_FALLING':
                return 'Valor en descenso';

            case 'VALUE_FALLING_FAST':
                return 'Valor cayendo con fuerza';

            case 'HIGH_PROFIT':
                return 'Alta rentabilidad';

            case 'SIGNIFICANT_LOSS':
                return 'Pérdida importante';

            case 'EXCELLENT_RECENT_FORM':
            case 'RECENT_FORM_EXCELLENT':
                return 'Forma reciente excelente';

            case 'GOOD_RECENT_FORM':
            case 'RECENT_FORM_GOOD':
                return 'Buena forma reciente';

            case 'POOR_RECENT_FORM':
            case 'RECENT_FORM_POOR':
                return 'Mala forma reciente';

            case 'RECENT_FORM_INSUFFICIENT_DATA':
                return 'Pocos datos recientes';

            case 'GOOD_HISTORICAL_PERFORMANCE':
            case 'HISTORICAL_PERFORMANCE_GOOD':
                return 'Buen rendimiento histórico';

            case 'STRONG_HISTORICAL_PERFORMANCE':
            case 'HISTORICAL_PERFORMANCE_STRONG':
                return 'Rendimiento histórico sólido';

            case 'POOR_HISTORICAL_PERFORMANCE':
            case 'HISTORICAL_PERFORMANCE_POOR':
                return 'Rendimiento histórico bajo';

            case 'POSITION_WELL_COVERED':
                return 'Posición bien cubierta';

            case 'STARTER':
                return 'Titular';

            default:
                return signal
                    .toLowerCase()
                    .split('_')
                    .map(
                        word =>
                            word.charAt(0).toUpperCase()
                            + word.slice(1)
                    )
                    .join(' ');
        }
    }

    isPositiveSignal(signal: string): boolean {
        return [
            'PRICE_BELOW_MARKET',
            'VALUE_RISING',
            'VALUE_RISING_FAST',
            'HIGH_PROFIT',
            'EXCELLENT_RECENT_FORM',
            'RECENT_FORM_EXCELLENT',
            'GOOD_RECENT_FORM',
            'RECENT_FORM_GOOD',
            'GOOD_HISTORICAL_PERFORMANCE',
            'HISTORICAL_PERFORMANCE_GOOD',
            'STRONG_HISTORICAL_PERFORMANCE',
            'HISTORICAL_PERFORMANCE_STRONG',
            'STARTER'
        ].includes(signal);
    }

    isNegativeSignal(signal: string): boolean {
        return [
            'PRICE_ABOVE_MARKET',
            'VALUE_FALLING',
            'VALUE_FALLING_FAST',
            'SIGNIFICANT_LOSS',
            'POOR_RECENT_FORM',
            'RECENT_FORM_POOR',
            'POOR_HISTORICAL_PERFORMANCE',
            'HISTORICAL_PERFORMANCE_POOR'
        ].includes(signal);
    }

    isNeutralSignal(signal: string): boolean {
        return !this.isPositiveSignal(signal)
            && !this.isNegativeSignal(signal);
    }

    private priorityWeight(
        priority: ActionPriority
    ): number {
        switch (priority) {
            case 'HIGH':
                return 1;

            case 'MEDIUM':
                return 2;

            case 'LOW':
                return 3;
        }
    }

    trackAction(
        index: number,
        action: ActionRecommendation
    ): string {
        return [
            action.type,
            action.playerId ?? 'general',
            index
        ].join('-');
    }
}