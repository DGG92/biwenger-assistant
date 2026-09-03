package com.artajerjes.biwengerassistant.manager;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.manager.dto.SquadProfitabilityResponse;
import com.artajerjes.biwengerassistant.player.Player;
import com.artajerjes.biwengerassistant.player.PlayerPosition;
import com.artajerjes.biwengerassistant.player.PlayerRepository;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@ExtendWith(MockitoExtension.class)
class SquadProfitabilityServiceTest {

        @Mock
        private ManagerRepository managerRepository;

        @Mock
        private PlayerRepository playerRepository;

        @Mock
        private PlayerMatchReportRepository playerMatchReportRepository;

        private SquadProfitabilityService squadProfitabilityService;

        @BeforeEach
        void setUp() {
                squadProfitabilityService = new SquadProfitabilityService(
                                managerRepository,
                                playerRepository,
                                playerMatchReportRepository);
        }

        @Test
        void getSquadProfitabilityShouldCalculateSquadMetrics() {
                Long leagueId = 1L;
                Long managerId = 10L;

                Manager manager = manager(
                                managerId,
                                "Califato Omeya");

                Player profitablePlayer = player(
                                1L,
                                "Jugador rentable",
                                15_000_000L,
                                10_000_000L);

                when(profitablePlayer.getId())
                                .thenReturn(1L);

                when(profitablePlayer.getName())
                                .thenReturn("Jugador rentable");

                Player losingPlayer = player(
                                2L,
                                "Jugador con pérdidas",
                                16_000_000L,
                                20_000_000L);

                when(losingPlayer.getId())
                                .thenReturn(2L);

                when(losingPlayer.getName())
                                .thenReturn("Jugador con pérdidas");

                Player breakEvenPlayer = player(
                                3L,
                                "Jugador estable",
                                5_000_000L,
                                5_000_000L);

                when(breakEvenPlayer.getId())
                                .thenReturn(3L);

                Player unknownPurchasePricePlayer = player(
                                4L,
                                "Jugador sin precio",
                                4_000_000L,
                                null);

                when(unknownPurchasePricePlayer.getId())
                                .thenReturn(4L);

                when(unknownPurchasePricePlayer.getName())
                                .thenReturn("Jugador sin precio");

                PlayerMatchReport profitableReport = org.mockito.Mockito.mock(
                                PlayerMatchReport.class);

                when(profitableReport.getSeason())
                                .thenReturn("2026-2027");

                when(profitableReport.getPoints())
                                .thenReturn(60);

                PlayerMatchReport losingReport = org.mockito.Mockito.mock(
                                PlayerMatchReport.class);

                when(losingReport.getSeason())
                                .thenReturn("2026-2027");

                when(losingReport.getPoints())
                                .thenReturn(40);

                PlayerMatchReport breakEvenReport = org.mockito.Mockito.mock(
                                PlayerMatchReport.class);

                when(breakEvenReport.getSeason())
                                .thenReturn("2026-2027");

                when(breakEvenReport.getPoints())
                                .thenReturn(10);

                PlayerMatchReport unknownPurchasePriceReport = org.mockito.Mockito.mock(
                                PlayerMatchReport.class);

                when(unknownPurchasePriceReport.getSeason())
                                .thenReturn("2026-2027");

                when(unknownPurchasePriceReport.getPoints())
                                .thenReturn(30);

                when(playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                1L))
                                .thenReturn(List.of(profitableReport));

