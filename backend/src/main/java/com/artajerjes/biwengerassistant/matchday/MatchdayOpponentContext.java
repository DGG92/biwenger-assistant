package com.artajerjes.biwengerassistant.matchday;

public record MatchdayOpponentContext(
                Long roundId,
                Long gameId,
                Long teamId,
                Long opponentTeamId,
                String opponentTeamName,
                MatchdayVenue venue,
                String gameStatus,
                Integer opponentPosition,
                Integer opponentPoints,
                Integer opponentWon,
                Integer opponentLost,
                Integer opponentTied,
                Integer opponentScored,
                Integer opponentAgainst) {
}