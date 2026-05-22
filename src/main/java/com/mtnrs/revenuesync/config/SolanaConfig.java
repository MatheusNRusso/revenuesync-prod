package com.mtnrs.revenuesync.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "solana")
public record SolanaConfig(

        /** RPC endpoint. Use devnet for testing, mainnet-beta for production */
        String rpcUrl,

        /** Default merchant wallet address to receive payments */
        String defaultRecipientWallet,

        /** Default payment expiration in minutes */
        int defaultExpirationMinutes,

        /** Max payments to verify per job cycle */
        int maxVerificationsPerCycle,

        /** Verification job interval in milliseconds */
        long verificationIntervalMs
) {
    public SolanaConfig {
        if (rpcUrl == null || rpcUrl.isBlank())
            rpcUrl = "https://api.devnet.solana.com";
        if (defaultExpirationMinutes <= 0)
            defaultExpirationMinutes = 15;
        if (maxVerificationsPerCycle <= 0)
            maxVerificationsPerCycle = 50;
        if (verificationIntervalMs <= 0)
            verificationIntervalMs = 5000;
    }
}
