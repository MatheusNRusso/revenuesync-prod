package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.domain.enums.UserRole;
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

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public String handleOAuthSuccess(Authentication authentication) {
        log.info("OAuth2 handleOAuthSuccess called");
        try {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            String email = oauthUser.getAttribute("email");
            String name = oauthUser.getAttribute("login");

            if (name == null || name.isBlank()) {
                name = "github-user";
            }
            if (email == null || email.isBlank()) {
                email = name + "@github.oauth";
            }

            final String finalEmail = email.toLowerCase().trim();
            final String finalName = name.trim();

            User user = userRepository.findByEmail(finalEmail)
                    .orElseGet(() -> userRepository.save(
                            User.of(
                                    finalName,
                                    finalEmail,
                                    passwordEncoder.encode(UUID.randomUUID().toString()),
                                    UserRole.USER
                            )
                    ));

            log.info("GitHub OAuth login successful: {}", finalEmail);
            return jwtService.generateToken(user);
        } catch (Exception e) {
            log.error("OAuth2 handleOAuthSuccess failed: {}", e.getMessage(), e);
            throw e;
        }
    }
}
