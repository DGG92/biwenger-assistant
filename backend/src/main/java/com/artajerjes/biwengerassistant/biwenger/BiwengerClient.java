package com.artajerjes.biwengerassistant.biwenger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.artajerjes.biwengerassistant.biwenger.dto.TestApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerHomeResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerLeagueApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.user.BiwengerUserResponse;

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

        private final RestClient cdnRestClient;
        private final String competition;
        private final Integer score;

        public BiwengerClient(
                        RestClient.Builder restClientBuilder,
                        ObjectMapper objectMapper,
                        @Value("${biwenger.base-url}") String baseUrl,
                        @Value("${biwenger.cdn-base-url}") String cdnBaseUrl,
                        @Value("${biwenger.token}") String token,
                        @Value("${biwenger.league-id}") String leagueId,
                        @Value("${biwenger.user-id}") String userId,
                        @Value("${biwenger.version}") String version,
                        @Value("${biwenger.language}") String language,
                        @Value("${biwenger.competition}") String competition,
                        @Value("${biwenger.score}") Integer score) {
                this.restClient = restClientBuilder
                                .baseUrl(baseUrl)
                                .build();

                this.cdnRestClient = restClientBuilder
                                .clone()
                                .baseUrl(cdnBaseUrl)
                                .build();

                this.objectMapper = objectMapper;
                this.token = token;
                this.leagueId = leagueId;
                this.userId = userId;
                this.version = version;
                this.language = language;
                this.competition = competition;
                this.score = score;
        }

        public BiwengerCompetitionResponse getCompetition() {
                byte[] responseBody = cdnRestClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/v2/competitions/{competition}/data")
                                                .queryParam("lang", language)
                                                .queryParam("score", score)
                                                .build(competition))
                                .retrieve()
                                .body(byte[].class);

                if (responseBody == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an empty competition response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerCompetitionResponse.class);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger competition response",
                                        exception);
                }
        }

        public BiwengerLeagueApiResponse getLeague() {
                byte[] responseBody = restClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/v2/league")
                                                .queryParam("include", "all,-lastAccess")
                                                .queryParam(
                                                                "fields",
                                                                "*,standings,tournaments,group,settings(description)")
                                                .build())
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
                                        "Biwenger returned an empty response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerLeagueApiResponse.class);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger league response",
                                        exception);
                }
        }

        public BiwengerUserResponse getUser(Long managerId) {
                byte[] responseBody = restClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/v2/user/{managerId}")
                                                .queryParam(
                                                                "fields",
                                                                "*,account(id),players(id,owner),lineups(round,points,count,position),league(id,name,competition,type,mode,marketMode,scoreID),market,seasons,offers,lastPositions,marketTransactions")
                                                .build(managerId))
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
                                        "Biwenger returned an empty user response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerUserResponse.class);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger user response",
                                        exception);
                }
        }

        public BiwengerUserResponse getCurrentUser() {
                byte[] responseBody = restClient
                                .get()
                                .uri(uriBuilder -> uriBuilder
                                                .path("/api/v2/user")
                                                .queryParam(
                                                                "fields",
                                                                "*,lineup(type,playersID,reservesID,captain,striker,coach,date),players(id,owner),market,offers,-trophies")
                                                .build())
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
                                        "Biwenger returned an empty current user response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerUserResponse.class);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger current user response",
                                        exception);
                }
        }

        public BiwengerMarketResponse getMarket() {
                byte[] responseBody = restClient
                                .get()
                                .uri("/api/v2/market")
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
                                        "Biwenger returned an empty market response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerMarketResponse.class);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger market response",
                                        exception);
                }
        }

        public BiwengerHomeResponse getHome() {
                byte[] responseBody = restClient
                                .get()
                                .uri("/api/v2/home")
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
                                        "Biwenger returned an empty home response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerHomeResponse.class);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger home response",
                                        exception);
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