export interface SquadProfitabilityPlayer {
    playerId: number;
    name: string;
    currentValue: number | null;
    purchasePrice: number | null;
    unrealizedProfit: number | null;
    unrealizedProfitPercent: number | null;
    points: number;
    pointsPerMillion: number | null;
}

export interface SquadProfitability {
    managerId: number;
    managerName: string;

    players: number;
    playersWithPurchasePrice: number;

    currentSquadValue: number;
    analyzedSquadValue: number;
    totalInvestment: number;

    unrealizedProfit: number;
    unrealizedProfitPercent: number | null;

    profitablePlayers: number;
    losingPlayers: number;
    breakEvenPlayers: number;

    bestInvestment: SquadProfitabilityPlayer | null;
    worstInvestment: SquadProfitabilityPlayer | null;
    mostEfficientPlayer: SquadProfitabilityPlayer | null;
}