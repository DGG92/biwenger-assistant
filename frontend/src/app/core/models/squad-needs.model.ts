export interface SquadNeeds {
    managerId: number;
    managerName: string;
    currentFormation: string | null;
    totalPlayers: number;

    playersByPosition: Record<string, number>;
    startersByPosition: Record<string, number>;
    injuredByPosition: Record<string, number>;
    needScoreByPosition: Record<string, number>;
}