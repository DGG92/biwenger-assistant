export type ActionType =
    | 'BUY'
    | 'BID'
    | 'SELL'
    | 'HOLD'
    | 'WATCH'
    | 'REPLACE_STARTER'
    | 'CHANGE_FORMATION'
    | 'PROTECT';

export type ActionPriority =
    | 'HIGH'
    | 'MEDIUM'
    | 'LOW';

export interface ActionRecommendation {
    type: ActionType;
    priority: ActionPriority;
    playerId: number | null;
    playerName: string | null;
    title: string;
    explanation: string;
    confidence: number | null;
    suggestedAmount: number | null;
    sourceSignals: string[];
}