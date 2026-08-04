package com.artajerjes.biwengerassistant.league;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.artajerjes.biwengerassistant.league.dto.CreateLeagueRequest;
import com.artajerjes.biwengerassistant.league.dto.LeagueResponse;
import com.artajerjes.biwengerassistant.league.dto.UpdateLeagueRequest;

@Service
public class LeagueService {

    private final LeagueRepository leagueRepository;

    public LeagueService(LeagueRepository leagueRepository) {
        this.leagueRepository = leagueRepository;
    }

    public LeagueResponse create(CreateLeagueRequest request) {
        if (
            request.biwengerLeagueId() != null
            && leagueRepository.existsByBiwengerLeagueId(request.biwengerLeagueId())
        ) {
            throw new LeagueAlreadyExistsException(request.biwengerLeagueId());
        }

        League league = new League(
                request.name(),
                request.biwengerLeagueId()
        );

        League savedLeague = leagueRepository.save(league);

        return toResponse(savedLeague);
    }

    public List<LeagueResponse> findAll() {
        return leagueRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LeagueResponse findById(Long id) {
        return leagueRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new LeagueNotFoundException(id));
    }

    @Transactional
    public LeagueResponse update(Long id, UpdateLeagueRequest request) {
        League league = leagueRepository.findById(id)
            .orElseThrow(() -> new LeagueNotFoundException(id));

        if (
            request.biwengerLeagueId() != null
            && leagueRepository.existsByBiwengerLeagueIdAndIdNot(
                request.biwengerLeagueId(),
                id
            )
        ) {
            throw new LeagueAlreadyExistsException(
                request.biwengerLeagueId()
            );
        }

        league.update(
            request.name(),
            request.biwengerLeagueId()
        );

        return toResponse(league);
    }

    public void delete(Long id) {
        League league = leagueRepository.findById(id)
            .orElseThrow(() -> new LeagueNotFoundException(id));

        leagueRepository.delete(league);
    }

    private LeagueResponse toResponse(League league) {
        return new LeagueResponse(
                league.getId(),
                league.getName(),
                league.getBiwengerLeagueId(),
                league.getCreatedAt()
        );
    }
}