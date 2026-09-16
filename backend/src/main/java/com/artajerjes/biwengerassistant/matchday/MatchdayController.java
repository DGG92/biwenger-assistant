package com.artajerjes.biwengerassistant.matchday;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.matchday.dto.MatchdayResponse;
import com.artajerjes.biwengerassistant.matchday.dto.MatchdayRoundOptionResponse;

@RestController
@RequestMapping("/api/matchday")
public class MatchdayController {

    private final MatchdayService matchdayService;

    public MatchdayController(
            MatchdayService matchdayService) {

        this.matchdayService = matchdayService;
    }

    @GetMapping
    public MatchdayResponse getCurrentMatchday() {
        return matchdayService.getCurrentMatchday();
    }

    @GetMapping("/rounds")
    public List<MatchdayRoundOptionResponse> getAvailableRounds() {

        return matchdayService.getAvailableRounds();
    }

    @GetMapping("/{roundId}")
    public MatchdayResponse getMatchday(
            @PathVariable Long roundId) {

        return matchdayService.getMatchday(roundId);
    }
}