import {
    Component,
    computed,
    inject,
    signal,
} from '@angular/core';
import {
    toObservable,
    toSignal,
} from '@angular/core/rxjs-interop';
import {
    map,
    startWith,
    switchMap,
} from 'rxjs';

import {
    LeagueStatisticSection,
    MarketStatisticSection,
    RoundStatisticSection,
    StatisticCell,
    StatisticFilter,
    StatisticParam,
    StatisticResponse,
    StatisticSection,
    StatisticUser,
} from '../../core/models/statistics.model';
import { StatisticsService } from '../../core/services/statistics';

interface ReportSelection {
    report: string;
    param?: StatisticParam;
}

interface ReportState {
    loading: boolean;
    response: StatisticResponse | null;
}

@Component({
    selector: 'app-statistics',
    imports: [],
    templateUrl: './statistics.html',
    styleUrl: './statistics.scss',
})
export class Statistics {
    private readonly statisticsService =
        inject(StatisticsService);

    readonly section =
        signal<StatisticSection>('rounds');

    readonly roundSection =
        signal<RoundStatisticSection>('rounds');

    readonly leagueSection =
        signal<LeagueStatisticSection>('standings');

    readonly marketSection =
        signal<MarketStatisticSection>('market');

    readonly marketFilter =
        signal<StatisticFilter>('all');

    private readonly reportSelection =
        computed<ReportSelection>(() => {
            switch (this.section()) {
                case 'rounds':
                    return {
                        report: this.roundSection(),
                    };

                case 'league':
                    return {
                        report: this.leagueSection(),
                    };

                case 'market':
                    return this.getMarketSelection();

                default:
                    return {
                        report: 'rounds',
                    };
            }
        });

    private readonly reportSelection$ =
        toObservable(this.reportSelection);

    private readonly reportState = toSignal(
        this.reportSelection$.pipe(
            switchMap(selection =>
                this.statisticsService
                    .getReport(
                        selection.report,
                        selection.param
                    )
                    .pipe(
                        map(
                            response =>
                                ({
                                    loading: false,
                                    response,
                                }) satisfies ReportState
                        ),
                        startWith(
                            {
                                loading: true,
                                response: null,
                            } satisfies ReportState
                        )
                    )
            )
        )
    );

    readonly loading = computed(
        () => this.reportState()?.loading ?? true
    );

    readonly report = computed(
        () =>
            this.reportState()?.response?.data ??
            null
    );

    readonly contextTitle = computed(() => {
        switch (this.section()) {
            case 'rounds':
                return this.roundSection() === 'rounds'
                    ? 'Jornadas · Posición'
                    : 'Jornadas · Puntos';

            case 'league':
                return this.leagueSection() === 'standings'
                    ? 'Liga · Clasificación'
                    : 'Liga · Puntos';

            case 'market':
                return this.marketTitle();

            default:
                return 'Estadísticas';
        }
    });

    readonly contextDescription = computed(() => {
        switch (this.section()) {
            case 'rounds':
                if (this.roundSection() === 'rounds') {
                    return 'Jornadas ganadas y posición media de cada manager durante la temporada.';
                }

                return 'Puntuación acumulada, mejor jornada y peor jornada de cada manager.';

            case 'league':
                if (
                    this.leagueSection() ===
                    'standings'
                ) {
                    return 'Posición media y tiempo acumulado en el liderato de la liga.';
                }

                return 'Media de puntos conseguida por jornada por cada manager.';

            case 'market':
                return this.marketDescription();

            default:
                return '';
        }
    });

    setSection(
        section: StatisticSection
    ): void {
        this.section.set(section);
    }

    setRoundSection(
        section: RoundStatisticSection
    ): void {
        this.roundSection.set(section);
    }

    setLeagueSection(
        section: LeagueStatisticSection
    ): void {
        this.leagueSection.set(section);
    }

    setMarketSection(
        section: MarketStatisticSection
    ): void {
        this.marketSection.set(section);

        if (section === 'market') {
            this.marketFilter.set('all');
        }
    }

    setMarketFilter(
        filter: StatisticFilter
    ): void {
        this.marketFilter.set(filter);
    }

    private getMarketSelection(): ReportSelection {
        const report = this.marketSection();

        if (report === 'market') {
            return {
                report: 'market',
            };
        }

        const filter = this.marketFilter();

        if (filter === 'all') {
            return {
                report,
            };
        }

        return {
            report,
            param: filter,
        };
    }

