package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.UserPublicProfile;
import com.mtnrs.revenuesync.domain.enums.ProfileCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserPublicProfileRepository extends JpaRepository<UserPublicProfile, Long> {

    Optional<UserPublicProfile> findByUserId(Long userId);

    Optional<UserPublicProfile> findBySlug(String slug);

    boolean existsBySlug(String slug);

    // Public listing — only visible profiles
    List<UserPublicProfile> findByIsPublicTrue();

    // Filter by category
    List<UserPublicProfile> findByIsPublicTrueAndCategory(ProfileCategory category);

    // Search by display name or headline (case-insensitive)
    @Query("""
        SELECT p FROM UserPublicProfile p
        WHERE p.isPublic = true
          AND (
            LOWER(p.displayName) LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.headline)  LIKE LOWER(CONCAT('%', :q, '%'))
            OR LOWER(p.tags)      LIKE LOWER(CONCAT('%', :q, '%'))
          )
        """)
    List<UserPublicProfile> searchPublic(@Param("q") String query);
}
