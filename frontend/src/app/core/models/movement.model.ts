export type MovementType =
    | 'MARKET_PURCHASE'
    | 'MARKET_SALE'
    | 'AUCTION_PURCHASE'
    | 'IMMEDIATE_SALE'
    | 'TRANSFER'
    | 'LOAN';

export interface MovementBid {
    managerId: number;
    managerName: string;
    amount: number;
}

export interface Movement {
    id: number;
    type: MovementType;

    playerId: number;
    biwengerPlayerId: string;
    playerName: string;

    fromManagerId: number | null;
    fromManagerName: string | null;

    toManagerId: number | null;
    toManagerName: string | null;

    amount: number;
    rounds: number | null;
    occurredAt: string;

    bids: MovementBid[];
}