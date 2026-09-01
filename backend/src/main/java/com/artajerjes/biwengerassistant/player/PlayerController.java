package com.artajerjes.biwengerassistant.player;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.biwenger.PlayerDetailSyncService;
import com.artajerjes.biwengerassistant.biwenger.dto.sync.PlayerDetailSyncResponse;
import com.artajerjes.biwengerassistant.history.dto.PlayerPriceHistorySyncResponse;
import com.artajerjes.biwengerassistant.player.dto.CreatePlayerRequest;
import com.artajerjes.biwengerassistant.player.dto.PlayerAnalyticsResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerLineupSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerOwnershipSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerResponse;
import com.artajerjes.biwengerassistant.player.dto.PlayerSyncResponse;
import com.artajerjes.biwengerassistant.player.dto.UpdatePlayerRequest;
import com.artajerjes.biwengerassistant.playerreport.dto.PlayerReportSyncResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/leagues/{leagueId}/players")
public class PlayerController {

        private final PlayerService playerService;
        private final PlayerDetailSyncService playerDetailSyncService;
        private final PlayerAnalyticsService playerAnalyticsService;

        public PlayerController(PlayerService playerService, PlayerDetailSyncService playerDetailSyncService,
                        PlayerAnalyticsService playerAnalyticsService) {
                this.playerService = playerService;
                this.playerDetailSyncService = playerDetailSyncService;
                this.playerAnalyticsService = playerAnalyticsService;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        public PlayerResponse create(
                        @PathVariable Long leagueId,
                        @Valid @RequestBody CreatePlayerRequest request) {
                return playerService.create(leagueId, request);
        }

        @GetMapping
        public List<PlayerResponse> findAll(
                        @PathVariable Long leagueId) {
                return playerService.findAll(leagueId);
        }

        @GetMapping("/{playerId}")
        public PlayerResponse findById(
                        @PathVariable Long leagueId,
                        @PathVariable Long playerId) {
                return playerService.findById(
                                leagueId,
                                playerId);
        }

        @GetMapping("/{playerId}/analytics")
        public PlayerAnalyticsResponse getPlayerAnalytics(
                        @PathVariable Long leagueId,
                        @PathVariable Long playerId) {

                return playerAnalyticsService.getPlayerAnalytics(
                                leagueId,
                                playerId);
        }

        @PutMapping("/{playerId}")
        public PlayerResponse update(
                        @PathVariable Long leagueId,
                        @PathVariable Long playerId,
                        @Valid @RequestBody UpdatePlayerRequest request) {
                return playerService.update(
                                leagueId,
                                playerId,
                                request);
        }

        @DeleteMapping("/{playerId}")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        public void delete(
                        @PathVariable Long leagueId,
                        @PathVariable Long playerId) {
                playerService.delete(
                                leagueId,
                                playerId);
        }

        @PostMapping("/sync")
        public PlayerSyncResponse syncCompetitionPlayers(
                        @PathVariable Long leagueId) {
                return playerService.syncCompetitionPlayers(leagueId);
        }

        @PostMapping("/sync-ownership")
        public PlayerOwnershipSyncResponse syncPlayerOwnership(
                        @PathVariable Long leagueId) {
                return playerService.syncPlayerOwnership(leagueId);
        }

        @PostMapping("/sync-lineup")
        public PlayerLineupSyncResponse syncCurrentLineup(
                        @PathVariable Long leagueId) {
                return playerService.syncCurrentLineup(leagueId);
        }

        @PostMapping("/{playerId}/reports/sync")
        public int syncPlayerReports(
                        @PathVariable Long leagueId,
                        @PathVariable Long playerId) {

                return playerService.syncPlayerReports(
                                leagueId,
                                playerId);
        }

        @PostMapping("/reports/sync")
        public PlayerReportSyncResponse syncLeagueReports(
                        @PathVariable Long leagueId) {

                return playerService
                                .syncLeagueReports(leagueId);
        }

        @PostMapping("/prices/sync")
        public PlayerPriceHistorySyncResponse syncLeaguePriceHistory(
                        @PathVariable Long leagueId) {

                return playerService.syncLeaguePriceHistory(
                                leagueId);
        }

        @PostMapping("/{playerId}/prices/sync")
        public int syncPlayerPriceHistory(
                        @PathVariable Long leagueId,
                        @PathVariable Long playerId) {

                return playerService.syncPlayerPriceHistory(
                                leagueId,
                                playerId);
        }

        @PostMapping("/details/sync")
        public PlayerDetailSyncResponse syncPlayerDetails(
                        @PathVariable Long leagueId) {

                return playerDetailSyncService
                                .syncLeaguePlayerDetails(leagueId);
        }
}