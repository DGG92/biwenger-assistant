package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.ObjectMapper;

class BiwengerClientTest {

    private static final String BASE_URL = "https://biwenger.test";
    private static final String CDN_BASE_URL = "https://cdn.biwenger.test";

    private MockRestServiceServer server;
    private MockRestServiceServer cdnServer;
    private BiwengerClient biwengerClient;

    @BeforeEach
    void setUp() {

        RestClient.Builder restBuilder = RestClient.builder()
                .baseUrl(BASE_URL);

        RestClient.Builder cdnBuilder = RestClient.builder()
                .baseUrl(CDN_BASE_URL);

        server = MockRestServiceServer
                .bindTo(restBuilder)
                .build();

        cdnServer = MockRestServiceServer
                .bindTo(cdnBuilder)
                .build();

        biwengerClient = new BiwengerClient(
                restBuilder.build(),
                cdnBuilder.build(),
                new ObjectMapper(),
                "test-token",
                "1268640",
                "4023835",
                "631",
                "es",
                "la-liga",
                100);
    }

    @Test
    void getMarketShouldDeserializeSuccessfulResponse() {

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "status": 200,
                                          "data": {
                                            "sales": [],
                                            "auctions": []
                                          }
                                        }
                                        """,
                                MediaType.APPLICATION_JSON));

        var response = biwengerClient.getMarket();

        assertEquals(
                200,
                response.status());

        server.verify();
    }

    @Test
    void getMarketShouldPropagateServerErrorAfterRetryFails() {

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        assertThrows(
                HttpServerErrorException.class,
                () -> biwengerClient.getMarket());

        server.verify();
    }

    @Test
    void getCompetitionShouldDeserializeSuccessfulResponse() {

        cdnServer.expect(
                requestTo(
                        CDN_BASE_URL
                                + "/api/v2/competitions/la-liga/data"
                                + "?lang=es&score=100"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "status": 200,
                                          "data": {
                                            "players": {},
                                            "teams": {}
                                          }
                                        }
                                        """,
                                MediaType.APPLICATION_JSON));

        var response = biwengerClient.getCompetition();

        assertEquals(
                200,
                response.status());

        cdnServer.verify();
    }

    @Test
    void getMarketShouldRetryOnceAfterServerError() {

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError());

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "status": 200,
                                          "data": {
                                            "sales": [],
                                            "auctions": []
                                          }
                                        }
                                        """,
                                MediaType.APPLICATION_JSON));

        var response = biwengerClient.getMarket();

        assertEquals(
                200,
                response.status());

        server.verify();
    }

    @Test
    void getMarketShouldNotRetryClientError() {

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest());

        assertThrows(
                org.springframework.web.client.HttpClientErrorException.class,
                () -> biwengerClient.getMarket());

        server.verify();
    }

    @Test
    void getMarketShouldRetryOnceAfterResourceAccessException() {

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(request -> {
                    throw new ResourceAccessException(
                            "Temporary network failure");
                });

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "status": 200,
                                          "data": {
                                            "sales": [],
                                            "auctions": []
                                          }
                                        }
                                        """,
                                MediaType.APPLICATION_JSON));

        var response = biwengerClient.getMarket();

        assertEquals(
                200,
                response.status());

        server.verify();
    }

    @Test
    void getMarketShouldNotRetryDeserializationError() {

        server.expect(
                requestTo(BASE_URL + "/api/v2/market"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(
                        withSuccess(
                                """
                                        {
                                          "status": 200,
                                          "data": {
                                            "sales":
                                        }
                                        """,
                                MediaType.APPLICATION_JSON));

        assertThrows(
                IllegalStateException.class,
                () -> biwengerClient.getMarket());

        server.verify();
    }
}