package com.mtnrs.revenuesync.domain;

import com.mtnrs.revenuesync.domain.enums.ProfileCategory;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_public_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
public class UserPublicProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(name = "display_name", length = 255)
    private String displayName;

    @Column(length = 255)
    private String headline;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 255)
    private String location;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 100)
    private ProfileCategory category;

    // Comma-separated list — e.g. "Java,Spring Boot,PostgreSQL"
    // Serialized/deserialized by PublicProfileMapper
    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(name = "github_username", length = 255)
    private String githubUsername;

    @Column(name = "github_avatar_url", length = 500)
    private String githubAvatarUrl;

    @Column(name = "github_profile_url", length = 500)
    private String githubProfileUrl;

    @Column(name = "github_public_repos")
    private Integer githubPublicRepos;

    @Column(name = "github_followers")
    private Integer githubFollowers;

    @Column(name = "is_public", nullable = false)
    private boolean isPublic;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static UserPublicProfile create(User user, String slug) {
        return UserPublicProfile.builder()
                .user(user)
                .slug(slug)
                .isPublic(false)
                .build();
    }

    // ── Mutators ──────────────────────────────────────────────────────────────

    public void updateProfile(String displayName, String headline, String bio,
                              String location, String websiteUrl,
                              ProfileCategory category, String tags) {
        this.displayName = displayName;
        this.headline    = headline;
        this.bio         = bio;
        this.location    = location;
        this.websiteUrl  = websiteUrl;
        this.category    = category;
        this.tags        = tags;
    }

    public void syncGitHub(String username, String avatarUrl, String profileUrl,
                           Integer publicRepos, Integer followers) {
        this.githubUsername    = username;
        this.githubAvatarUrl   = avatarUrl;
        this.githubProfileUrl  = profileUrl;
        this.githubPublicRepos = publicRepos;
        this.githubFollowers   = followers;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    // ── JPA hooks ─────────────────────────────────────────────────────────────

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
