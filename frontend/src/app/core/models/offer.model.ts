export interface OfferPlayer {
    id: number;
    name: string;
    marketValue: number;
    purchasePrice: number | null;
}

export interface Offer {
    id: number;
    biwengerOfferId: number;
    amount: number;
    status: string;
    type: string;

    fromManagerId: number | null;
    fromManagerName: string | null;

    toManagerId: number | null;
    toManagerName: string | null;

    createdAt: string;
    expiresAt: string;

    requestedPlayers: OfferPlayer[];
}

export interface EconomicStatus {
    balance: number;
    maximumBid: number;
}