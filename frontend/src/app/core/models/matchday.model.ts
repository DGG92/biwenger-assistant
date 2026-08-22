export type MatchdayGameStatus =
    | 'PENDING'
    | 'IN_PLAY'
    | 'FINISHED'
    | 'UNKNOWN';

export type MatchdayLineupPosition =
    | 'PT'
    | 'DF'
    | 'MC'
    | 'DL'
    | 'E';

export interface MatchdayPlayer {
    biwengerPlayerId: number;
    name: string;

    teamName: string | null;
    teamId: number | null;

    gameId: number | null;
    gameRoundPart: number | null;

    lineupIndex: number | null;
    lineupPosition: MatchdayLineupPosition | null;

    starter: boolean;
    reserve: boolean;
    discarded: boolean;

    captain: boolean;
    ram: boolean;
    coach: boolean;

    gameStatus: MatchdayGameStatus;

    locked: boolean;
    modifiable: boolean;

    points: number | null;
}

export interface MatchdayResponse {
    roundId: number;
    roundName: string;
    roundShortName: string;

    roundPart: number | null;
    roundStatus: string | null;

    formation: string | null;

    splitRound: string | null;
    lineupRoundChangesIn: string | null;

    players: MatchdayPlayer[];
}