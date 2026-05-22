package com.mtnrs.revenuesync.dto.admin;

import com.mtnrs.revenuesync.domain.Payment;
import com.mtnrs.revenuesync.domain.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminPaymentResponse(
        Long id,
        Long merchantId,
        String merchantName,
        Long leadId,
        String customerEmail,
        String externalPaymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        OffsetDateTime  createdAt
) {

    public static AdminPaymentResponse from(Payment payment) {
        return new AdminPaymentResponse(
                payment.getId(),
                payment.getMerchant() != null ? payment.getMerchant().getId() : null,
                payment.getMerchant() != null ? payment.getMerchant().getName() : null,
                payment.getLead() != null ? payment.getLead().getId() : null,
                payment.getCustomerEmail(),
                payment.getExternalId(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getCreatedAt()
        );
    }
}