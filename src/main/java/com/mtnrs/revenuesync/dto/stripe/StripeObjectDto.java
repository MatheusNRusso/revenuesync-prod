package com.mtnrs.revenuesync.dto.stripe;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record StripeObjectDto(
        String id,
        @JsonProperty("amount_total") Long amountTotal,
        @JsonProperty("currency") String currency,
        @JsonProperty("customer_details") CustomerDetailsDto customerDetails,
        @JsonProperty("payment_status") String paymentStatus,
        Map<String, String> metadata
) {
    public record CustomerDetailsDto(
            @JsonProperty("email") String email,

            @JsonProperty("name")
            String name
    ) {}
}
