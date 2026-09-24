package com.artajerjes.biwengerassistant.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LeagueAccessInterceptor leagueAccessInterceptor;

    public WebMvcConfig(
            LeagueAccessInterceptor leagueAccessInterceptor) {
        this.leagueAccessInterceptor = leagueAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(leagueAccessInterceptor)
                .addPathPatterns("/api/leagues/**");
    }
}