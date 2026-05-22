package com.mtnrs.revenuesync.dto.merchant;

import java.math.BigDecimal;

public record CreateMerchantProfileRequest(
        String name,
        String email,
        String description,
        String avatarUrl,
        String walletAddress,
        BigDecimal defaultAmountSol
) {}