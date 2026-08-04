package com.artajerjes.biwengerassistant.manager;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.biwenger.BiwengerClient;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerLeagueApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerStanding;
import com.artajerjes.biwengerassistant.league.League;
import com.artajerjes.biwengerassistant.league.LeagueNotFoundException;
import com.artajerjes.biwengerassistant.league.LeagueRepository;
import com.artajerjes.biwengerassistant.manager.dto.ManagerSyncResponse;

@Service
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final LeagueRepository leagueRepository;
    private final BiwengerClient biwengerClient;

    public ManagerService(
            ManagerRepository managerRepository,
            LeagueRepository leagueRepository,
            BiwengerClient biwengerClient
    ) {
        this.managerRepository = managerRepository;
        this.leagueRepository = leagueRepository;
        this.biwengerClient = biwengerClient;
    }

    @Transactional
    public ManagerSyncResponse sync(Long leagueId) {
        League league = leagueRepository.findById(leagueId)
                .orElseThrow(
                        () -> new LeagueNotFoundException(leagueId)
                );

        BiwengerLeagueApiResponse response = biwengerClient.getLeague();

        if (response == null || response.data() == null) {
            throw new IllegalStateException(
                    "Biwenger returned an empty league response"
            );
        }

        if (
                response.data().id() == null
                || !response.data().id().toString()
                        .equals(league.getBiwengerLeagueId())
        ) {
            throw new IllegalStateException(
                    "The configured Biwenger league does not match league '"
                    + leagueId
                    + "'"
            );
        }

        List<BiwengerStanding> standings =
                response.data().standings() == null
                        ? List.of()
                        : response.data().standings();

        int created = 0;
        int updated = 0;

        for (BiwengerStanding standing : standings) {
            Manager manager = managerRepository
                    .findByBiwengerManagerIdAndLeague_Id(
                            standing.id(),
                            leagueId
                    )
                    .orElse(null);

            if (manager == null) {
                Manager newManager = new Manager(
                        standing.id(),
                        standing.name(),
                        standing.icon(),
                        standing.points(),
                        standing.teamSize(),
                        standing.teamValue(),
                        standing.teamValueInc(),
                        standing.position(),
                        normalizeRole(standing.role()),
                        league
                );

                managerRepository.save(newManager);
                created++;
            } else {
                manager.updateFromBiwenger(
                        standing.name(),
                        standing.icon(),
                        standing.points(),
                        standing.teamSize(),
                        standing.teamValue(),
                        standing.teamValueInc(),
                        standing.position(),
                        normalizeRole(standing.role())
                );

                updated++;
            }
        }

        return new ManagerSyncResponse(
                standings.size(),
                created,
                updated
        );
    }

    private String normalizeRole(String role) {
        return role == null ? "" : role;
    }
}