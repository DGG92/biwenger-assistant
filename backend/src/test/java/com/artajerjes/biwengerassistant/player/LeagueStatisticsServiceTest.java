package com.artajerjes.biwengerassistant.player;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.artajerjes.biwengerassistant.history.PlayerPriceHistory;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.player.dto.LeagueStatisticsResponse;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

@ExtendWith(MockitoExtension.class)
class LeagueStatisticsServiceTest {

        @Mock
        private PlayerRepository playerRepository;

        @Mock
        private PlayerMatchReportRepository playerMatchReportRepository;

        @Mock
        private PlayerPriceHistoryRepository playerPriceHistoryRepository;

        private LeagueStatisticsService leagueStatisticsService;

        @BeforeEach
        void setUp() {
                leagueStatisticsService = new LeagueStatisticsService(
                                playerRepository,
                                playerMatchReportRepository,
                                playerPriceHistoryRepository);
        }

        @Test
        void getLeagueStatisticsShouldCalculateCurrentSeasonRankings() {

                Long leagueId = 1L;

                Player playerOne = player(
                                1L,
                                "Jugador uno",
                                10_000_000L,
                                PlayerPosition.DL);

                Player playerTwo = player(
                                2L,
                                "Jugador dos",
                                5_000_000L,
                                PlayerPosition.MC);

                Player playerThree = mock(Player.class);

                when(playerThree.getId())
                                .thenReturn(3L);

                Player playerWithoutData = mock(Player.class);

                when(playerWithoutData.getId())
                                .thenReturn(4L);

                PlayerMatchReport playerOneReportOne = report(
                                playerOne,
                                "2026-2027",
                                10);

                PlayerMatchReport playerOneReportTwo = report(
                                playerOne,
                                "2026-2027",
                                8);

                PlayerMatchReport playerTwoReportOne = report(
                                playerTwo,
                                "2026-2027",
                                7);

                PlayerMatchReport playerTwoReportTwo = report(
                                playerTwo,
                                "2026-2027",
                                7);

                PlayerMatchReport playerThreeReport = mock(
                                PlayerMatchReport.class);

                when(playerThreeReport.getSeason())
                                .thenReturn("2025-2026");

                when(playerRepository.findAllByLeague_Id(leagueId))
                                .thenReturn(List.of(
                                                playerOne,
                                                playerTwo,
                                                playerThree,
                                                playerWithoutData));

                when(playerMatchReportRepository
                                .findAllScoredReportsByLeague(leagueId))
                                .thenReturn(List.of(
                                                playerOneReportOne,
                                                playerOneReportTwo,
                                                playerTwoReportOne,
                                                playerTwoReportTwo,
                                                playerThreeReport));

                when(playerPriceHistoryRepository
                                .findAllByLeagueIdOrderByPlayerAndPriceDate(leagueId))
                                .thenReturn(List.of());

                LeagueStatisticsResponse result = leagueStatisticsService
                                .getLeagueStatistics(leagueId);

                assertEquals(
                                leagueId,
                                result.leagueId());

                assertEquals(
                                "2026-2027",
                                result.season());

                assertEquals(
                                4,
                                result.players());

                assertEquals(
                                2,
                                result.playersWithData());

                assertEquals(
                                50.0,
                                result.coveragePercent(),
                                0.000001);

                assertEquals(
                                2,
                                result.topPoints().size());

                assertEquals(
                                "Jugador uno",
                                result.topPoints().get(0).name());

                assertEquals(
                                18,
                                result.topPoints().get(0).totalPoints());

                assertEquals(
                                2,
                                result.topPoints().get(0).matchesPlayed());

                assertEquals(
                                9.0,
                                result.topPoints().get(0).averagePoints(),
                                0.000001);

                assertEquals(
                                "Jugador uno",
                                result.topAverage().get(0).name());

                assertEquals(
                                "Jugador dos",
                                result.topEfficiency().get(0).name());

                assertEquals(
                                2.8,
                                result.topEfficiency().get(0).pointsPerMillion(),
                                0.000001);

                assertTrue(
                                result.topPoints().stream()
                                                .noneMatch(statistic -> statistic.playerId().equals(3L)));

                assertTrue(
                                result.topPoints().stream()
                                                .noneMatch(statistic -> statistic.playerId().equals(4L)));
        }

