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
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

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
                                .csrf(csrf -> {
                                        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository
                                                        .withHttpOnlyFalse();

                                        csrfTokenRepository.setCookieCustomizer(cookie -> cookie
                                                        .path("/")
                                                        .secure(true)
                                                        .sameSite("None"));

                                        CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();

                                        requestHandler.setCsrfRequestAttributeName(null);

                                        csrf
                                                        .csrfTokenRepository(csrfTokenRepository)
                                                        .csrfTokenRequestHandler(requestHandler)
                                                        .ignoringRequestMatchers("/api/auth/login");
                                })

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

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/auth/csrf")
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
                                                // El estado puede consultarlo cualquier usuario autenticado.
                                                // LeagueAccessInterceptor valida que el usuario tenga acceso
                                                // a la liga solicitada.
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/leagues/*/sync/status")
                                                .authenticated()

                                                // Iniciar una sincronización sigue siendo exclusivo de ADMIN.
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/sync/now")
                                                .hasRole("ADMIN")

                                                // Gestión directa de ligas
                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/leagues")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.GET,
                                                                "/api/leagues/*")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/leagues/*")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/leagues/*")
                                                .hasRole("ADMIN")

                                                // Gestión manual de jugadores
                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/leagues/*/players")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.PUT,
                                                                "/api/leagues/*/players/*")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.DELETE,
                                                                "/api/leagues/*/players/*")
                                                .hasRole("ADMIN")

                                                // Toda la API restante requiere sesión
                                                .requestMatchers("/api/**")
                                                .authenticated()

                                                .anyRequest()
                                                .permitAll());

                return http.build();
        }
}