    private marketTitle(): string {
        switch (this.marketSection()) {
            case 'purchases':
                return `Mercado · Compras · ${this.filterLabel()}`;

            case 'sales':
                return `Mercado · Ventas · ${this.filterLabel()}`;

            case 'market':
            default:
                return 'Mercado · Operaciones';
        }
    }

    private marketDescription(): string {
        switch (this.marketSection()) {
            case 'purchases':
                return this.purchaseDescription();

            case 'sales':
                return this.saleDescription();

            case 'market':
            default:
                return 'Número total de operaciones, compras y ventas realizadas por cada manager.';
        }
    }

    private purchaseDescription(): string {
        switch (this.marketFilter()) {
            case 'clause':
                return 'Cláusulas ejecutadas por cada manager durante la temporada.';

            case 'bid':
                return 'Jugadores adquiridos mediante subasta por cada manager.';

            case 'loan':
                return 'Operaciones de compra mediante cesión realizadas por cada manager.';

            case 'envelope':
                return 'Jugadores adquiridos mediante sobres por cada manager.';

            case 'autoSale':
                return 'Operaciones correspondientes a venta automática registradas como compras.';

            case 'all':
            default:
                return 'Desglose de todas las compras realizadas por cada manager según su origen.';
        }
    }

    private saleDescription(): string {
        switch (this.marketFilter()) {
            case 'clause':
                return 'Cláusulas recibidas por cada manager durante la temporada.';

            case 'bid':
                return 'Ventas asociadas a subastas registradas por cada manager.';

            case 'loan':
                return 'Operaciones de venta mediante cesión realizadas por cada manager.';

            case 'envelope':
                return 'Ventas asociadas a sobres registradas por cada manager.';

            case 'autoSale':
                return 'Operaciones de venta automática realizadas por cada manager.';

            case 'all':
            default:
                return 'Desglose de todas las ventas realizadas por cada manager según su tipo.';
        }
    }

    private filterLabel(): string {
        switch (this.marketFilter()) {
            case 'clause':
                return 'Cláusulas';

            case 'bid':
                return 'Subastas';

            case 'loan':
                return 'Cesiones';

            case 'envelope':
                return 'Sobres';

            case 'autoSale':
                return 'Venta automática';

            case 'all':
            default:
                return 'Todas';
        }
    }

    isUser(
        cell: StatisticCell
    ): cell is StatisticUser {
        return (
            typeof cell === 'object' &&
            cell !== null &&
            'id' in cell &&
            'name' in cell
        );
    }

    userIcon(
        user: StatisticUser
    ): string | null {
        if (!user.icon) {
            return null;
        }

        if (
            user.icon.startsWith('http://') ||
            user.icon.startsWith('https://')
        ) {
            return user.icon;
        }

        return `https://cdn.biwenger.com/${user.icon}`;
    }

    userInitials(
        name: string
    ): string {
        return name
            .trim()
            .split(/\s+/)
            .slice(0, 2)
            .map(part => part.charAt(0))
            .join('')
            .toUpperCase();
    }

    gridColumns(
        columnCount: number
    ): string {
        if (columnCount <= 1) {
            return 'minmax(180px, 1fr)';
        }

        if (columnCount >= 6) {
            return `
            minmax(170px, 1.45fr)
            repeat(
                ${columnCount - 1},
                minmax(70px, 0.8fr)
            )
        `;
        }

        if (columnCount >= 4) {
            return `
            minmax(190px, 1.5fr)
            repeat(
                ${columnCount - 1},
                minmax(85px, 1fr)
            )
        `;
        }

        return `
        minmax(220px, 1.5fr)
        repeat(
            ${columnCount - 1},
            minmax(110px, 1fr)
        )
    `;
    }

    formatValue(
        cell: StatisticCell,
        type: string
    ): string | number {
        if (
            cell === null ||
            typeof cell === 'object'
        ) {
            return '—';
        }

        const numericValue = Number(cell);

        if (type === 'ordinal') {
            return numericValue > 0
                ? `${numericValue}º`
                : '—';
        }

        if (type === 'interval') {
            return numericValue > 0
                ? this.formatInterval(numericValue)
                : '—';
        }

        return cell;
    }

    private formatInterval(
        seconds: number
    ): string {
        const days = Math.floor(
            seconds / 86400
        );

        if (days > 0) {
            return `${days} d`;
        }

        const hours = Math.floor(
            seconds / 3600
        );

        if (hours > 0) {
            return `${hours} h`;
        }

        const minutes = Math.floor(
            seconds / 60
        );

        return `${minutes} min`;
    }
}