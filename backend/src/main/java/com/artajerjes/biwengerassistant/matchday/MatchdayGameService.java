package com.artajerjes.biwengerassistant.matchday;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundGame;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundRef;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundTeam;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;

@Service
public class MatchdayGameService {

        private final MatchdayGameRepository matchdayGameRepository;
        private final LeagueRepository leagueRepository;
        private final BiwengerClient biwengerClient;

        public MatchdayGameService(
                        MatchdayGameRepository matchdayGameRepository,
                        LeagueRepository leagueRepository,
                        BiwengerClient biwengerClient) {

                this.matchdayGameRepository = matchdayGameRepository;
                this.leagueRepository = leagueRepository;
                this.biwengerClient = biwengerClient;
        }

        @Transactional
        public int syncCurrentMatchday(Long leagueId) {

                BiwengerRoundsResponse response = biwengerClient.getRounds();

                return syncCurrentMatchday(
                                leagueId,
                                response);
        }

        @Transactional
        public int syncCurrentMatchday(
                        Long leagueId,
                        BiwengerRoundsResponse response) {

                League league = leagueRepository.findById(leagueId)
                                .orElseThrow(
                                                () -> new LeagueNotFoundException(leagueId));

                if (response == null
                                || response.data() == null) {

                        throw new IllegalStateException(
                                        "Invalid Biwenger rounds response");
                }

                Long roundId = response.data().id();

                if (roundId == null) {
                        throw new IllegalStateException(
                                        "Biwenger current round has no id");
                }

                Integer roundPart = response.data().part();

                List<BiwengerRoundGame> games = response.data().games();

                if (games == null
                                || games.isEmpty()) {

                        return 0;
                }

                int synced = 0;

                for (BiwengerRoundGame game : games) {

                        if (game == null
                                        || game.id() == null) {

                                continue;
                        }

                        syncGame(
                                        league,
                                        roundId,
                                        roundPart,
                                        game);

                        synced++;
                }

                return synced;
        }

        private void syncGame(
                        League league,
                        Long roundId,
                        Integer defaultRoundPart,
                        BiwengerRoundGame game) {

                Integer roundPart = resolveRoundPart(
                                game,
                                defaultRoundPart);

                BiwengerRoundTeam home = game.home();
                BiwengerRoundTeam away = game.away();

                MatchdayGame matchdayGame = matchdayGameRepository
                                .findByLeagueIdAndBiwengerGameId(
                                                league.getId(),
                                                game.id())
                                .orElse(null);

                if (matchdayGame == null) {

                        matchdayGame = new MatchdayGame(
                                        league,
                                        roundId,
                                        game.id(),
                                        roundPart,
                                        game.date(),
                                        game.status(),
                                        resolveTeamId(home),
                                        resolveTeamName(home),
                                        resolveTeamId(away),
                                        resolveTeamName(away));

                } else {

                        matchdayGame.update(
                                        roundId,
                                        roundPart,
                                        game.date(),
                                        game.status(),
                                        resolveTeamId(home),
                                        resolveTeamName(home),
                                        resolveTeamId(away),
                                        resolveTeamName(away));
                }

                matchdayGameRepository.save(
                                matchdayGame);
        }

        private Integer resolveRoundPart(
                        BiwengerRoundGame game,
                        Integer defaultRoundPart) {

                BiwengerRoundRef round = game.round();

                if (round != null
                                && round.part() != null) {

                        return round.part();
                }

                return defaultRoundPart;
        }

        private Long resolveTeamId(
                        BiwengerRoundTeam team) {

                return team != null
                                ? team.id()
                                : null;
        }

        private String resolveTeamName(
                        BiwengerRoundTeam team) {

                return team != null
                                ? team.name()
                                : null;
        }
}