package com.mtnrs.revenuesync.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.domain.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        name = "payments",
        indexes = {
            @Index(name = "idx_payments_external_id", columnList = "external_id", unique = true),
            @Index(name = "idx_payments_created_at", columnList = "created_at"),
            @Index(name = "idx_payments_event_id", columnList = "event_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", nullable = false, updatable = false, unique = true, length = 120)
    private String externalId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 9)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 10)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_email", length = 180)
    private String customerEmail;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "event_id", length = 120)
    private String eventId;

    @Column(name = "notified_at")
    private OffsetDateTime notifiedAt;

    @JsonIgnore
    @JoinColumn(name = "merchant_id", nullable = false)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Merchant merchant;

    @JsonIgnore
    @JoinColumn(name = "lead_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Lead lead;

    // Factory
    public static Payment of(
            Merchant merchant,
            Lead lead,
            String externalId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String customerName,
            String customerEmail,
            String rawPayload,
            String eventId
    ) {
        if (merchant == null) {
            throw new IllegalArgumentException("merchant is required");
        }
        if (externalId == null || externalId.isBlank()) {
            throw new IllegalArgumentException("externalId is required");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be > 0");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }

        return Payment.builder()
                .merchant(merchant)
                .lead(lead)
                .externalId(externalId.trim())
                .amount(amount)
                .currency(currency.trim().toUpperCase())
                .status(status != null ? status : PaymentStatus.UNKNOWN)
                .customerName(customerName != null ? customerName.trim() : null)
                .customerEmail(normalizeEmail(customerEmail))
                .rawPayload(rawPayload)
                .eventId(eventId)
                .build();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (status == null) {
            status = PaymentStatus.UNKNOWN;
        }
    }

    public void transitionTo(PaymentStatus newStatus) {
        if (newStatus == null) {
            throw new IllegalArgumentException("newStatus is required");
        }

        if (this.status == PaymentStatus.SUCCEEDED && newStatus != PaymentStatus.SUCCEEDED) {
            throw new IllegalStateException("Cannot downgrade a SUCCEEDED payment");
        }

        this.status = newStatus;
    }

    public void markAsNotified() {
        this.notifiedAt = OffsetDateTime.now();
    }

    private static String normalizeEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase();
    }
}
