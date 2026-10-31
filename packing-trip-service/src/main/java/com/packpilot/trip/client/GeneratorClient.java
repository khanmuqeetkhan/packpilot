package com.packpilot.trip.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GeneratorClient {

    private final RestClient restClient;

    public GeneratorClient(@Value("${app.generator.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public GeneratorResponse generate(GeneratorRequest request) {
        return restClient.post()
                .uri("/packing-lists/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(GeneratorResponse.class);
    }
}
