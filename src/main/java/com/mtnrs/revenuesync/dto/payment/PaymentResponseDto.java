package com.mtnrs.revenuesync.dto.payment;

import com.mtnrs.revenuesync.domain.enums.PaymentStatus;

import java.math.BigDecimal;

public record PaymentResponseDto(
        Long id,
        String externalPaymentId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String customerEmail,
        String customerName,
        String createdAt
) {}
