import { PlayerStatus } from "./player.model";

export type MarketRecommendationReason =
    | 'PRICE_BELOW_MARKET'
    | 'PRICE_ABOVE_MARKET'
    | 'VALUE_RISING'
    | 'VALUE_RISING_FAST'
    | 'VALUE_FALLING'
    | 'GOOD_RECENT_FORM'
    | 'EXCELLENT_RECENT_FORM'
    | 'STRONG_HISTORICAL_PERFORMANCE'
    | 'POOR_HISTORICAL_PERFORMANCE'
    | 'SQUAD_POSITION_NEEDED'
    | 'INJURED'
    | 'UNAFFORDABLE';

export interface MarketScoreBreakdown {
    base: number;
    price: number;
    valueTrend: number;
    squadNeed: number;

    recentForm: number;
    recentFormSampleSize: number;

    historicalAveragePoints: number;
    historicalSampleSize: number;
    historicalPerformance: number;

    status: number;
    scoreBeforeCaps: number;
    affordabilityCapApplied: boolean;
    auctionBidCapApplied: boolean;
}

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

    value7DaysAgo: number | null;
    change7Days: number | null;
    changePercent7Days: number | null;
    pointsPerMillion: number | null;

    points: number;
    status: PlayerStatus;
    affordable: boolean;
    score: number;
    recommendation: 'STRONG_BUY' | 'BUY' | 'WATCH' | 'AVOID';
    reasons: MarketRecommendationReason[];
    scoreBreakdown: MarketScoreBreakdown;
}