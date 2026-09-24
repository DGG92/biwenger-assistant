package com.artajerjes.biwengerassistant.config;

import java.util.Map;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import com.artajerjes.biwengerassistant.auth.LeagueAccessService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class LeagueAccessInterceptor implements HandlerInterceptor {

    private final LeagueAccessService leagueAccessService;

    public LeagueAccessInterceptor(
            LeagueAccessService leagueAccessService) {
        this.leagueAccessService = leagueAccessService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {

        Object attribute = request.getAttribute(
                HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);

        if (!(attribute instanceof java.util.Map<?, ?> variables)) {
            return true;
        }

        Object rawLeagueId = variables.get("leagueId");

        if (rawLeagueId == null) {
            return true;
        }

        Long leagueId;

        try {
            leagueId = Long.valueOf(rawLeagueId.toString());
        } catch (NumberFormatException exception) {
            return true;
        }

        leagueAccessService.validateAccess(leagueId);

        return true;
    }
}