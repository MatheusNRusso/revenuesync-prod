package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.domain.enums.UserRole;
import com.mtnrs.revenuesync.repository.UserPublicProfileRepository;
import com.mtnrs.revenuesync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthService {

    private final UserRepository              userRepository;
    private final JwtService                  jwtService;
    private final PasswordEncoder             passwordEncoder;
    private final PublicProfileService        publicProfileService;
    private final UserPublicProfileRepository publicProfileRepository;

    @Transactional
    public String handleOAuthSuccess(Authentication authentication) {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String login     = oauthUser.getAttribute("login");
        String email     = oauthUser.getAttribute("email");
        String avatarUrl = oauthUser.getAttribute("avatar_url");
        String htmlUrl   = oauthUser.getAttribute("html_url");
        Integer repos    = oauthUser.getAttribute("public_repos");
        Integer followers= oauthUser.getAttribute("followers");

        if (login == null || login.isBlank()) login = "github-user";
        if (email == null || email.isBlank()) email = login + "@github.oauth";

        final String finalEmail = email.toLowerCase().trim();
        final String finalLogin = login.trim();

        // ── Upsert user ───────────────────────────────────────────────────────
        // 1. Try to find by githubUsername (handles email change scenario)
        // 2. Fallback to email lookup
        // 3. Create new user if not found
        User user = publicProfileRepository.findByGithubUsername(finalLogin)
                .map(profile -> profile.getUser())
                .orElseGet(() -> userRepository.findByEmail(finalEmail)
                        .orElseGet(() -> {
                            User newUser = User.of(
                                    finalLogin,
                                    finalEmail,
                                    passwordEncoder.encode(UUID.randomUUID().toString()),
                                    UserRole.USER
                            );
                            newUser.markAsGithubUser();
                            return userRepository.save(newUser);
                        }));

        // Mark existing users as GitHub users if not already marked
        if (!user.isGithubUser()) {
            user.markAsGithubUser();
            userRepository.save(user);
        }

        // ── Sync GitHub public profile data ───────────────────────────────────
        publicProfileService.syncGitHubData(
                user,
                finalLogin,
                avatarUrl,
                htmlUrl,
                repos     != null ? repos     : 0,
                followers != null ? followers : 0
        );

        log.info("GitHub OAuth login successful: {} (github={})", finalEmail, finalLogin);
        return jwtService.generateToken(user);
    }
}
