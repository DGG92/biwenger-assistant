package com.artajerjes.biwengerassistant.player;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.artajerjes.biwengerassistant.history.PlayerPriceHistory;
import com.artajerjes.biwengerassistant.history.PlayerPriceHistoryRepository;
import com.artajerjes.biwengerassistant.history.PlayerPriceSource;
import com.artajerjes.biwengerassistant.player.dto.PlayerAnalyticsResponse;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReport;
import com.artajerjes.biwengerassistant.playerreport.PlayerMatchReportRepository;

class PlayerAnalyticsServiceTest {

        private PlayerRepository playerRepository;
        private PlayerPriceHistoryRepository playerPriceHistoryRepository;
        private PlayerMatchReportRepository playerMatchReportRepository;

        private PlayerAnalyticsService service;

        @BeforeEach
        void setUp() {

                playerRepository = mock(PlayerRepository.class);
                playerPriceHistoryRepository = mock(
                                PlayerPriceHistoryRepository.class);
                playerMatchReportRepository = mock(
                                PlayerMatchReportRepository.class);

                service = new PlayerAnalyticsService(
                                playerRepository,
                                playerPriceHistoryRepository,
                                playerMatchReportRepository);
        }

        @Test
        void getPlayerAnalyticsShouldCalculateMarketAndPerformanceMetrics() {

                Long leagueId = 1L;
                Long playerId = 10L;

                Player player = player(
                                playerId,
                                20_000_000L,
                                15_000_000L);

                LocalDate today = LocalDate.now();

                PlayerPriceHistory oneDayAgo = price(
                                playerId,
                                leagueId,
                                today.minusDays(1),
                                19_500_000L);

                PlayerPriceHistory sevenDaysAgo = price(
                                playerId,
                                leagueId,
                                today.minusDays(7),
                                18_000_000L);

                PlayerPriceHistory thirtyDaysAgo = price(
                                playerId,
                                leagueId,
                                today.minusDays(30),
                                16_000_000L);

                List<PlayerPriceHistory> history = List.of(
                                price(
                                                playerId,
                                                leagueId,
                                                today.minusDays(60),
                                                14_000_000L),
                                thirtyDaysAgo,
                                sevenDaysAgo,
                                oneDayAgo,
                                price(
                                                playerId,
                                                leagueId,
                                                today,
                                                20_000_000L),
                                price(
                                                playerId,
                                                leagueId,
                                                today.minusDays(10),
                                                22_000_000L));

                when(playerRepository.findByIdAndLeague_Id(
                                playerId,
                                leagueId))
                                .thenReturn(Optional.of(player));

                when(playerPriceHistoryRepository
                                .findTopByPlayerIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                                                playerId,
                                                today.minusDays(1)))
                                .thenReturn(Optional.of(oneDayAgo));

