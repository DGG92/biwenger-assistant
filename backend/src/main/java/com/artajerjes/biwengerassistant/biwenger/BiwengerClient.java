package com.artajerjes.biwengerassistant.biwenger;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.artajerjes.biwengerassistant.biwenger.dto.TestApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.competition.BiwengerCompetitionResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.home.BiwengerHomeResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.league.BiwengerLeagueApiResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.market.BiwengerMarketResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.playerdetail.BiwengerPlayerDetailResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.report.BiwengerReportResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.roundleague.BiwengerRoundLeagueResponse;
import com.artajerjes.biwengerassistant.biwenger.dto.rounds.BiwengerRoundsResponse;
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

        @Autowired
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

                ClientHttpRequestFactory requestFactory = createDefaultRequestFactory();

                this.restClient = restClientBuilder
                                .requestFactory(requestFactory)
                                .baseUrl(baseUrl)
                                .build();

                this.cdnRestClient = restClientBuilder
                                .clone()
                                .requestFactory(requestFactory)
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

        BiwengerClient(
                        RestClient restClient,
                        RestClient cdnRestClient,
                        ObjectMapper objectMapper,
                        String token,
                        String leagueId,
                        String userId,
                        String version,
                        String language,
                        String competition,
                        Integer score) {

                this.restClient = restClient;
                this.cdnRestClient = cdnRestClient;
                this.objectMapper = objectMapper;
                this.token = token;
                this.leagueId = leagueId;
                this.userId = userId;
                this.version = version;
                this.language = language;
                this.competition = competition;
                this.score = score;
        }

        private static ClientHttpRequestFactory createDefaultRequestFactory() {

                HttpClient httpClient = HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(10))
                                .build();

                JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);

                requestFactory.setReadTimeout(
                                Duration.ofSeconds(30));

                return requestFactory;
        }

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerCompetitionResponse getCompetition() {

                byte[] responseBody = executeWithRetry(
                                () -> cdnRestClient
                                                .get()
                                                .uri(uriBuilder -> uriBuilder
                                                                .path("/api/v2/competitions/{competition}/data")
                                                                .queryParam("lang", language)
                                                                .queryParam("score", score)
                                                                .build(competition))
                                                .retrieve()
                                                .body(byte[].class));

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

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerPlayerDetailResponse getPlayerDetail(
                        String playerSlug) {

                byte[] responseBody = executeWithRetry(
                                () -> cdnRestClient
                                                .get()
                                                .uri(uriBuilder -> uriBuilder
                                                                .path("/api/v2/players/{competition}/{playerSlug}")
                                                                .queryParam("lang", language)
                                                                .queryParam(
                                                                                "fields",
                                                                                "*,team,fitness,reports(points,rawStats,home,events,status(status,statusInfo),match(*,round,home,away),star),prices,competition,seasons,news,threads")
                                                                .build(
                                                                                competition,
                                                                                playerSlug))
                                                .retrieve()
                                                .body(byte[].class));

                if (responseBody == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an empty player detail response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerPlayerDetailResponse.class);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger player detail response",
                                        exception);
                }
        }

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerLeagueApiResponse getLeague() {

                byte[] responseBody = executeWithRetry(
                                () -> restClient
                                                .get()
                                                .uri(uriBuilder -> uriBuilder
                                                                .path("/api/v2/league")
                                                                .queryParam(
                                                                                "include",
                                                                                "all,-lastAccess")
                                                                .queryParam(
                                                                                "fields",
                                                                                "*,standings,tournaments,group,settings(customScore,splitRound,lineupRoundChangesIn)")
                                                                .build())
                                                .headers(headers -> {
                                                        headers.setBearerAuth(token);
                                                        headers.set(
                                                                        "x-league",
                                                                        leagueId);
                                                        headers.set(
                                                                        "x-user",
                                                                        userId);
                                                        headers.set(
                                                                        "x-version",
                                                                        version);
                                                        headers.set(
                                                                        "x-lang",
                                                                        language);
                                                })
                                                .retrieve()
                                                .body(byte[].class));

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

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerUserResponse getUser(Long managerId) {

                byte[] responseBody = executeWithRetry(
                                () -> restClient
                                                .get()
                                                .uri(uriBuilder -> uriBuilder
                                                                .path("/api/v2/user/{managerId}")
                                                                .queryParam(
                                                                                "fields",
                                                                                "*,account(id),players(id,owner),lineups(round,points,count,position),league(id,name,competition,type,mode,marketMode,scoreID),market,seasons,offers,lastPositions,marketTransactions")
                                                                .build(managerId))
                                                .headers(headers -> {
                                                        headers.setBearerAuth(token);
                                                        headers.set(
                                                                        "x-league",
                                                                        leagueId);
                                                        headers.set(
                                                                        "x-user",
                                                                        userId);
                                                        headers.set(
                                                                        "x-version",
                                                                        version);
                                                        headers.set(
                                                                        "x-lang",
                                                                        language);
                                                })
                                                .retrieve()
                                                .body(byte[].class));

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

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerUserResponse getCurrentUser() {

                byte[] responseBody = executeWithRetry(
                                () -> restClient
                                                .get()
                                                .uri(uriBuilder -> uriBuilder
                                                                .path("/api/v2/user")
                                                                .queryParam(
                                                                                "fields",
                                                                                "*,lineup(type,playersID,reservesID,reserves(id,position),captain,striker,coach,date),players(id,owner),market,offers,-trophies")
                                                                .build())
                                                .headers(headers -> {
                                                        headers.setBearerAuth(token);
                                                        headers.set(
                                                                        "x-league",
                                                                        leagueId);
                                                        headers.set(
                                                                        "x-user",
                                                                        userId);
                                                        headers.set(
                                                                        "x-version",
                                                                        version);
                                                        headers.set(
                                                                        "x-lang",
                                                                        language);
                                                })
                                                .retrieve()
                                                .body(byte[].class));

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

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerRoundLeagueResponse getRoundLeague() {

                byte[] responseBody = executeWithRetry(
                                () -> restClient
                                                .get()
                                                .uri("/api/v2/rounds/league")
                                                .headers(headers -> {
                                                        headers.setBearerAuth(token);
                                                        headers.set(
                                                                        "x-league",
                                                                        leagueId);
                                                        headers.set(
                                                                        "x-user",
                                                                        userId);
                                                        headers.set(
                                                                        "x-version",
                                                                        version);
                                                        headers.set(
                                                                        "x-lang",
                                                                        language);
                                                })
                                                .retrieve()
                                                .body(byte[].class));

                if (responseBody == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an empty round league response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerRoundLeagueResponse.class);

                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger round league response",
                                        exception);
                }
        }

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerRoundsResponse getRounds() {

                byte[] responseBody = executeWithRetry(
                                () -> cdnRestClient
                                                .get()
                                                .uri(uriBuilder -> uriBuilder
                                                                .path("/api/v2/rounds/{competition}")
                                                                .queryParam("score", score)
                                                                .queryParam("lang", language)
                                                                .queryParam("v", version)
                                                                .build(competition))
                                                .retrieve()
                                                .body(byte[].class));

                if (responseBody == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an empty rounds response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerRoundsResponse.class);

                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger rounds response",
                                        exception);
                }
        }

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerMarketResponse getMarket() {

                byte[] responseBody = executeWithRetry(
                                () -> restClient
                                                .get()
                                                .uri("/api/v2/market")
                                                .headers(headers -> {
                                                        headers.setBearerAuth(token);
                                                        headers.set(
                                                                        "x-league",
                                                                        leagueId);
                                                        headers.set(
                                                                        "x-user",
                                                                        userId);
                                                        headers.set(
                                                                        "x-version",
                                                                        version);
                                                        headers.set(
                                                                        "x-lang",
                                                                        language);
                                                })
                                                .retrieve()
                                                .body(byte[].class));

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

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerHomeResponse getHome() {

                byte[] responseBody = executeWithRetry(
                                () -> restClient
                                                .get()
                                                .uri("/api/v2/home")
                                                .headers(headers -> {
                                                        headers.setBearerAuth(token);
                                                        headers.set(
                                                                        "x-league",
                                                                        leagueId);
                                                        headers.set(
                                                                        "x-user",
                                                                        userId);
                                                        headers.set(
                                                                        "x-version",
                                                                        version);
                                                        headers.set(
                                                                        "x-lang",
                                                                        language);
                                                })
                                                .retrieve()
                                                .body(byte[].class));

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

        @SuppressWarnings("UseSpecificCatch")
        public BiwengerReportResponse getReport(
                        String report,
                        String param) {

                byte[] responseBody = executeWithRetry(
                                () -> restClient
                                                .get()
                                                .uri(uriBuilder -> {
                                                        uriBuilder
                                                                        .path("/api/v2/league/current/report/{report}")
                                                                        .queryParam(
                                                                                        "mode",
                                                                                        "total");

                                                        if (param != null
                                                                        && !param.isBlank()) {
                                                                uriBuilder.queryParam(
                                                                                "param",
                                                                                param);
                                                        }

                                                        return uriBuilder.build(
                                                                        report);
                                                })
                                                .headers(headers -> {
                                                        headers.setBearerAuth(token);
                                                        headers.set(
                                                                        "x-league",
                                                                        leagueId);
                                                        headers.set(
                                                                        "x-user",
                                                                        userId);
                                                        headers.set(
                                                                        "x-version",
                                                                        version);
                                                        headers.set(
                                                                        "x-lang",
                                                                        language);
                                                })
                                                .retrieve()
                                                .body(byte[].class));

                if (responseBody == null) {
                        throw new IllegalStateException(
                                        "Biwenger returned an empty report response");
                }

                try {
                        return objectMapper.readValue(
                                        responseBody,
                                        BiwengerReportResponse.class);
                } catch (Exception exception) {
                        throw new IllegalStateException(
                                        "Could not deserialize Biwenger report response",
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

        private <T> T executeWithRetry(Supplier<T> action) {

                try {
                        return action.get();

                } catch (HttpServerErrorException
                                | ResourceAccessException firstException) {

                        return action.get();
                }
        }
}