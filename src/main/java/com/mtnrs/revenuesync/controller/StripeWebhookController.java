package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.service.StripeWebhookService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/stripe")
public class StripeWebhookController {

    private final StripeWebhookService webhookService;

    public StripeWebhookController(StripeWebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PostMapping("/{merchantId}")
    public ResponseEntity<Void> receive(
            @PathVariable Long merchantId,
            @RequestBody String payload,
            @RequestHeader(name = "Stripe-Signature", required = false) String signature
    ) throws Exception {
        webhookService.handle(merchantId, payload, signature);
        return ResponseEntity.ok().build();
    }
}