import {
    Component,
    computed,
    inject,
    signal,
} from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import {
    MatchdayGameStatus,
    MatchdayPlayer,
    MatchdayResponse,
} from '../../core/models/matchday.model';
import { MatchdayService } from '../../core/services/matchday';

@Component({
    selector: 'app-matchday',
    imports: [],
    templateUrl: './matchday.html',
    styleUrl: './matchday.scss',
})
export class Matchday {
    private readonly matchdayService = inject(MatchdayService);

    readonly availableRounds = toSignal(
        this.matchdayService.getAvailableRounds(),
        {
            initialValue: [],
        }
    );

    readonly matchday = signal<MatchdayResponse | null>(null);

    readonly selectedRoundId = signal<number | null>(null);

    constructor() {
        this.matchdayService
            .getCurrentMatchday()
            .subscribe(matchday => {
                this.matchday.set(matchday);
                this.selectedRoundId.set(matchday.roundId);
            });
    }

    readonly players = computed(
        () => this.matchday()?.players ?? []
    );

    readonly starters = computed(() =>
        this.players()
            .filter(player => player.starter)
            .sort(
                (a, b) =>
                    (a.lineupIndex ?? Number.MAX_SAFE_INTEGER)
                    - (b.lineupIndex ?? Number.MAX_SAFE_INTEGER)
            )
    );

    readonly goalkeeper = computed(() =>
        this.starters().filter(
            player => player.lineupPosition === 'PT'
        )
    );

    readonly defenders = computed(() =>
        this.starters().filter(
            player => player.lineupPosition === 'DF'
        )
    );

    readonly midfielders = computed(() =>
        this.starters().filter(
            player => player.lineupPosition === 'MC'
        )
    );

    readonly forwards = computed(() =>
        this.starters().filter(
            player => player.lineupPosition === 'DL'
        )
    );

    readonly formationSlots = computed(() => {
        const formation = this.matchday()?.formation;

        if (!formation) {
            return null;
        }

        const parts = formation
            .split('-')
            .map(Number);

        if (
            parts.length !== 3
            || parts.some(part => !Number.isInteger(part) || part < 0)
        ) {
            return null;
        }

        const [defenders, midfielders, forwards] = parts;

        return {
            goalkeeper: 1,
            defenders,
            midfielders,
            forwards,
        };
    });

    readonly emptyGoalkeeperSlots = computed(() =>
        Math.max(
            (this.formationSlots()?.goalkeeper ?? 0)
            - this.goalkeeper().length,
            0
        )
    );

    readonly emptyDefenderSlots = computed(() =>
        Math.max(
            (this.formationSlots()?.defenders ?? 0)
            - this.defenders().length,
            0
        )
    );

    readonly emptyMidfielderSlots = computed(() =>
        Math.max(
            (this.formationSlots()?.midfielders ?? 0)
            - this.midfielders().length,
            0
        )
    );

    readonly emptyForwardSlots = computed(() =>
        Math.max(
            (this.formationSlots()?.forwards ?? 0)
            - this.forwards().length,
            0
        )
    );

    readonly emptyLineupSlots = computed(() =>
        this.emptyGoalkeeperSlots()
        + this.emptyDefenderSlots()
        + this.emptyMidfielderSlots()
        + this.emptyForwardSlots()
    );

    readonly incompleteLineupPenalty = computed(
        () => this.emptyLineupSlots() * -4
    );

    readonly reserves = computed(() =>
        this.players().filter(player => player.reserve)
    );

    readonly discarded = computed(() =>
        this.players().filter(player => player.discarded)
    );

    readonly coach = computed(() =>
        this.players().find(player => player.coach) ?? null
    );

    readonly lockedPlayers = computed(() =>
        this.players().filter(player => player.locked).length
    );

    readonly modifiablePlayers = computed(() =>
        this.players().filter(player => player.modifiable).length
    );

    readonly playersWithPoints = computed(() =>
        this.players().filter(
            player => player.points !== null
        ).length
    );

    readonly totalPoints = computed(() =>
        this.players().reduce(
            (total, player) =>
                total + (player.points ?? 0),
            0
        )
    );

    selectRound(roundId: number): void {

        if (roundId === this.selectedRoundId()) {
            return;
        }

        this.matchdayService
            .getMatchday(roundId)
            .subscribe(matchday => {
                this.matchday.set(matchday);
                this.selectedRoundId.set(matchday.roundId);
            });
    }

    pointsClass(points: number | null): string {
        if (points === null) {
            return 'points-pending';
        }

        if (points < 0) {
            return 'points-negative';
        }

        if (points <= 5) {
            return 'points-low';
        }

        if (points <= 9) {
            return 'points-good';
        }

        return 'points-excellent';
    }

    gameStatusLabel(
        status: MatchdayGameStatus
    ): string {
        switch (status) {
            case 'PENDING':
                return 'Pendiente';

            case 'IN_PLAY':
                return 'En juego';

            case 'FINISHED':
                return 'Finalizado';

            case 'UNKNOWN':
            default:
                return 'Desconocido';
        }
    }

    gameStatusClass(
        status: MatchdayGameStatus
    ): string {
        switch (status) {
            case 'PENDING':
                return 'status-pending';

            case 'IN_PLAY':
                return 'status-playing';

            case 'FINISHED':
                return 'status-finished';

            case 'UNKNOWN':
            default:
                return 'status-unknown';
        }
    }

    playerInitial(
        player: MatchdayPlayer
    ): string {
        return player.name.charAt(0).toUpperCase();
    }

    playerImageUrl(biwengerPlayerId: string | number): string {
        return `https://cdn.biwenger.com/i/p/${biwengerPlayerId}.png`;
    }

    hideBrokenPlayerImage(event: Event): void {
        const image = event.target as HTMLImageElement;
        image.style.display = 'none';
    }
}