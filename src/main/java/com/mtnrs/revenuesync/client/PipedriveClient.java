package com.mtnrs.revenuesync.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class PipedriveClient {

    private final WebClient webClient;

    public PipedriveClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public String createLead(String requestJson) {

        return webClient.post()
                .uri("/mock/pipedrive")
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
