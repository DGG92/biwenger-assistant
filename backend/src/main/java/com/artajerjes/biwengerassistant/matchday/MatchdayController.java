package com.artajerjes.biwengerassistant.matchday;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.artajerjes.biwengerassistant.matchday.dto.MatchdayResponse;

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
}