package com.mtnrs.revenuesync.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;

@Component
public class MetaCapiClient {

    private final WebClient webClient;

    public MetaCapiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public String sendPurchaseEvent(String requestJson) {

        return webClient.post()
                .uri("/mock/meta")
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
