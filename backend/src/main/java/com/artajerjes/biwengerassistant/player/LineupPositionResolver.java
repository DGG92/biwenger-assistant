package com.artajerjes.biwengerassistant.player;

import java.util.ArrayList;
import java.util.List;

public final class LineupPositionResolver {

    private LineupPositionResolver() {
    }

    public static List<PlayerPosition> resolve(String formation) {

        if (formation == null || formation.isBlank()) {
            return List.of();
        }

        String[] lines = formation.split("-");

        if (lines.length < 2) {
            throw new IllegalArgumentException(
                    "Invalid Biwenger formation: " + formation);
        }

        List<Integer> counts = new ArrayList<>();

        for (String line : lines) {
            try {
                counts.add(Integer.valueOf(line));
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(
                        "Invalid Biwenger formation: " + formation,
                        exception);
            }
        }

        List<PlayerPosition> positions = new ArrayList<>();

        positions.add(PlayerPosition.PT);

        for (int i = 0; i < counts.get(0); i++) {
            positions.add(PlayerPosition.DF);
        }

        for (int lineIndex = 1; lineIndex < counts.size() - 1; lineIndex++) {

            for (int i = 0; i < counts.get(lineIndex); i++) {
                positions.add(PlayerPosition.MC);
            }
        }

        int forwards = counts.get(counts.size() - 1);

        for (int i = 0; i < forwards; i++) {
            positions.add(PlayerPosition.DL);
        }

        return positions;
    }
}