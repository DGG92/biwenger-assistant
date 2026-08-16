import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import { Manager } from '../../core/models/manager.model';
import { ManagerService } from '../../core/services/manager';

@Component({
    selector: 'app-standings',
    imports: [CurrencyPipe],
    templateUrl: './standings.html',
    styleUrl: './standings.scss',
})
export class Standings {
    private readonly managerService = inject(ManagerService);

    private readonly managersData = toSignal(
        this.managerService.getManagers(),
        {
            initialValue: [] as Manager[],
        }
    );

    readonly managers = computed(() =>
        [...this.managersData()].sort(
            (a, b) => a.position - b.position
        )
    );

    gapToPrevious(index: number): number | null {
        if (index === 0) {
            return null;
        }

        const managers = this.managers();

        return Math.max(
            0,
            managers[index - 1].points - managers[index].points
        );
    }

    managerIcon(manager: Manager): string | null {
        if (!manager.icon) {
            return null;
        }

        if (
            manager.icon.startsWith('http://') ||
            manager.icon.startsWith('https://')
        ) {
            return manager.icon;
        }

        return `https://cdn.biwenger.com/${manager.icon}`;
    }

    managerInitials(name: string): string {
        return name
            .trim()
            .split(/\s+/)
            .slice(0, 2)
            .map(part => part.charAt(0))
            .join('')
            .toUpperCase();
    }
}