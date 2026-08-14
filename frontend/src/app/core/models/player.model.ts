export type PlayerProtectionAlertLevel =
    'NONE' | 'WATCH' | 'PROTECT';

export type PlayerProtectionReason =
    | 'VALUE_RISING'
    | 'VALUE_RISING_FAST'
    | 'GOOD_RECENT_FORM'
    | 'EXCELLENT_RECENT_FORM'
    | 'HIGH_PROFITABILITY'
    | 'INJURED';

export type PlayerStatus =
    | 'OK'
    | 'DOUBT'
    | 'INJURED'
    | 'SANCTIONED'
    | 'WARNED'
    | 'DISCARDED'
    | 'UNKNOWN';

export interface PlayerProtectionAlert {
    level: PlayerProtectionAlertLevel;
    score: number;
    reasons: PlayerProtectionReason[];
}

export interface Player {
    id: number;
    biwengerPlayerId: string;
    name: string;
    positions: string[];
    points: number;
    teamName: string | null;
    marketValue: number;
    status: PlayerStatus;
    captain: boolean;
    ram: boolean;
    coach: boolean;
    starter: boolean;
    reserve: boolean;
    lineupPosition: string | null;
    benchPosition: string | null;
    purchasePrice: number | null;
    profitability: number | null;
    valueFluctuation: number;
    blockedClause: boolean;
    clauseLockedUntil: string | null;
    clauseValue: number | null;
    ownerId: number | null;
    ownerName: string | null;
    freePlayer: boolean;
    signedAt: string | null;
    leagueId: number;
    createdAt: string;
    playerProtectionAlert: PlayerProtectionAlert;
}