package com.artajerjes.biwengerassistant.league;

import java.util.List;

import org.springframework.stereotype.Service;

import com.artajerjes.biwengerassistant.league.dto.CreateLeagueRequest;
import com.artajerjes.biwengerassistant.league.dto.LeagueResponse;

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

    private LeagueResponse toResponse(League league) {
        return new LeagueResponse(
                league.getId(),
                league.getName(),
                league.getBiwengerLeagueId(),
                league.getCreatedAt()
        );
    }
}