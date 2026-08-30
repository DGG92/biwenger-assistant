export type RecommendedLineupPosition =
    | 'PT'
    | 'DF'
    | 'MC'
    | 'DL';

export type RecommendedLineupChangeType =
    | 'IN'
    | 'OUT';

export interface RecommendedLineupPlayer {
    playerId: number;
    playerName: string;
    position: RecommendedLineupPosition;
    rating: number;
}

export interface RecommendedLineupChange {
    type: RecommendedLineupChangeType;
    playerId: number;
    playerName: string;
}

export interface RecommendedLineup {
    currentFormation: string | null;
    recommendedFormation: string | null;
    currentScore: number;
    recommendedScore: number;
    improvement: number;
    confidence: number;
    recommendedStarters: RecommendedLineupPlayer[];
    changes: RecommendedLineupChange[];
}