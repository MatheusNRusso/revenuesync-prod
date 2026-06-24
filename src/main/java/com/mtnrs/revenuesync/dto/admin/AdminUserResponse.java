package com.mtnrs.revenuesync.dto.admin;

import com.mtnrs.revenuesync.domain.User;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active,
        boolean githubUser,
        LocalDateTime createdAt
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isEnabled(),
                user.isGithubUser(),
                user.getCreatedAt()
        );
    }
}
