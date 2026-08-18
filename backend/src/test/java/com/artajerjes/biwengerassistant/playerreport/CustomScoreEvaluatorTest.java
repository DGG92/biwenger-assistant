package com.artajerjes.biwengerassistant.playerreport;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CustomScoreEvaluatorTest {

        private CustomScoreEvaluator evaluator;

        @BeforeEach
        void setUp() {
                evaluator = new CustomScoreEvaluator();
        }

        @Test
        void evaluateShouldCalculateRyanRealScore() {

                String expression = """
                                (score2 * 0.5) + (score3 * 0.5)
                                +savedPenalties * 3
                                +penaltyMissed * -1
                                +ownGoals * -1
                                +if((pos1 or pos2), assists * 2)
                                +if((pos3 or pos4), assists * 1)
                                +if(pos1 and minutesPlayed > 70, 1)
                                +if(pos2 and minutesPlayed > 70, 1)
                                +if(pos3 and minutesPlayed > 70, 1)
                                +if(pos4 and minutesPlayed > 70, 1)
                                +if(homeScore == 0 and away and pos2, 1)
                                +if(homeScore == 0 and away and pos1, 2)
                                +if(goals > 0 and minutesPlayed < 31, 2)
                                """;

                Map<String, Object> rawStats = new HashMap<>();

                rawStats.put("pos1", true);
                rawStats.put("price", 3_970_000);
                rawStats.put("score1", 6);
                rawStats.put("score2", 3);
                rawStats.put("score3", 0);
                rawStats.put("away", true);
                rawStats.put("homeScore", 3);
                rawStats.put("awayScore", 0);
                rawStats.put("lost", true);
                rawStats.put("cleanSheet", false);
                rawStats.put("minutesPlayed", 90);
                rawStats.put("picas", 2);
                rawStats.put("sofascore", 6.7);

                assertEquals(
                                3,
                                evaluator.evaluate(
                                                expression,
                                                rawStats));
        }

        @Test
        void evaluateShouldTreatMissingNumericVariablesAsZero() {

                String expression = """
                                score2
                                +goals
                                +assists
                                +savedPenalties
                                """;

                Map<String, Object> rawStats = Map.of(
                                "score2",
                                4);

                assertEquals(
                                4,
                                evaluator.evaluate(
                                                expression,
                                                rawStats));
        }

        @Test
        void evaluateShouldTreatMissingBooleanVariablesAsFalse() {

                String expression = """
                                2
                                +if(pos1, 10)
                                +if(pos2, 20)
                                """;

                assertEquals(
                                2,
                                evaluator.evaluate(
                                                expression,
                                                Map.of()));
        }

        @Test
        void evaluateShouldSupportAndOrAndComparisons() {

                String expression = """
                                if((pos1 or pos2) and minutesPlayed > 70, 5)
                                +if(homeScore == 0 and away, 3)
                                """;

                Map<String, Object> rawStats = Map.of(
                                "pos1",
                                false,
                                "pos2",
                                true,
                                "minutesPlayed",
                                90,
                                "homeScore",
                                0,
                                "away",
                                true);

                assertEquals(
                                8,
                                evaluator.evaluate(
                                                expression,
                                                rawStats));
        }

        @Test
        void evaluateShouldRoundPositiveHalfAwayFromZero() {

                assertEquals(
                                2,
                                evaluator.evaluate(
                                                "1.5",
                                                Map.of()));
        }

        @Test
        void evaluateShouldRoundNegativeHalfAwayFromZero() {

                assertEquals(
                                -2,
                                evaluator.evaluate(
                                                "-1.5",
                                                Map.of()));
        }

        @Test
        void evaluateShouldSupportOptionalFalseBranchInIf() {

                String expression = """
                                if(goals > 0, 10, -3)
                                """;

                assertEquals(
                                -3,
                                evaluator.evaluate(
                                                expression,
                                                Map.of(
                                                                "goals",
                                                                0)));
        }

        @Test
        void evaluateShouldRejectDivisionByZero() {

                assertThrows(
                                IllegalArgumentException.class,
                                () -> evaluator.evaluate(
                                                "10 / 0",
                                                Map.of()));
        }
}