package com.mtnrs.revenuesync.dto.admin;

import com.mtnrs.revenuesync.domain.Conversion;
import com.mtnrs.revenuesync.domain.enums.ConversionPlatform;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record AdminConversionResponse(
        Long id,
        Long paymentId,
        Long merchantId,
        Long leadId,
        ConversionPlatform platform,
        BigDecimal value,
        String currency,
        OffsetDateTime createdAt
) {

    public static AdminConversionResponse from(Conversion conversion) {
        return new AdminConversionResponse(
                conversion.getId(),
                conversion.getPaymentId(),
                conversion.getMerchantId(),
                conversion.getLeadId(),
                conversion.getPlatform(),
                conversion.getValue(),
                conversion.getCurrency(),
                conversion.getCreatedAt()
        );
    }
}