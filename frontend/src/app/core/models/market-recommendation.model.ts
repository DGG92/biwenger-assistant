import { PlayerStatus } from "./player.model";

export type MarketRecommendationReason =
    | 'PRICE_BELOW_MARKET'
    | 'PRICE_ABOVE_MARKET'
    | 'VALUE_RISING'
    | 'VALUE_RISING_FAST'
    | 'VALUE_FALLING'
    | 'GOOD_RECENT_FORM'
    | 'EXCELLENT_RECENT_FORM'
    | 'SQUAD_POSITION_NEEDED'
    | 'INJURED'
    | 'UNAFFORDABLE';

export interface MarketRecommendation {
    playerId: number;
    biwengerPlayerId: string;
    playerName: string;
    teamName: string;
    positions: string[];
    marketType: 'SALE' | 'AUCTION';
    sellerId: number | null;
    sellerName: string | null;
    marketValue: number;
    askingPrice: number;
    currentBid: number | null;
    maximumRecommendedBid: number | null;
    priceDifference: number;
    priceDifferencePercentage: number;
    valueFluctuation: number;
    points: number;
    injured: boolean;
    status: PlayerStatus;
    affordable: boolean;
    score: number;
    recommendation: 'STRONG_BUY' | 'BUY' | 'WATCH' | 'AVOID';
    reasons: MarketRecommendationReason[];
}