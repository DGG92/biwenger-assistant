package com.artajerjes.biwengerassistant.biwenger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.artajerjes.biwengerassistant.biwenger.dto.TestApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerLeagueApiResponse;

import tools.jackson.databind.ObjectMapper;

@Component
public class BiwengerClient {

    private final RestClient restClient;
    private final String token;
    private final String leagueId;
    private final String userId;
    private final String version;
    private final String language;
    private final ObjectMapper objectMapper;

    public BiwengerClient(
        RestClient.Builder restClientBuilder,
        ObjectMapper objectMapper,
        @Value("${biwenger.base-url}") String baseUrl,
        @Value("${biwenger.token}") String token,
        @Value("${biwenger.league-id}") String leagueId,
        @Value("${biwenger.user-id}") String userId,
        @Value("${biwenger.version}") String version,
        @Value("${biwenger.language}") String language
    ) {
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .build();

        this.objectMapper = objectMapper;
        this.token = token;
        this.leagueId = leagueId;
        this.userId = userId;
        this.version = version;
        this.language = language;
}

    public BiwengerLeagueApiResponse getLeague() {
        byte[] responseBody = restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v2/league")
                        .queryParam("include", "all,-lastAccess")
                        .queryParam(
                                "fields",
                                "*,standings,tournaments,group,settings(description)"
                        )
                        .build()
                )
                .headers(headers -> {
                    headers.setBearerAuth(token);
                    headers.set("x-league", leagueId);
                    headers.set("x-user", userId);
                    headers.set("x-version", version);
                    headers.set("x-lang", language);
                })
                .retrieve()
                .body(byte[].class);

        if (responseBody == null) {
            throw new IllegalStateException(
                    "Biwenger returned an empty response"
            );
        }

        try {
            return objectMapper.readValue(
                    responseBody,
                    BiwengerLeagueApiResponse.class
            );
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not deserialize Biwenger league response",
                    exception
            );
        }
    }

    public TestApiResponse testConnection() {
        return restClient
                .get()
                .uri("https://jsonplaceholder.typicode.com/todos/1")
                .retrieve()
                .body(TestApiResponse.class);
    }
}