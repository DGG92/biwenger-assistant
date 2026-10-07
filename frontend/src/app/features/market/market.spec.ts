import { ComponentFixture, TestBed } from '@angular/core/testing';
import { registerLocaleData } from '@angular/common';
import localeEs from '@angular/common/locales/es';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';

import { RecommendationService } from '../../core/services/recommendation';
import { RecommendationOverview } from '../../core/models/recommendation-overview.model';
import { MarketRecommendation } from '../../core/models/market-recommendation.model';
import { Market } from './market';

registerLocaleData(localeEs);

describe('Market', () => {
    let fixture: ComponentFixture<Market>;
    let component: Market;

    const recommendationServiceMock = {
        getOverview: vi.fn(),
    };

    const activatedRouteMock = {
        snapshot: {
            queryParamMap: convertToParamMap({}),
        },
    };

    const createRecommendation = (
        overrides: Partial<MarketRecommendation> = {}
    ): MarketRecommendation => ({
        playerId: 1,
        biwengerPlayerId: 'player-1',
        playerName: 'Jugador 2.1',
        teamName: 'Equipo Test',
        positions: ['MC'],
        marketType: 'SALE',
        sellerId: null,
        sellerName: null,
        marketValue: 10_000_000,
        askingPrice: 9_000_000,
        currentBid: null,
        maximumRecommendedBid: null,
        priceDifference: 1_000_000,
        priceDifferencePercentage: 10,
        valueFluctuation: 500_000,

        value7DaysAgo: 9_500_000,
        change7Days: 500_000,
        changePercent7Days: 5.26,
        pointsPerMillion: 4.5,

        points: 45,
        status: 'OK',
        affordable: true,
        score: 82,
        recommendation: 'STRONG_BUY',
        reasons: [
            'ECONOMIC_DYNAMICS_IMPROVING',
            'RECENT_FORM_IMPROVING',
        ],

        scoreBreakdown: {
            base: 50,
            price: 8,

            valueTrend: 12,
            valueTrendBase: 9,
            economicTrendCorrection: 3,
            marketMomentumPercentPerDay: 1.25,
            valueAccelerationPercentPerDaySquared: 0.4,
            economicTrendConsistency: 0.8,

            squadNeed: 2,

            recentForm: 4,
            recentFormSampleSize: 5,

            historicalAveragePoints: 5.4,
            historicalSampleSize: 10,
            historicalPerformance: 3,

            sportsTrendCorrection: 3,
            recentFormDelta: 2.1,
            historicalConsistency: 0.9,

            status: 0,
            scoreBeforeCaps: 82,
            affordabilityCapApplied: false,
            auctionBidCapApplied: false,
        },

        ...overrides,
    });

    const createOverview = (
        market: MarketRecommendation[] = []
    ): RecommendationOverview => ({
        market,
        actions: [],
    });

    beforeEach(async () => {
        recommendationServiceMock.getOverview.mockReset();
        recommendationServiceMock.getOverview.mockReturnValue(
            of(createOverview())
        );

        activatedRouteMock.snapshot.queryParamMap =
            convertToParamMap({});

        await TestBed.configureTestingModule({
            imports: [Market],
            providers: [
                {
                    provide: RecommendationService,
                    useValue: recommendationServiceMock,
                },
                {
                    provide: ActivatedRoute,
                    useValue: activatedRouteMock,
                },
            ],
        }).compileComponents();

        fixture = TestBed.createComponent(Market);
        component = fixture.componentInstance;
    });

    it('should load market recommendations from the overview', () => {
        const player = createRecommendation();

        recommendationServiceMock.getOverview.mockReturnValue(
            of(createOverview([player]))
        );

        fixture = TestBed.createComponent(Market);
        component = fixture.componentInstance;

        expect(
            recommendationServiceMock.getOverview
        ).toHaveBeenCalled();

        expect(component.loading()).toBe(false);
        expect(component.loadError()).toBe(false);
        expect(component.filteredRecommendations()).toEqual([
            player,
        ]);
    });

    it('should expose a load error when the overview cannot be loaded', () => {
        recommendationServiceMock.getOverview.mockReturnValue(
            throwError(() => new Error('Overview unavailable'))
        );

        fixture = TestBed.createComponent(Market);
        component = fixture.componentInstance;

        expect(component.loading()).toBe(false);
        expect(component.loadError()).toBe(true);
        expect(component.filteredRecommendations()).toEqual([]);
    });

    it('should reload the market when requested', () => {
        const secondOverview$ =
            new Subject<RecommendationOverview>();

        recommendationServiceMock.getOverview.mockReset();

        recommendationServiceMock.getOverview
            .mockReturnValueOnce(of(createOverview()))
            .mockReturnValueOnce(secondOverview$.asObservable());

        fixture = TestBed.createComponent(Market);
        component = fixture.componentInstance;

        expect(
            recommendationServiceMock.getOverview
        ).toHaveBeenCalledTimes(1);

        component.reloadMarket();

        expect(
            recommendationServiceMock.getOverview
        ).toHaveBeenCalledTimes(2);

        expect(component.loading()).toBe(true);

        const player = createRecommendation();

        secondOverview$.next(createOverview([player]));
        secondOverview$.complete();

        expect(component.loading()).toBe(false);
        expect(component.loadError()).toBe(false);
        expect(component.filteredRecommendations()).toEqual([
            player,
        ]);
    });

    it('should preserve and expose Engine 2.1 market signals', () => {
        const player = createRecommendation();

        recommendationServiceMock.getOverview.mockReturnValue(
            of(createOverview([player]))
        );

        fixture = TestBed.createComponent(Market);
        component = fixture.componentInstance;

        const result =
            component.filteredRecommendations()[0];

        expect(result.scoreBreakdown.valueTrendBase).toBe(9);
        expect(
            result.scoreBreakdown.economicTrendCorrection
        ).toBe(3);
        expect(
            result.scoreBreakdown.marketMomentumPercentPerDay
        ).toBe(1.25);
        expect(
            result.scoreBreakdown
                .valueAccelerationPercentPerDaySquared
        ).toBe(0.4);
        expect(
            result.scoreBreakdown.economicTrendConsistency
        ).toBe(0.8);

        expect(
            result.scoreBreakdown.sportsTrendCorrection
        ).toBe(3);
        expect(result.scoreBreakdown.recentFormDelta).toBe(2.1);
        expect(
            result.scoreBreakdown.historicalConsistency
        ).toBe(0.9);

        expect(result.reasons).toContain(
            'ECONOMIC_DYNAMICS_IMPROVING'
        );
        expect(result.reasons).toContain(
            'RECENT_FORM_IMPROVING'
        );
    });

    it('should translate and prioritize Engine 2.1 reasons', () => {
        expect(
            component.reasonLabel(
                'ECONOMIC_DYNAMICS_IMPROVING'
            )
        ).toBe('Dinámica económica mejorando');

        expect(
            component.reasonLabel(
                'ECONOMIC_DYNAMICS_WEAKENING'
            )
        ).toBe('Dinámica económica debilitándose');

        expect(
            component.reasonLabel(
                'RECENT_FORM_IMPROVING'
            )
        ).toBe('Forma reciente mejorando');

        expect(
            component.reasonLabel(
                'RECENT_FORM_DECLINING'
            )
        ).toBe('Forma reciente empeorando');

        expect(
            component.orderedReasons([
                'ECONOMIC_DYNAMICS_WEAKENING',
                'PRICE_BELOW_MARKET',
                'ECONOMIC_DYNAMICS_IMPROVING',
                'RECENT_FORM_IMPROVING',
            ])
        ).toEqual([
            'RECENT_FORM_IMPROVING',
            'ECONOMIC_DYNAMICS_IMPROVING',
            'PRICE_BELOW_MARKET',
            'ECONOMIC_DYNAMICS_WEAKENING',
        ]);
    });

    it('should render the Engine 2.1 score breakdown', () => {
        const player = createRecommendation();

        recommendationServiceMock.getOverview.mockReturnValue(
            of(createOverview([player]))
        );

        fixture = TestBed.createComponent(Market);
        component = fixture.componentInstance;

        component.toggleScoreBreakdown(player.playerId);
        fixture.detectChanges();

        const text =
            (fixture.nativeElement as HTMLElement)
                .textContent ?? '';

        expect(text).toContain(
            'Tendencia de valor · base'
        );
        expect(text).toContain(
            'Dinámica económica · ajuste 2.1'
        );
        expect(text).toContain(
            'Tendencia de valor · total'
        );
        expect(text).toContain(
            'Momentum de mercado'
        );
        expect(text).toContain(
            'Aceleración del valor'
        );
        expect(text).toContain(
            'Consistencia económica'
        );
        expect(text).toContain(
            'Dinámica deportiva · ajuste 2.1'
        );
        expect(text).toContain(
            'Variación de forma reciente'
        );
        expect(text).toContain(
            'Consistencia deportiva'
        );
    });

    it('should keep missing Engine 2.1 evidence as unavailable instead of negative', () => {
        const player = createRecommendation({
            reasons: [],
            scoreBreakdown: {
                base: 50,
                price: 0,

                valueTrend: 0,
                valueTrendBase: 0,
                economicTrendCorrection: 0,
                marketMomentumPercentPerDay: null,
                valueAccelerationPercentPerDaySquared: null,
                economicTrendConsistency: null,

                squadNeed: 0,

                recentForm: 0,
                recentFormSampleSize: 0,

                historicalAveragePoints: 0,
                historicalSampleSize: 0,
                historicalPerformance: 0,

                sportsTrendCorrection: 0,
                recentFormDelta: null,
                historicalConsistency: null,

                status: 0,
                scoreBeforeCaps: 50,
                affordabilityCapApplied: false,
                auctionBidCapApplied: false,
            },
        });

        recommendationServiceMock.getOverview.mockReturnValue(
            of(createOverview([player]))
        );

        fixture = TestBed.createComponent(Market);
        component = fixture.componentInstance;

        const result =
            component.filteredRecommendations()[0];

        expect(
            result.scoreBreakdown.economicTrendCorrection
        ).toBe(0);
        expect(
            result.scoreBreakdown.sportsTrendCorrection
        ).toBe(0);

        expect(
            result.scoreBreakdown.marketMomentumPercentPerDay
        ).toBeNull();
        expect(
            result.scoreBreakdown
                .valueAccelerationPercentPerDaySquared
        ).toBeNull();
        expect(
            result.scoreBreakdown.economicTrendConsistency
        ).toBeNull();
        expect(
            result.scoreBreakdown.recentFormDelta
        ).toBeNull();
        expect(
            result.scoreBreakdown.historicalConsistency
        ).toBeNull();

        expect(result.reasons).not.toContain(
            'ECONOMIC_DYNAMICS_IMPROVING'
        );
        expect(result.reasons).not.toContain(
            'ECONOMIC_DYNAMICS_WEAKENING'
        );
        expect(result.reasons).not.toContain(
            'RECENT_FORM_IMPROVING'
        );
        expect(result.reasons).not.toContain(
            'RECENT_FORM_DECLINING'
        );
    });
});