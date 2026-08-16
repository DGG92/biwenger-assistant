export interface Manager {
    id: number;
    biwengerManagerId: string;
    name: string;
    icon: string | null;
    points: number;
    teamSize: number;
    teamValue: number;
    teamValueInc: number;
    position: number;
    role: string | null;
    cash: number | null;
    createdAt: string;
}