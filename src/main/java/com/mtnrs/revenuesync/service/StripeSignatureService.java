package com.mtnrs.revenuesync.service;

import org.springframework.stereotype.Service;

@Service
public class StripeSignatureService {

    /**
     * Validate header Stripe-Signature.
     */
    public void verifyOrThrow(String payload, String stripeSignatureHeader, String webhookSecret) {
        if (stripeSignatureHeader == null || stripeSignatureHeader.isBlank()) {
            throw new IllegalArgumentException("Missing Stripe-Signature header");
        }


    }
}
