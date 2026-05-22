package com.mtnrs.revenuesync.dto.conversion;

import java.math.BigDecimal;

public record PurchaseConversionDto(
        String eventId,
        String externalPaymentId,
        BigDecimal value,
        String currency,
        String customerEmail,
        String clickId,
        String fbc
) {}
