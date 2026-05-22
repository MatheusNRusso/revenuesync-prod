package com.mtnrs.revenuesync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "stripe")
public record StripeConfig(
        String webhookSecret
) {}
