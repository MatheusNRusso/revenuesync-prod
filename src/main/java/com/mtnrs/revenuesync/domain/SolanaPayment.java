package com.mtnrs.revenuesync.domain;

import com.mtnrs.revenuesync.domain.enums.SolanaPaymentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        name = "solana_payments",
        indexes = {
                @Index(name = "idx_solana_reference", columnList = "reference", unique = true),
                @Index(name = "idx_solana_status", columnList = "status"),
                @Index(name = "idx_solana_merchant", columnList = "merchant_id"),
                @Index(name = "idx_solana_lead", columnList = "lead_id"),
                @Index(name = "idx_solana_created_at", columnList = "created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class SolanaPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 32-byte base58 reference used to locate the transaction on-chain */
    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    /** Merchant's destination wallet address */
    @Column(name = "recipient_wallet", nullable = false, length = 64)
    private String recipientWallet;

    @Column(name = "amount", nullable = false, precision = 19, scale = 9)
    private BigDecimal amount;

    /** SOL or SPL Token mint address (e.g. USDC) */
    @Column(nullable = false, length = 64)
    private String currency;

    /** SPL Token mint address — null means native SOL */
    @Column(name = "spl_token", length = 64)
    private String splToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SolanaPaymentStatus status;

    /** Confirmed on-chain transaction signature */
    @Column(name = "tx_signature", length = 128)
    private String txSignature;

    /** Payment ID created in the existing pipeline after confirmation */
    @Column(name = "payment_id")
    private Long paymentId;

    @Column(name = "merchant_id", nullable = false)
    private Long merchantId;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "customer_email", length = 180)
    private String customerEmail;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    public static SolanaPayment create(
            Long merchantId,
            Long leadId,
            String reference,
            String recipientWallet,
            BigDecimal amount,
            String currency,
            String splToken,
            String customerEmail,
            String label,
            String message,
            int expirationMinutes
    ) {
        if (merchantId == null) throw new IllegalArgumentException("merchantId is required");
        if (reference == null) throw new IllegalArgumentException("reference is required");
        if (recipientWallet == null) throw new IllegalArgumentException("recipientWallet is required");
        if (amount == null || amount.signum() <= 0) throw new IllegalArgumentException("amount must be > 0");

        var now = OffsetDateTime.now();

        return SolanaPayment.builder()
                .merchantId(merchantId)
                .leadId(leadId)
                .reference(reference)
                .recipientWallet(recipientWallet)
                .amount(amount)
                .currency(currency != null ? currency : "SOL")
                .splToken(splToken)
                .status(SolanaPaymentStatus.PENDING)
                .customerEmail(customerEmail)
                .label(label)
                .message(message)
                .createdAt(now)
                .expiresAt(now.plusMinutes(expirationMinutes))
                .build();
    }

    public void confirm(String txSignature, Long paymentId) {
        if (this.status != SolanaPaymentStatus.PENDING) {
            throw new IllegalStateException("Only PENDING payments can be confirmed");
        }

        this.status = SolanaPaymentStatus.CONFIRMED;
        this.txSignature = txSignature;
        this.paymentId = paymentId;
        this.confirmedAt = OffsetDateTime.now();
    }

    public void expire() {
        if (this.status == SolanaPaymentStatus.PENDING) {
            this.status = SolanaPaymentStatus.EXPIRED;
        }
    }

    public void fail() {
        if (this.status == SolanaPaymentStatus.PENDING) {
            this.status = SolanaPaymentStatus.FAILED;
        }
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = OffsetDateTime.now();
        if (status == null) status = SolanaPaymentStatus.PENDING;
    }
}