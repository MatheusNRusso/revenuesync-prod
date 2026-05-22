package com.mtnrs.revenuesync.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class GoogleAdsClient {

    private final WebClient webClient;

    public GoogleAdsClient(WebClient webClient) {

        this.webClient = webClient;
    }

    public String sendPurchaseConversion(String requestJson) {

        return webClient.post()
                .uri("/mock/google")
                .bodyValue(requestJson)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

}
