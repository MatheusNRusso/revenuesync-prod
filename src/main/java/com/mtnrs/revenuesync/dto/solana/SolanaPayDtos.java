package com.mtnrs.revenuesync.dto.solana;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class SolanaPayDtos {

    // ── Create payment request ────────────────────────────────────────────────

    public record CreatePaymentRequest(
            @NotNull @DecimalMin("0.000000001")
            BigDecimal amount,

            /** "SOL" or SPL Token mint address (e.g. USDC mint) */
            String currency,

            /** SPL Token mint address. Null means native SOL */
            String splToken,

            String customerEmail,
            String label,
            String message,

            /** Minutes until the payment expires. Defaults to 15 */
            Integer expirationMinutes
    ) {
        public int resolvedExpiration() {
            return (expirationMinutes != null && expirationMinutes > 0) ? expirationMinutes : 15;
        }
    }

    // ── Create payment response ───────────────────────────────────────────────

    public record CreatePaymentResponse(
            String reference,
            String solanaPayUrl,
            String qrCodeData,
            String status,
            String amount,
            String currency,
            String recipientWallet,
            String expiresAt,
            String createdAt
    ) {}

    // ── Payment status response ───────────────────────────────────────────────

    public record PaymentStatusResponse(
            String reference,
            String status,
            String txSignature,
            Long paymentId,
            String confirmedAt,
            String expiresAt
    ) {}
}