                when(playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                2L))
                                .thenReturn(List.of(losingReport));

                when(playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                3L))
                                .thenReturn(List.of(breakEvenReport));

                when(playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                4L))
                                .thenReturn(List.of(unknownPurchasePriceReport));

                when(managerRepository.findByIdAndLeague_Id(
                                managerId,
                                leagueId))
                                .thenReturn(Optional.of(manager));

                when(playerRepository.findAllByOwner_IdAndLeague_Id(
                                managerId,
                                leagueId))
                                .thenReturn(List.of(
                                                profitablePlayer,
                                                losingPlayer,
                                                breakEvenPlayer,
                                                unknownPurchasePricePlayer));

                SquadProfitabilityResponse result = squadProfitabilityService
                                .getSquadProfitability(
                                                leagueId,
                                                managerId);

                assertEquals(
                                managerId,
                                result.managerId());

                assertEquals(
                                "Califato Omeya",
                                result.managerName());

                assertEquals(
                                4,
                                result.players());

                assertEquals(
                                3,
                                result.playersWithPurchasePrice());

                assertEquals(
                                40_000_000L,
                                result.currentSquadValue());

                assertEquals(
                                36_000_000L,
                                result.analyzedSquadValue());

                assertEquals(
                                35_000_000L,
                                result.totalInvestment());

                assertEquals(
                                1_000_000L,
                                result.unrealizedProfit());

                assertEquals(
                                2.857142857142857,
                                result.unrealizedProfitPercent(),
                                0.000001);

                assertEquals(
                                1,
                                result.profitablePlayers());

                assertEquals(
                                1,
                                result.losingPlayers());

                assertEquals(
                                1,
                                result.breakEvenPlayers());

                assertEquals(
                                "Jugador rentable",
                                result.bestInvestment().name());

                assertEquals(
                                5_000_000L,
                                result.bestInvestment().unrealizedProfit());

                assertEquals(
                                "Jugador con pérdidas",
                                result.worstInvestment().name());

                assertEquals(
                                -4_000_000L,
                                result.worstInvestment().unrealizedProfit());

                assertEquals(
                                "Jugador sin precio",
                                result.mostEfficientPlayer().name());

                assertEquals(
                                7.5,
                                result.mostEfficientPlayer().pointsPerMillion(),
                                0.000001);
        }

        @Test
        void getSquadProfitabilityShouldExcludeNonPlayerPosition() {

                Long leagueId = 1L;
                Long managerId = 10L;

                Manager manager = manager(
                                managerId,
                                "Califato Omeya");

                Player squadPlayer = player(
                                1L,
                                "Jugador de plantilla",
                                10_000_000L,
                                8_000_000L,
                                PlayerPosition.DL);

                when(squadPlayer.getId())
                                .thenReturn(1L);

                when(squadPlayer.getName())
                                .thenReturn("Jugador de plantilla");

                Player excludedPlayer = org.mockito.Mockito.mock(
                                Player.class);

                when(excludedPlayer.getPositions())
                                .thenReturn(List.of(PlayerPosition.E));

                when(managerRepository.findByIdAndLeague_Id(
                                managerId,
                                leagueId))
                                .thenReturn(Optional.of(manager));

                when(playerRepository.findAllByOwner_IdAndLeague_Id(
                                managerId,
                                leagueId))
                                .thenReturn(List.of(
                                                squadPlayer,
                                                excludedPlayer));

                when(playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                1L))
                                .thenReturn(List.of());

                SquadProfitabilityResponse result = squadProfitabilityService.getSquadProfitability(
                                leagueId,
                                managerId);

                assertEquals(
                                1,
                                result.players());

                assertEquals(
                                10_000_000L,
                                result.currentSquadValue());

                assertEquals(
                                8_000_000L,
                                result.totalInvestment());

                assertEquals(
                                2_000_000L,
                                result.unrealizedProfit());

                assertEquals(
                                "Jugador de plantilla",
                                result.bestInvestment().name());

                assertEquals(
                                "Jugador de plantilla",
                                result.worstInvestment().name());
        }

        @Test
        void getSquadProfitabilityShouldHandleEmptySquad() {
                Long leagueId = 1L;
                Long managerId = 10L;

                Manager manager = manager(
                                managerId,
                                "Califato Omeya");

                when(managerRepository.findByIdAndLeague_Id(
                                managerId,
                                leagueId))
                                .thenReturn(Optional.of(manager));

                when(playerRepository.findAllByOwner_IdAndLeague_Id(
                                managerId,
                                leagueId))
                                .thenReturn(List.of());

                SquadProfitabilityResponse result = squadProfitabilityService
                                .getSquadProfitability(
                                                leagueId,
                                                managerId);

                assertEquals(
                                0,
                                result.players());

                assertEquals(
                                0,
                                result.playersWithPurchasePrice());

                assertEquals(
                                0L,
                                result.currentSquadValue());

                assertEquals(
                                0L,
                                result.analyzedSquadValue());

                assertEquals(
                                0L,
                                result.totalInvestment());

                assertEquals(
                                0L,
                                result.unrealizedProfit());

                assertNull(
                                result.unrealizedProfitPercent());

                assertEquals(
                                0,
                                result.profitablePlayers());

                assertEquals(
                                0,
                                result.losingPlayers());

                assertEquals(
                                0,
                                result.breakEvenPlayers());

                assertNull(
                                result.bestInvestment());

                assertNull(
                                result.worstInvestment());

                assertNull(
                                result.mostEfficientPlayer());
        }

        @Test
        void getSquadProfitabilityShouldRejectManagerOutsideLeague() {
                Long leagueId = 1L;
                Long managerId = 10L;

                when(managerRepository.findByIdAndLeague_Id(
                                managerId,
                                leagueId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> squadProfitabilityService
                                                .getSquadProfitability(
                                                                leagueId,
                                                                managerId));
        }

        private Manager manager(
                        Long id,
                        String name) {

                Manager manager = org.mockito.Mockito.mock(
                                Manager.class);

                when(manager.getId())
                                .thenReturn(id);

                when(manager.getName())
                                .thenReturn(name);

                return manager;
        }

        private Player player(
                        Long id,
                        String name,
                        Long marketValue,
                        Long purchasePrice) {

                return player(
                                id,
                                name,
                                marketValue,
                                purchasePrice,
                                PlayerPosition.DL);
        }

        private Player player(
                        Long id,
                        String name,
                        Long marketValue,
                        Long purchasePrice,
                        PlayerPosition position) {

                Player player = org.mockito.Mockito.mock(
                                Player.class);

                when(player.getPositions())
                                .thenReturn(List.of(position));

                when(player.getMarketValue())
                                .thenReturn(marketValue);

                when(player.getPurchasePrice())
                                .thenReturn(purchasePrice);

                if (purchasePrice == null
                                || marketValue == null) {

                        when(player.getProfitability())
                                        .thenReturn(null);

                } else {

                        when(player.getProfitability())
                                        .thenReturn(
                                                        marketValue
                                                                        - purchasePrice);
                }

                return player;
        }
}