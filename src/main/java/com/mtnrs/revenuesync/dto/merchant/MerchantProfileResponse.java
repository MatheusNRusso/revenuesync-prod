package com.mtnrs.revenuesync.dto.merchant;

public record MerchantProfileResponse(
        Long id,
        String name,
        String email,
        String slug,
        String description,
        String avatarUrl,
        String walletAddress,
        boolean active
) {
}