export interface SquadNeeds {
    managerId: number;
    managerName: string;
    totalPlayers: number;

    playersByPosition: Record<string, number>;
    startersByPosition: Record<string, number>;
    injuredByPosition: Record<string, number>;
    needScoreByPosition: Record<string, number>;
}