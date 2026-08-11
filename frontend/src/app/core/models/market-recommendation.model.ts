export interface MarketRecommendation {
    playerId: number;
    biwengerPlayerId: string;
    playerName: string;
    teamName: string;
    positions: string[];
    marketType: 'SALE' | 'AUCTION';
    marketValue: number;
    askingPrice: number;
    currentBid: number | null;
    maximumRecommendedBid: number | null;
    priceDifference: number;
    priceDifferencePercentage: number;
    valueFluctuation: number;
    points: number;
    injured: boolean;
    affordable: boolean;
    score: number;
    recommendation: 'BUY' | 'WATCH' | 'AVOID';
}