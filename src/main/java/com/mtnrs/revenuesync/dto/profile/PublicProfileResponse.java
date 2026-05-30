package com.mtnrs.revenuesync.dto.profile;

import com.mtnrs.revenuesync.domain.enums.ProfileCategory;

import java.util.List;

public record PublicProfileResponse(
        Long                       id,
        String                     slug,
        String                     displayName,
        String                     headline,
        String                     bio,
        String                     location,
        String                     websiteUrl,
        ProfileCategory            category,
        List<String>               tags,
        String                     githubUsername,
        String                     githubAvatarUrl,
        String                     githubProfileUrl,
        Integer                    githubPublicRepos,
        Integer                    githubFollowers,
        boolean                    isPublic,
        List<PublicMerchantSummary> merchants
) {}
