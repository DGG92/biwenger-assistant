package com.artajerjes.biwengerassistant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
public class SecurityConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        AuthenticationManager authenticationManager(
                        AuthenticationConfiguration configuration) throws Exception {
                return configuration.getAuthenticationManager();
        }

        @Bean
        SecurityFilterChain securityFilterChain(HttpSecurity http)
                        throws Exception {

                http
                                .cors(cors -> {
                                })
                                .csrf(csrf -> csrf.disable())

                                .exceptionHandling(exceptions -> exceptions
                                                .authenticationEntryPoint(
                                                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                                                .accessDeniedHandler(
                                                                (request, response, exception) -> response.sendError(
                                                                                HttpStatus.FORBIDDEN.value())))

                                .authorizeHttpRequests(auth -> auth

                                                // Auth pública
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/auth/login")
                                                .permitAll()

                                                // Auth del usuario actual
                                                .requestMatchers(
                                                                "/api/auth/me",
                                                                "/api/auth/logout",
                                                                "/api/auth/change-password")
                                                .authenticated()

                                                // Administración de Biwenger Assistant
                                                .requestMatchers("/api/admin/**")
                                                .hasRole("ADMIN")

                                                // Sincronización general
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/biwenger/sync/*")
                                                .hasRole("ADMIN")

                                                // Managers
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/managers/sync")
                                                .hasRole("ADMIN")

                                                // Market
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/market/sync")
                                                .hasRole("ADMIN")

                                                // Movements
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/movements/sync")
                                                .hasRole("ADMIN")

                                                // Offers
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/offers/sync")
                                                .hasRole("ADMIN")

                                                // Players: sincronizaciones generales
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/players/sync",
                                                                "/api/leagues/*/players/sync-ownership",
                                                                "/api/leagues/*/players/sync-lineup",
                                                                "/api/leagues/*/players/reports/sync",
                                                                "/api/leagues/*/players/prices/sync",
                                                                "/api/leagues/*/players/details/sync")
                                                .hasRole("ADMIN")

                                                // Players: sincronizaciones individuales
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/players/*/reports/sync",
                                                                "/api/leagues/*/players/*/prices/sync")
                                                .hasRole("ADMIN")

                                                // Sync Center
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/leagues/*/sync/status")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/sync/now")
                                                .hasRole("ADMIN")

                                                // Toda la API restante requiere sesión
                                                .requestMatchers("/api/**")
                                                .authenticated()

                                                .anyRequest()
                                                .permitAll());

                return http.build();
        }
}