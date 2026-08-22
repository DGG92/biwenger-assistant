import { Component, computed, inject } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';

import {
    MatchdayGameStatus,
    MatchdayPlayer,
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

    readonly matchday = toSignal(
        this.matchdayService.getCurrentMatchday(),
        {
            initialValue: null,
        }
    );

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
}