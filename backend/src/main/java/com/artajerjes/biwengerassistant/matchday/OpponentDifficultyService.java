package com.artajerjes.biwengerassistant.matchday;

import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class OpponentDifficultyService {

        private static final double NEUTRAL_DIFFICULTY = 50.0;

        public Optional<OpponentDifficulty> calculate(
                        MatchdayOpponentContext context) {

                if (context == null) {
                        return Optional.empty();
                }

                double positionStrength = calculatePositionStrength(
                                context.opponentPosition());

                int playedMatches = calculatePlayedMatches(context);

                double attackingStrength = calculateAttackingStrength(
                                context.opponentScored(),
                                playedMatches);

                double defensiveStrength = calculateDefensiveStrength(
                                context.opponentAgainst(),
                                playedMatches);

                double overallDifficulty = positionStrength * 0.50
                                + attackingStrength * 0.25
                                + defensiveStrength * 0.25;

                overallDifficulty = applyVenueAdjustment(
                                overallDifficulty,
                                context.venue());

                return Optional.of(
                                new OpponentDifficulty(
                                                round(overallDifficulty),
                                                round(attackingStrength),
                                                round(defensiveStrength),
                                                context.venue()));
        }

        private double calculatePositionStrength(
                        Integer position) {

                if (position == null
                                || position < 1
                                || position > 20) {

                        return NEUTRAL_DIFFICULTY;
                }

                return ((20.0 - position) / 19.0) * 100.0;
        }

        private int calculatePlayedMatches(
                        MatchdayOpponentContext context) {

                if (context.opponentWon() == null
                                || context.opponentLost() == null
                                || context.opponentTied() == null) {

                        return 0;
                }

                int playedMatches = context.opponentWon()
                                + context.opponentLost()
                                + context.opponentTied();

                return Math.max(
                                playedMatches,
                                0);
        }

        private double calculateAttackingStrength(
                        Integer scored,
                        int playedMatches) {

                if (scored == null
                                || scored < 0
                                || playedMatches <= 0) {

                        return NEUTRAL_DIFFICULTY;
                }

                double goalsPerMatch = scored / (double) playedMatches;

                /*
                 * Escala:
                 *
                 * 0 GF/partido -> 0
                 * 1 GF/partido -> 33.33
                 * 2 GF/partido -> 66.67
                 * 3 GF/partido -> 100
                 *
                 * Más de 3 se limita a 100.
                 */
                return clamp(
                                (goalsPerMatch / 3.0) * 100.0,
                                0.0,
                                100.0);
        }

        private double calculateDefensiveStrength(
                        Integer against,
                        int playedMatches) {

                if (against == null
                                || against < 0
                                || playedMatches <= 0) {

                        return NEUTRAL_DIFFICULTY;
                }

                double goalsAgainstPerMatch = against / (double) playedMatches;

                /*
                 * Cuantos menos goles recibe el rival,
                 * mayor es su fortaleza defensiva.
                 *
                 * 0 GC/partido -> 100
                 * 1 GC/partido -> 66.67
                 * 2 GC/partido -> 33.33
                 * 3 GC/partido -> 0
                 *
                 * A partir de 3 GC/partido se limita a 0.
                 */
                return clamp(
                                100.0
                                                - (goalsAgainstPerMatch / 3.0)
                                                                * 100.0,
                                0.0,
                                100.0);
        }

        private double applyVenueAdjustment(
                        double difficulty,
                        MatchdayVenue venue) {

                if (venue == MatchdayVenue.HOME) {
                        return clamp(
                                        difficulty - 5.0,
                                        0.0,
                                        100.0);
                }

                if (venue == MatchdayVenue.AWAY) {
                        return clamp(
                                        difficulty + 5.0,
                                        0.0,
                                        100.0);
                }

                return difficulty;
        }

        private double clamp(
                        double value,
                        double min,
                        double max) {

                return Math.max(
                                min,
                                Math.min(max, value));
        }

        private double round(
                        double value) {

                return Math.round(value * 100.0) / 100.0;
        }
}