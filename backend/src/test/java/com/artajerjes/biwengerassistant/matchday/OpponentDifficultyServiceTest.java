package com.artajerjes.biwengerassistant.matchday;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class OpponentDifficultyServiceTest {

        private final OpponentDifficultyService service = new OpponentDifficultyService();

        @Test
        void calculateShouldReturnHighDifficultyForStrongAwayOpponent() {

                MatchdayOpponentContext context = new MatchdayOpponentContext(
                                4901L,
                                50736L,
                                65L,
                                15L,
                                "Real Madrid",
                                MatchdayVenue.AWAY,
                                "finished",
                                1,
                                9,
                                3,
                                0,
                                0,
                                10,
                                2);

                OpponentDifficulty difficulty = service.calculate(context)
                                .orElseThrow();

                assertEquals(
                                100.0,
                                difficulty.attackingStrength());

                assertEquals(
                                77.78,
                                difficulty.defensiveStrength());

                assertEquals(
                                99.44,
                                difficulty.overallDifficulty());

                assertEquals(
                                MatchdayVenue.AWAY,
                                difficulty.venue());
        }

        @Test
        void calculateShouldReturnLowDifficultyForWeakHomeOpponent() {

                MatchdayOpponentContext context = new MatchdayOpponentContext(
                                4901L,
                                50735L,
                                812L,
                                75L,
                                "Elche",
                                MatchdayVenue.HOME,
                                "finished",
                                19,
                                1,
                                0,
                                2,
                                1,
                                3,
                                9);

                OpponentDifficulty difficulty = service.calculate(context)
                                .orElseThrow();

                assertEquals(
                                33.33,
                                difficulty.attackingStrength());

                assertEquals(
                                0.0,
                                difficulty.defensiveStrength());

                assertEquals(
                                5.96,
                                difficulty.overallDifficulty());

                assertEquals(
                                MatchdayVenue.HOME,
                                difficulty.venue());
        }

        @Test
        void calculateShouldIncreaseDifficultyWhenPlayingAway() {

                MatchdayOpponentContext homeContext = new MatchdayOpponentContext(
                                4901L,
                                5001L,
                                100L,
                                200L,
                                "Rival",
                                MatchdayVenue.HOME,
                                "preview",
                                10,
                                4,
                                1,
                                1,
                                1,
                                5,
                                5);

                MatchdayOpponentContext awayContext = new MatchdayOpponentContext(
                                4901L,
                                5001L,
                                100L,
                                200L,
                                "Rival",
                                MatchdayVenue.AWAY,
                                "preview",
                                10,
                                4,
                                1,
                                1,
                                1,
                                5,
                                5);

                OpponentDifficulty home = service.calculate(homeContext)
                                .orElseThrow();

                OpponentDifficulty away = service.calculate(awayContext)
                                .orElseThrow();

                assertEquals(
                                10.0,
                                away.overallDifficulty()
                                                - home.overallDifficulty());
        }

        @Test
        void calculateShouldUseNeutralValuesWhenStandingDataIsMissing() {

                MatchdayOpponentContext context = new MatchdayOpponentContext(
                                4901L,
                                5001L,
                                100L,
                                200L,
                                "Rival desconocido",
                                MatchdayVenue.HOME,
                                "preview",
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null);

                OpponentDifficulty difficulty = service.calculate(context)
                                .orElseThrow();

                assertEquals(
                                50.0,
                                difficulty.attackingStrength());

                assertEquals(
                                50.0,
                                difficulty.defensiveStrength());

                assertEquals(
                                45.0,
                                difficulty.overallDifficulty());
        }

        @Test
        void calculateShouldReturnEmptyForNullContext() {

                assertTrue(
                                service.calculate(null)
                                                .isEmpty());
        }
}