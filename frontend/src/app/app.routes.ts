import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: 'login',
        loadComponent: () =>
            import('./features/auth/login/login').then(
                (m) => m.Login
            ),
    },
    {
        path: '',
        loadComponent: () =>
            import('./layout/main-layout/main-layout').then(
                (m) => m.MainLayout
            ),
        children: [
            {
                path: '',
                redirectTo: 'dashboard',
                pathMatch: 'full',
            },
            {
                path: 'dashboard',
                loadComponent: () =>
                    import(
                        './features/dashboard/dashboard'
                    ).then(
                        (m) => m.Dashboard
                    ),
            },
            {
                path: 'matchday',
                loadComponent: () =>
                    import(
                        './features/matchday/matchday'
                    ).then(
                        (m) => m.Matchday
                    ),
            },
            {
                path: 'squad',
                loadComponent: () =>
                    import(
                        './features/squad/squad'
                    ).then(
                        (m) => m.Squad
                    ),
            },
            {
                path: 'market',
                loadComponent: () =>
                    import(
                        './features/market/market'
                    ).then(
                        (m) => m.Market
                    ),
            },
            {
                path: 'standings',
                loadComponent: () =>
                    import(
                        './features/standings/standings'
                    ).then(
                        (m) => m.Standings
                    ),
            },
            {
                path: 'statistics',
                loadComponent: () =>
                    import(
                        './features/statistics/statistics'
                    ).then(
                        (m) => m.Statistics
                    ),
            },
            {
                path: 'offers',
                loadComponent: () =>
                    import(
                        './features/offers/offers'
                    ).then(
                        (m) => m.Offers
                    ),
            },
            {
                path: 'movements',
                loadComponent: () =>
                    import(
                        './features/movements/movements'
                    ).then(
                        (m) => m.Movements
                    ),
            },
        ],
    },
    {
        path: '**',
        redirectTo: 'dashboard',
    },
];