export interface LeaguePlayerStatistics {
    playerId: number;
    name: string;
    positions: string[];
    marketValue: number | null;
    totalPoints: number;
    matchesPlayed: number;
    averagePoints: number;
    pointsPerMillion: number | null;
}

export interface LeaguePlayerEconomicStatistics {
    playerId: number;
    name: string;
    positions: string[];

    currentValue: number | null;

    value7DaysAgo: number | null;
    change7Days: number | null;
    changePercent7Days: number | null;

    purchasePrice: number | null;
    unrealizedProfit: number | null;
    unrealizedProfitPercent: number | null;
}

export interface LeagueStatistics {
    leagueId: number;
    season: string | null;

    players: number;
    playersWithData: number;
    coveragePercent: number;

    topPoints: LeaguePlayerStatistics[];
    topAverage: LeaguePlayerStatistics[];
    topEfficiency: LeaguePlayerStatistics[];

    playersWithPriceHistory: number;
    priceHistoryCoveragePercent: number;

    mostValuable: LeaguePlayerEconomicStatistics[];
    biggestRisers: LeaguePlayerEconomicStatistics[];
    biggestFallers: LeaguePlayerEconomicStatistics[];
    bestInvestments: LeaguePlayerEconomicStatistics[];
    worstInvestments: LeaguePlayerEconomicStatistics[];
}