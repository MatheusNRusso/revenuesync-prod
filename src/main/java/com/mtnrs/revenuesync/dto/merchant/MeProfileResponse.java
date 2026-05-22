package com.mtnrs.revenuesync.dto.merchant;

import java.util.List;

public record MeProfileResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean hasMerchants,
        boolean onboardingCompleted,
        List<MerchantProfileResponse> merchants
) {}