        @Test
        void getLeagueStatisticsShouldReturnEmptyRankingsWhenThereAreNoReports() {

                Long leagueId = 1L;

                Player playerOne = mock(Player.class);
                Player playerTwo = mock(Player.class);

                when(playerOne.getId())
                                .thenReturn(1L);

                when(playerTwo.getId())
                                .thenReturn(2L);

                when(playerRepository.findAllByLeague_Id(leagueId))
                                .thenReturn(List.of(
                                                playerOne,
                                                playerTwo));

                when(playerMatchReportRepository
                                .findAllScoredReportsByLeague(leagueId))
                                .thenReturn(List.of());

                when(playerPriceHistoryRepository
                                .findAllByLeagueIdOrderByPlayerAndPriceDate(leagueId))
                                .thenReturn(List.of());

                LeagueStatisticsResponse result = leagueStatisticsService
                                .getLeagueStatistics(leagueId);

                assertEquals(
                                leagueId,
                                result.leagueId());

                assertEquals(
                                null,
                                result.season());

                assertEquals(
                                2,
                                result.players());

                assertEquals(
                                0,
                                result.playersWithData());

                assertEquals(
                                0.0,
                                result.coveragePercent(),
                                0.000001);

                assertTrue(result.topPoints().isEmpty());
                assertTrue(result.topAverage().isEmpty());
                assertTrue(result.topEfficiency().isEmpty());
        }

        @Test
        void getLeagueStatisticsShouldExcludePlayerWithoutValidMarketValueFromEfficiencyRanking() {

                Long leagueId = 1L;

                Player playerWithValue = player(
                                1L,
                                "Jugador con valor",
                                10_000_000L,
                                PlayerPosition.DL);

                Player playerWithoutValue = player(
                                2L,
                                "Jugador sin valor",
                                null,
                                PlayerPosition.MC);

                PlayerMatchReport reportWithValue = report(
                                playerWithValue,
                                "2026-2027",
                                10);

                PlayerMatchReport reportWithoutValue = report(
                                playerWithoutValue,
                                "2026-2027",
                                50);

                when(playerRepository.findAllByLeague_Id(leagueId))
                                .thenReturn(List.of(
                                                playerWithValue,
                                                playerWithoutValue));

                when(playerMatchReportRepository
                                .findAllScoredReportsByLeague(leagueId))
                                .thenReturn(List.of(
                                                reportWithValue,
                                                reportWithoutValue));

                when(playerPriceHistoryRepository
                                .findAllByLeagueIdOrderByPlayerAndPriceDate(leagueId))
                                .thenReturn(List.of());

                LeagueStatisticsResponse result = leagueStatisticsService
                                .getLeagueStatistics(leagueId);

                assertEquals(
                                2,
                                result.playersWithData());

                assertEquals(
                                2,
                                result.topPoints().size());

                assertEquals(
                                1,
                                result.topEfficiency().size());

                assertEquals(
                                "Jugador con valor",
                                result.topEfficiency().get(0).name());

                assertEquals(
                                1.0,
                                result.topEfficiency().get(0).pointsPerMillion(),
                                0.000001);
        }