                when(playerPriceHistoryRepository
                                .findTopByPlayerIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                                                playerId,
                                                today.minusDays(7)))
                                .thenReturn(Optional.of(sevenDaysAgo));

                when(playerPriceHistoryRepository
                                .findTopByPlayerIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                                                playerId,
                                                today.minusDays(30)))
                                .thenReturn(Optional.of(thirtyDaysAgo));

                when(playerPriceHistoryRepository
                                .findAllByPlayerIdOrderByPriceDateAsc(playerId))
                                .thenReturn(history);

                List<PlayerMatchReport> reports = List.of(
                                report(player, 10),
                                report(player, 8),
                                report(player, 6),
                                report(player, 4),
                                report(player, 2),
                                report(player, 0));

                when(playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                playerId))
                                .thenReturn(reports);

                PlayerAnalyticsResponse result = service
                                .getPlayerAnalytics(
                                                leagueId,
                                                playerId);

                assertEquals(
                                playerId,
                                result.playerId());

                assertEquals(
                                20_000_000L,
                                result.currentValue());

                assertEquals(
                                19_500_000L,
                                result.value1DayAgo());

                assertEquals(
                                18_000_000L,
                                result.value7DaysAgo());

                assertEquals(
                                16_000_000L,
                                result.value30DaysAgo());

                assertEquals(
                                500_000L,
                                result.change1Day());

                assertEquals(
                                2_000_000L,
                                result.change7Days());

                assertEquals(
                                4_000_000L,
                                result.change30Days());

                assertEquals(
                                11.11111111111111,
                                result.changePercent7Days(),
                                0.000001);

                assertEquals(
                                25.0,
                                result.changePercent30Days(),
                                0.000001);

                assertEquals(
                                14_000_000L,
                                result.historicalMinValue());

                assertEquals(
                                22_000_000L,
                                result.historicalMaxValue());

                assertEquals(
                                15_000_000L,
                                result.purchasePrice());

                assertEquals(
                                5_000_000L,
                                result.unrealizedProfit());

                assertEquals(
                                33.33333333333333,
                                result.unrealizedProfitPercent(),
                                0.000001);

                assertEquals(
                                "2026",
                                result.season());

                assertEquals(
                                30,
                                result.totalPoints());

                assertEquals(
                                6,
                                result.matchesPlayed());

                assertEquals(
                                5.0,
                                result.averagePoints(),
                                0.000001);

                /*
                 * Los cinco primeros reports son los más recientes:
                 *
                 * 10 + 8 + 6 + 4 + 2 = 30
                 * 30 / 5 = 6
                 */
                assertEquals(
                                6.0,
                                result.recentAveragePoints(),
                                0.000001);
        }

        @Test
        void getPlayerAnalyticsShouldHandleMissingHistoricalAndOwnershipData() {

                Long leagueId = 1L;
                Long playerId = 20L;

                Player player = player(
                                playerId,
                                5_000_000L,
                                null);

                when(playerRepository.findByIdAndLeague_Id(
                                playerId,
                                leagueId))
                                .thenReturn(Optional.of(player));

                when(playerPriceHistoryRepository
                                .findTopByPlayerIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                                                any(),
                                                any()))
                                .thenReturn(Optional.empty());

                when(playerPriceHistoryRepository
                                .findAllByPlayerIdOrderByPriceDateAsc(playerId))
                                .thenReturn(List.of());

                when(playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                playerId))
                                .thenReturn(List.of());

                PlayerAnalyticsResponse result = service
                                .getPlayerAnalytics(
                                                leagueId,
                                                playerId);

                assertEquals(
                                5_000_000L,
                                result.currentValue());

                assertNull(
                                result.value1DayAgo());

                assertNull(
                                result.value7DaysAgo());

                assertNull(
                                result.value30DaysAgo());

                assertNull(
                                result.change1Day());

                assertNull(
                                result.change7Days());

                assertNull(
                                result.change30Days());

                assertNull(
                                result.changePercent7Days());

                assertNull(
                                result.changePercent30Days());

                /*
                 * Si aún no existe histórico, usamos el valor actual
                 * como mínimo y máximo conocidos.
                 */
                assertEquals(
                                5_000_000L,
                                result.historicalMinValue());

                assertEquals(
                                5_000_000L,
                                result.historicalMaxValue());

                assertNull(
                                result.purchasePrice());

                assertNull(
                                result.unrealizedProfit());

                assertNull(
                                result.unrealizedProfitPercent());

                assertNull(
                                result.season());

                assertEquals(
                                0,
                                result.totalPoints());

                assertEquals(
                                0,
                                result.matchesPlayed());

                assertNull(
                                result.averagePoints());

                assertNull(
                                result.recentAveragePoints());
        }

        @Test
        void getPlayerAnalyticsShouldOnlyUseCurrentSeasonReports() {

                Long leagueId = 1L;
                Long playerId = 25L;

                Player player = player(
                                playerId,
                                10_000_000L,
                                8_000_000L);

                when(playerRepository.findByIdAndLeague_Id(
                                playerId,
                                leagueId))
                                .thenReturn(Optional.of(player));

                when(playerPriceHistoryRepository
                                .findTopByPlayerIdAndPriceDateLessThanEqualOrderByPriceDateDesc(
                                                any(),
                                                any()))
                                .thenReturn(Optional.empty());

                when(playerPriceHistoryRepository
                                .findAllByPlayerIdOrderByPriceDateAsc(playerId))
                                .thenReturn(List.of());

                List<PlayerMatchReport> reports = List.of(
                                report(player, 10, "2026"),
                                report(player, 8, "2026"),
                                report(player, 6, "2026"),

                                report(player, 20, "2025"),
                                report(player, 18, "2025"),
                                report(player, 16, "2025"),
                                report(player, 14, "2025"),
                                report(player, 12, "2025"));

                when(playerMatchReportRepository
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                playerId))
                                .thenReturn(reports);

                PlayerAnalyticsResponse result = service
                                .getPlayerAnalytics(
                                                leagueId,
                                                playerId);

                assertEquals(
                                "2026",
                                result.season());

                assertEquals(
                                3,
                                result.matchesPlayed());

                assertEquals(
                                24,
                                result.totalPoints());

                assertEquals(
                                8.0,
                                result.averagePoints(),
                                0.000001);

                /*
                 * Solo hay tres partidos en la temporada actual.
                 *
                 * 10 + 8 + 6 = 24
                 * 24 / 3 = 8
                 *
                 * No deben utilizarse partidos de 2025
                 * para completar los cinco recientes.
                 */
                assertEquals(
                                8.0,
                                result.recentAveragePoints(),
                                0.000001);
        }

        @Test
        void getPlayerAnalyticsShouldRejectPlayerOutsideLeague() {

                Long leagueId = 1L;
                Long playerId = 30L;

                when(playerRepository.findByIdAndLeague_Id(
                                playerId,
                                leagueId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> service.getPlayerAnalytics(
                                                leagueId,
                                                playerId));

                verify(playerPriceHistoryRepository, never())
                                .findAllByPlayerIdOrderByPriceDateAsc(
                                                any());

                verify(playerMatchReportRepository, never())
                                .findAllByPlayer_IdAndParticipatedTrueAndPointsIsNotNullOrderByMatchDateDesc(
                                                any());
        }

        private Player player(
                        Long id,
                        Long marketValue,
                        Long purchasePrice) {

                Player player = mock(Player.class);

                when(player.getId())
                                .thenReturn(id);

                when(player.getName())
                                .thenReturn("Test Player");

                when(player.getPositions())
                                .thenReturn(List.of(PlayerPosition.MC));

                when(player.getMarketValue())
                                .thenReturn(marketValue);

                when(player.getPurchasePrice())
                                .thenReturn(purchasePrice);

                when(player.getProfitability())
                                .thenReturn(
                                                purchasePrice == null
                                                                ? null
                                                                : marketValue - purchasePrice);

                return player;
        }

        private PlayerPriceHistory price(
                        Long playerId,
                        Long leagueId,
                        LocalDate date,
                        Long marketValue) {

                return new PlayerPriceHistory(
                                playerId,
                                leagueId,
                                date,
                                marketValue,
                                PlayerPriceSource.BIWENGER_DETAIL,
                                LocalDateTime.now());
        }

        private PlayerMatchReport report(
                        Player player,
                        int points) {

                return report(
                                player,
                                points,
                                "2026");
        }

        private PlayerMatchReport report(
                        Player player,
                        int points,
                        String season) {

                return new PlayerMatchReport(
                                player,
                                System.nanoTime(),
                                1L,
                                "Jornada",
                                "J1",
                                LocalDateTime.now(),
                                season,
                                true,
                                null,
                                points);
        }
}