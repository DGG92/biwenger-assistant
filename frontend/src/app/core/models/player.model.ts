export interface Player {
    id: number;
    biwengerPlayerId: string;
    name: string;
    positions: string[];
    points: number;
    teamName: string | null;
    marketValue: number;
    injured: boolean;
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
}