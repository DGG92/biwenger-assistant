package com.artajerjes.biwengerassistant.biwenger;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.artajerjes.biwengerassistant.biwenger.dto.TestApiResponse;

@Component
public class BiwengerClient {
    private final RestClient restClient;

    public BiwengerClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
            .baseUrl("https://jsonplaceholder.typicode.com")
            .build();
    }

    public TestApiResponse testConnection() {
        return restClient
            .get()
            .uri("/todos/1")
            .retrieve()
            .body(TestApiResponse.class);
    }
}
