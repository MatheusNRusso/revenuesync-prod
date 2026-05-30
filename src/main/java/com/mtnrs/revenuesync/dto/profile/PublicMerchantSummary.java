package com.mtnrs.revenuesync.dto.profile;

public record PublicMerchantSummary(
        Long   id,
        String name,
        String slug,
        String description,
        String avatarUrl
) {}
