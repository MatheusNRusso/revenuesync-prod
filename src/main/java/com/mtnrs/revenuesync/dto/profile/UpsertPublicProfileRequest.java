package com.mtnrs.revenuesync.dto.profile;

import com.mtnrs.revenuesync.domain.enums.ProfileCategory;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpsertPublicProfileRequest(

        @Size(max = 255)
        String displayName,

        @Size(max = 255)
        String headline,

        String bio,

        @Size(max = 255)
        String location,

        @Size(max = 500)
        String websiteUrl,

        ProfileCategory category,

        // Max 10 tags, each up to 50 chars
        @Size(max = 10)
        List<@Size(max = 50) String> tags,

        boolean isPublic
) {}
