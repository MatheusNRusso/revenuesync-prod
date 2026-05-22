package com.mtnrs.revenuesync.dto.stripe;

public record StripeWebhookHeaders(
        String signature
) {
}
