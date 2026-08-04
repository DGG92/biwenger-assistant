package com.artajerjes.biwengerassistant.league;

import com.artajerjes.biwengerassistant.league.dto.CreateLeagueRequest;
import com.artajerjes.biwengerassistant.league.dto.LeagueResponse;
import com.artajerjes.biwengerassistant.league.dto.UpdateLeagueRequest;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeagueServiceTest {

    @Mock
    private LeagueRepository leagueRepository;

    @InjectMocks
    private LeagueService leagueService;

    @Test
    void createShouldSaveAndReturnLeague() {
        CreateLeagueRequest request = new CreateLeagueRequest(
                "Liga de prueba",
                "123456"
        );

        when(leagueRepository.existsByBiwengerLeagueId("123456"))
                .thenReturn(false);

        when(leagueRepository.save(any(League.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LeagueResponse response = leagueService.create(request);

        assertEquals("Liga de prueba", response.name());
        assertEquals("123456", response.biwengerLeagueId());

        verify(leagueRepository)
                .existsByBiwengerLeagueId("123456");

        verify(leagueRepository)
                .save(any(League.class));
    }

    @Test
    void createShouldThrowWhenBiwengerLeagueIdAlreadyExists() {
        CreateLeagueRequest request = new CreateLeagueRequest(
                "Liga duplicada",
                "123456"
        );

        when(leagueRepository.existsByBiwengerLeagueId("123456"))
                .thenReturn(true);

        assertThrows(
                LeagueAlreadyExistsException.class,
                () -> leagueService.create(request)
        );

        verify(leagueRepository, never())
                .save(any(League.class));
    }

    @Test
    void findAllShouldReturnAllLeagues() {
        League firstLeague = new League(
                "Liga uno",
                "111111"
        );

        League secondLeague = new League(
                "Liga dos",
                "222222"
        );

        when(leagueRepository.findAll())
                .thenReturn(List.of(firstLeague, secondLeague));

        List<LeagueResponse> result = leagueService.findAll();

        assertEquals(2, result.size());
        assertEquals("Liga uno", result.get(0).name());
        assertEquals("Liga dos", result.get(1).name());
    }

    @Test
    void findByIdShouldReturnLeagueWhenItExists() {
        League league = new League(
                "Liga encontrada",
                "123456"
        );

        when(leagueRepository.findById(1L))
                .thenReturn(Optional.of(league));

        LeagueResponse response = leagueService.findById(1L);

        assertEquals("Liga encontrada", response.name());
        assertEquals("123456", response.biwengerLeagueId());
    }

    @Test
    void findByIdShouldThrowWhenLeagueDoesNotExist() {
        when(leagueRepository.findById(999L))
                .thenReturn(Optional.empty());

        LeagueNotFoundException exception = assertThrows(
                LeagueNotFoundException.class,
                () -> leagueService.findById(999L)
        );

        assertTrue(exception.getMessage().contains("999"));
    }

    @Test
    void updateShouldModifyAndReturnLeague() {
        League league = new League(
                "Nombre anterior",
                "123456"
        );

        UpdateLeagueRequest request = new UpdateLeagueRequest(
                "Nombre nuevo",
                "654321"
        );

        when(leagueRepository.findById(1L))
                .thenReturn(Optional.of(league));

        when(
                leagueRepository.existsByBiwengerLeagueIdAndIdNot(
                        "654321",
                        1L
                )
        ).thenReturn(false);

        LeagueResponse response = leagueService.update(1L, request);

        assertEquals("Nombre nuevo", response.name());
        assertEquals("654321", response.biwengerLeagueId());

        verify(leagueRepository, never())
                .save(any(League.class));
    }

    @Test
    void updateShouldThrowWhenAnotherLeagueHasBiwengerLeagueId() {
        League league = new League(
                "Liga original",
                "123456"
        );

        UpdateLeagueRequest request = new UpdateLeagueRequest(
                "Liga modificada",
                "999999"
        );

        when(leagueRepository.findById(1L))
                .thenReturn(Optional.of(league));

        when(
                leagueRepository.existsByBiwengerLeagueIdAndIdNot(
                        "999999",
                        1L
                )
        ).thenReturn(true);

        assertThrows(
                LeagueAlreadyExistsException.class,
                () -> leagueService.update(1L, request)
        );

        assertEquals("Liga original", league.getName());
        assertEquals("123456", league.getBiwengerLeagueId());
    }

    @Test
    void deleteShouldDeleteExistingLeague() {
        League league = new League(
                "Liga eliminada",
                "123456"
        );

        when(leagueRepository.findById(1L))
                .thenReturn(Optional.of(league));

        leagueService.delete(1L);

        verify(leagueRepository).delete(league);
    }

    @Test
    void deleteShouldThrowWhenLeagueDoesNotExist() {
        when(leagueRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                LeagueNotFoundException.class,
                () -> leagueService.delete(999L)
        );

        verify(leagueRepository, never())
                .delete(any(League.class));
    }
}