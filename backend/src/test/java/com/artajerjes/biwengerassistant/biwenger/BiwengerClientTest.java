package com.artajerjes.biwengerassistant.biwenger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
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

  @Test
  void getRoundLeagueShouldDeserializeEffectiveRoundLineup() {

    server.expect(
        requestTo(BASE_URL + "/api/v2/rounds/league"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                    {
                      "status": 200,
                      "data": {
                        "round": {
                          "id": 4899
                        },
                        "league": {
                          "id": 1268640,
                          "name": "VII Güenguer",
                          "competition": "la-liga",
                          "mode": "league",
                          "type": "premium",
                          "marketMode": "normal",
                          "scoreID": 100,
                          "standings": [
                            {
                              "id": 11467137,
                              "name": "Califato Omeya",
                              "points": 0,
                              "teamValue": 40000000,
                              "teamValueInc": 100000,
                              "position": 11,
                              "lineup": {
                                "type": "4-4-2",
                                "captain": {
                                  "id": 2184
                                },
                                "striker": {
                                  "id": 32435
                                },
                                "coach": {
                                  "id": 41088
                                },
                                "date": 1786811729,
                                "players": [
                                  2184,
                                  2600,
                                  9065,
                                  17731,
                                  17756,
                                  18397,
                                  27591,
                                  30789,
                                  32435,
                                  41416,
                                  42370
                                ],
                                "reserves": [
                                  null,
                                  41093,
                                  null,
                                  null
                                ],
                                "discarded": [
                                  1721
                                ],
                                "count": true
                              }
                            }
                          ],
                          "settings": {
                            "splitRound": "rollingLockout",
                            "lineupShow": "round",
                            "lineupRoundChanges": 1,
                            "lineupRoundChangesIn": "onlyNoPlayed",
                            "lineupRoundChangeStrategy": false
                          }
                        }
                      }
                    }
                    """,
                MediaType.APPLICATION_JSON));

    var response = biwengerClient.getRoundLeague();

    assertEquals(
        200,
        response.status());

    assertEquals(
        4899L,
        response.data().round().id());

    assertEquals(
        1268640L,
        response.data().league().id());

    assertEquals(
        "rollingLockout",
        response.data().league().settings().splitRound());

    assertEquals(
        "onlyNoPlayed",
        response.data().league().settings().lineupRoundChangesIn());

    assertEquals(
        1,
        response.data().league().standings().size());

    var lineup = response.data()
        .league()
        .standings()
        .get(0)
        .lineup();

    assertEquals(
        "4-4-2",
        lineup.type());

    assertEquals(
        11,
        lineup.players().size());

    assertEquals(
        2184L,
        lineup.captain().id());

    assertEquals(
        32435L,
        lineup.striker().id());

    assertEquals(
        41088L,
        lineup.coach().id());

    server.verify();
  }

  @Test
  void getRoundsShouldDeserializeCurrentRoundGames() {

    cdnServer.expect(
        requestTo(
            CDN_BASE_URL
                + "/api/v2/rounds/la-liga"
                + "?score=100&lang=es&v=631"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                    {
                      "status": 200,
                      "data": {
                        "id": 4899,
                        "name": "Jornada 1",
                        "short": "J1",
                        "status": "active",
                        "scoreID": 100,
                        "part": 1,
                        "games": [
                          {
                            "id": 50709,
                            "date": 1786815000,
                            "status": "finished",
                            "home": {
                              "id": 91,
                              "name": "Alavés",
                              "slug": "alaves",
                              "score": 3
                            },
                            "away": {
                              "id": 8,
                              "name": "Getafe",
                              "slug": "getafe",
                              "score": 0
                            },
                            "round": {
                              "id": 4899,
                              "name": "Jornada 1",
                              "short": "J1",
                              "part": 1
                            }
                          },
                          {
                            "id": 50712,
                            "date": 1786993200,
                            "status": "preview",
                            "home": {
                              "id": 6,
                              "name": "Deportivo",
                              "slug": "deportivo",
                              "score": null
                            },
                            "away": {
                              "id": 75,
                              "name": "Elche",
                              "slug": "elche",
                              "score": null
                            },
                            "round": {
                              "id": 4899,
                              "name": "Jornada 1",
                              "short": "J1",
                              "part": 1
                            }
                          }
                        ]
                      }
                    }
                    """,
                MediaType.APPLICATION_JSON));

    var response = biwengerClient.getRounds();

    assertEquals(
        200,
        response.status());

    assertEquals(
        4899L,
        response.data().id());

    assertEquals(
        "Jornada 1",
        response.data().name());

    assertEquals(
        "J1",
        response.data().shortName());

    assertEquals(
        "active",
        response.data().status());

    assertEquals(
        1,
        response.data().part());

    assertEquals(
        2,
        response.data().games().size());

    assertEquals(
        "finished",
        response.data().games().get(0).status());

    assertEquals(
        91L,
        response.data().games().get(0).home().id());

    assertEquals(
        "preview",
        response.data().games().get(1).status());

    assertEquals(
        1,
        response.data().games().get(1).round().part());

    cdnServer.verify();
  }

  @Test
  void getPlayerDetailShouldDeserializePriceHistory() {

    cdnServer.expect(
        requestTo(
            org.hamcrest.Matchers.containsString(
                CDN_BASE_URL
                    + "/api/v2/players/la-liga/raphinha")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                """
                    {
                      "status": 200,
                      "data": {
                        "id": 26930,
                        "name": "Raphinha",
                        "slug": "raphinha",
                        "prices": [
                          [260830, 17570000],
                          [260831, 17760000],
                          [260901, 17950000]
                        ],
                        "reports": [],
                        "seasons": []
                      }
                    }
                    """,
                MediaType.APPLICATION_JSON));

    var response = biwengerClient.getPlayerDetail(
        "raphinha");

    assertEquals(
        200,
        response.status());

    assertEquals(
        26930L,
        response.data().id());

    assertEquals(
        "raphinha",
        response.data().slug());

    assertEquals(
        3,
        response.data().prices().size());

    assertEquals(
        260830L,
        response.data().prices().get(0).get(0));

    assertEquals(
        17_570_000L,
        response.data().prices().get(0).get(1));

    assertEquals(
        260901L,
        response.data().prices().get(2).get(0));

    assertEquals(
        17_950_000L,
        response.data().prices().get(2).get(1));

    cdnServer.verify();
  }
}