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

        return result;
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