package com.mtnrs.revenuesync.dto.admin;

import java.math.BigDecimal;
import java.util.List;

public record AdminDashboardResponse(
        long totalUsers,
        long totalMerchants,
        long totalLeads,
        long totalPayments,
        BigDecimal totalRevenueSol,
        long totalConversions,
        long metaConversions,
        long googleConversions,
        List<AdminPaymentResponse> latestPayments,
        List<AdminLeadResponse> latestLeads,
        List<AdminMerchantResponse> latestMerchants,
        List<AdminConversionResponse> latestConversions
) {
}