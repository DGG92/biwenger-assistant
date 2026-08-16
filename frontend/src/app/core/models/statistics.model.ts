export interface StatisticColumn {
    name: string;
    type: string;
}

export interface StatisticUser {
    id: number;
    name: string;
    icon: string | null;
}

export type StatisticCell =
    | StatisticUser
    | string
    | number
    | null;

export interface StatisticData {
    columns: StatisticColumn[];
    rows: StatisticCell[][];
    settings: unknown;
}

export interface StatisticResponse {
    status: number;
    data: StatisticData;
}

export type StatisticSection =
    | 'rounds'
    | 'league'
    | 'market';

export type RoundStatisticSection =
    | 'rounds'
    | 'roundPoints';

export type LeagueStatisticSection =
    | 'standings'
    | 'points';

export type MarketStatisticSection =
    | 'market'
    | 'purchases'
    | 'sales';

export type StatisticParam =
    | 'clause'
    | 'bid'
    | 'loan'
    | 'envelope'
    | 'autoSale';

export type StatisticFilter =
    | 'all'
    | StatisticParam;