package com.mtnrs.revenuesync.mapper;

import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.domain.UserPublicProfile;
import com.mtnrs.revenuesync.dto.profile.PublicMerchantSummary;
import com.mtnrs.revenuesync.dto.profile.PublicProfileResponse;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class PublicProfileMapper {

    public PublicProfileResponse toResponse(UserPublicProfile profile, List<Merchant> merchants) {
        return new PublicProfileResponse(
                profile.getId(),
                profile.getSlug(),
                profile.getDisplayName(),
                profile.getHeadline(),
                profile.getBio(),
                profile.getLocation(),
                profile.getWebsiteUrl(),
                profile.getCategory(),
                parseTags(profile.getTags()),
                profile.getGithubUsername(),
                profile.getGithubAvatarUrl(),
                profile.getGithubProfileUrl(),
                profile.getGithubPublicRepos(),
                profile.getGithubFollowers(),
                profile.isPublic(),
                toMerchantSummaries(merchants)
        );
    }

    // Overload without merchants list — keeps backward compatibility
    public PublicProfileResponse toResponse(UserPublicProfile profile) {
        return toResponse(profile, Collections.emptyList());
    }

    // ── Merchant helpers ──────────────────────────────────────────────────────

    private List<PublicMerchantSummary> toMerchantSummaries(List<Merchant> merchants) {
        if (merchants == null || merchants.isEmpty()) return Collections.emptyList();
        return merchants.stream()
                .filter(Merchant::isActive)
                .map(m -> new PublicMerchantSummary(
                        m.getId(),
                        m.getName(),
                        m.getSlug(),
                        m.getDescription() != null ? m.getDescription() : "",
                        m.getAvatarUrl()   != null ? m.getAvatarUrl()   : ""
                ))
                .toList();
    }

    // ── Tag helpers ───────────────────────────────────────────────────────────

    public static List<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) return Collections.emptyList();
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    public static String serializeTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return null;
        return String.join(",", tags);
    }
}
