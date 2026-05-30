package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.domain.UserPublicProfile;
import com.mtnrs.revenuesync.domain.enums.ProfileCategory;
import com.mtnrs.revenuesync.dto.profile.PublicProfileResponse;
import com.mtnrs.revenuesync.dto.profile.UpsertPublicProfileRequest;
import com.mtnrs.revenuesync.mapper.PublicProfileMapper;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.repository.UserPublicProfileRepository;
import com.mtnrs.revenuesync.util.SlugUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicProfileService {

    private final UserPublicProfileRepository repository;
    private final MerchantRepository          merchantRepository;
    private final PublicProfileMapper         mapper;

    // ── Public listing ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PublicProfileResponse> listPublic(String category, String search) {
        List<UserPublicProfile> profiles;

        if (search != null && !search.isBlank()) {
            profiles = repository.searchPublic(search.trim());
        } else if (category != null && !category.isBlank()) {
            ProfileCategory cat = ProfileCategory.valueOf(category.toUpperCase());
            profiles = repository.findByIsPublicTrueAndCategory(cat);
        } else {
            profiles = repository.findByIsPublicTrue();
        }

        return profiles.stream()
                .map(p -> mapper.toResponse(p, merchantRepository.findAllByUserId(p.getUser().getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public PublicProfileResponse getBySlug(String slug) {
        UserPublicProfile profile = repository.findBySlug(slug)
                .filter(UserPublicProfile::isPublic)
                .orElseThrow(() -> new RuntimeException("Profile not found: " + slug));

        return mapper.toResponse(profile, merchantRepository.findAllByUserId(profile.getUser().getId()));
    }

    // ── Authenticated user ────────────────────────────────────────────────────

    @Transactional
    public PublicProfileResponse getMyProfile(User user) {
        UserPublicProfile profile = repository.findByUserId(user.getId())
                .orElseGet(() -> {
                    String slug = generateUniqueSlug(user.getName());
                    log.info("Auto-creating public profile for user={} slug={}", user.getId(), slug);
                    return repository.save(UserPublicProfile.create(user, slug));
                });

        return mapper.toResponse(profile, merchantRepository.findAllByUserId(user.getId()));
    }

    @Transactional
    public PublicProfileResponse upsert(User user, UpsertPublicProfileRequest request) {
        UserPublicProfile profile = repository.findByUserId(user.getId())
                .orElseGet(() -> {
                    String slug = generateUniqueSlug(user.getName());
                    log.info("Creating new public profile for user={} slug={}", user.getId(), slug);
                    return repository.save(UserPublicProfile.create(user, slug));
                });

        profile.updateProfile(
                request.displayName(),
                request.headline(),
                request.bio(),
                request.location(),
                request.websiteUrl(),
                request.category(),
                PublicProfileMapper.serializeTags(request.tags())
        );
        profile.setPublic(request.isPublic());

        return mapper.toResponse(repository.save(profile), merchantRepository.findAllByUserId(user.getId()));
    }

    @Transactional
    public PublicProfileResponse setVisibility(User user, boolean isPublic) {
        UserPublicProfile profile = repository.findByUserId(user.getId())
                .orElseThrow(() -> new RuntimeException("Profile not found for user: " + user.getId()));
        profile.setPublic(isPublic);
        return mapper.toResponse(repository.save(profile), merchantRepository.findAllByUserId(user.getId()));
    }

    // ── GitHub sync ───────────────────────────────────────────────────────────

    @Transactional
    public void syncGitHubData(User user, String username, String avatarUrl,
                               String profileUrl, Integer publicRepos, Integer followers) {
        UserPublicProfile profile = repository.findByUserId(user.getId())
                .orElseGet(() -> {
                    String slug = generateUniqueSlug(username != null ? username : user.getName());
                    return repository.save(UserPublicProfile.create(user, slug));
                });

        profile.syncGitHub(username, avatarUrl, profileUrl, publicRepos, followers);

        if (profile.getDisplayName() == null && username != null) {
            profile.updateProfile(
                    username,
                    profile.getHeadline(),
                    profile.getBio(),
                    profile.getLocation(),
                    profile.getWebsiteUrl(),
                    profile.getCategory(),
                    profile.getTags()
            );
        }

        repository.save(profile);
        log.info("GitHub data synced for user={} github={}", user.getId(), username);
    }

    // ── Slug generation ───────────────────────────────────────────────────────

    private String generateUniqueSlug(String name) {
        String base = SlugUtils.slugify(name);
        if (!repository.existsBySlug(base)) return base;

        long suffix = 2;
        String candidate;
        do {
            candidate = SlugUtils.slugifyWithSuffix(name, suffix++);
        } while (repository.existsBySlug(candidate));

        return candidate;
    }
}