        @Test
        void getLeagueStatisticsShouldCalculateEconomicRankings() {

                Long leagueId = 1L;

                Player riser = player(
                                1L,
                                "Jugador subida",
                                12_000_000L,
                                PlayerPosition.DL);

                Player faller = player(
                                2L,
                                "Jugador bajada",
                                8_000_000L,
                                PlayerPosition.MC);

                Player valuable = player(
                                3L,
                                "Jugador valioso",
                                30_000_000L,
                                PlayerPosition.DF);

                when(riser.getPurchasePrice())
                                .thenReturn(8_000_000L);

                when(faller.getPurchasePrice())
                                .thenReturn(12_000_000L);

                when(valuable.getPurchasePrice())
                                .thenReturn(25_000_000L);

                PlayerPriceHistory riserHistory = priceHistory(
                                1L,
                                LocalDate.now().minusDays(7),
                                10_000_000L);

                PlayerPriceHistory fallerHistory = priceHistory(
                                2L,
                                LocalDate.now().minusDays(7),
                                10_000_000L);

                PlayerPriceHistory valuableHistory = priceHistory(
                                3L,
                                LocalDate.now().minusDays(7),
                                30_000_000L);

                when(playerRepository.findAllByLeague_Id(leagueId))
                                .thenReturn(List.of(
                                                riser,
                                                faller,
                                                valuable));

                when(playerMatchReportRepository
                                .findAllScoredReportsByLeague(leagueId))
                                .thenReturn(List.of());

                when(playerPriceHistoryRepository
                                .findAllByLeagueIdOrderByPlayerAndPriceDate(leagueId))
                                .thenReturn(List.of(
                                                riserHistory,
                                                fallerHistory,
                                                valuableHistory));

                LeagueStatisticsResponse result = leagueStatisticsService.getLeagueStatistics(leagueId);

                assertEquals(
                                3,
                                result.playersWithPriceHistory());

                assertEquals(
                                100.0,
                                result.priceHistoryCoveragePercent(),
                                0.000001);

                assertEquals(
                                "Jugador valioso",
                                result.mostValuable().get(0).name());

                assertEquals(
                                30_000_000L,
                                result.mostValuable().get(0).currentValue());

                assertEquals(
                                "Jugador subida",
                                result.biggestRisers().get(0).name());

                assertEquals(
                                20.0,
                                result.biggestRisers().get(0).changePercent7Days(),
                                0.000001);

                assertEquals(
                                "Jugador bajada",
                                result.biggestFallers().get(0).name());

                assertEquals(
                                -20.0,
                                result.biggestFallers().get(0).changePercent7Days(),
                                0.000001);

                assertEquals(
                                "Jugador valioso",
                                result.bestInvestments().get(0).name());

                assertEquals(
                                5_000_000L,
                                result.bestInvestments().get(0).unrealizedProfit());

                assertEquals(
                                "Jugador bajada",
                                result.worstInvestments().get(0).name());

                assertEquals(
                                -4_000_000L,
                                result.worstInvestments().get(0).unrealizedProfit());
        }

        @Test
        void getLeagueStatisticsShouldExcludeZeroPurchasePriceFromInvestmentRankings() {

                Long leagueId = 1L;

                Player validInvestment = player(
                                1L,
                                "Inversión válida",
                                12_000_000L,
                                PlayerPosition.DL);

                Player zeroPurchasePrice = player(
                                2L,
                                "Precio cero",
                                20_000_000L,
                                PlayerPosition.MC);

                when(validInvestment.getPurchasePrice())
                                .thenReturn(10_000_000L);

                when(zeroPurchasePrice.getPurchasePrice())
                                .thenReturn(0L);

                when(playerRepository.findAllByLeague_Id(leagueId))
                                .thenReturn(List.of(
                                                validInvestment,
                                                zeroPurchasePrice));

                when(playerMatchReportRepository
                                .findAllScoredReportsByLeague(leagueId))
                                .thenReturn(List.of());

                when(playerPriceHistoryRepository
                                .findAllByLeagueIdOrderByPlayerAndPriceDate(leagueId))
                                .thenReturn(List.of());

                LeagueStatisticsResponse result = leagueStatisticsService.getLeagueStatistics(leagueId);

                assertEquals(
                                1,
                                result.bestInvestments().size());

                assertEquals(
                                "Inversión válida",
                                result.bestInvestments().get(0).name());

                assertEquals(
                                2_000_000L,
                                result.bestInvestments().get(0).unrealizedProfit());

                assertTrue(
                                result.bestInvestments().stream()
                                                .noneMatch(statistic -> statistic.playerId().equals(2L)));

                assertTrue(
                                result.worstInvestments().stream()
                                                .noneMatch(statistic -> statistic.playerId().equals(2L)));
        }

        private Player player(
                        Long id,
                        String name,
                        Long marketValue,
                        PlayerPosition position) {

                Player player = mock(Player.class);

                when(player.getId())
                                .thenReturn(id);

                when(player.getName())
                                .thenReturn(name);

                when(player.getMarketValue())
                                .thenReturn(marketValue);

                when(player.getPositions())
                                .thenReturn(List.of(position));

                return player;
        }

        private PlayerMatchReport report(
                        Player player,
                        String season,
                        Integer points) {

                PlayerMatchReport report = mock(PlayerMatchReport.class);

                when(report.getPlayer())
                                .thenReturn(player);

                when(report.getSeason())
                                .thenReturn(season);

                when(report.getPoints())
                                .thenReturn(points);

                return report;
        }

        private PlayerPriceHistory priceHistory(
                        Long playerId,
                        LocalDate priceDate,
                        Long marketValue) {

                PlayerPriceHistory history = mock(PlayerPriceHistory.class);

                when(history.getPlayerId())
                                .thenReturn(playerId);

                when(history.getPriceDate())
                                .thenReturn(priceDate);

                when(history.getMarketValue())
                                .thenReturn(marketValue);

                return history;
        }
}