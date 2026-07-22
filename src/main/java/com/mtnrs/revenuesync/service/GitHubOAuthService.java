package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.domain.UserPublicProfile;
import com.mtnrs.revenuesync.domain.enums.UserRole;
import com.mtnrs.revenuesync.repository.UserPublicProfileRepository;
import com.mtnrs.revenuesync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class GitHubOAuthService {

    private static final String SYNTHETIC_EMAIL_SUFFIX = "@github.oauth";

    private final UserRepository              userRepository;
    private final JwtService                  jwtService;
    private final PasswordEncoder             passwordEncoder;
    private final PublicProfileService        publicProfileService;
    private final UserPublicProfileRepository publicProfileRepository;
    private final OAuth2AuthorizedClientService authorizedClientService;

    // Dedicated client for the GitHub API with bounded timeouts so a slow
    // upstream can never hang the login flow.
    private final RestClient gitHubClient = RestClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Accept", "application/vnd.github+json")
            .requestFactory(timeoutRequestFactory())
            .build();

    @Transactional
    public String handleOAuthSuccess(Authentication authentication) {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String login      = oauthUser.getAttribute("login");
        String avatarUrl  = oauthUser.getAttribute("avatar_url");
        String htmlUrl    = oauthUser.getAttribute("html_url");
        Integer repos     = oauthUser.getAttribute("public_repos");
        Integer followers = oauthUser.getAttribute("followers");

        if (login == null || login.isBlank()) login = "github-user";
        final String finalLogin = login.trim();

        // The plain "email" attribute (from GET /user) is null when the user
        // keeps their email private. The verified email is only available via
        // the dedicated GET /user/emails endpoint, which the user:email scope
        // authorizes. We resolve it here with a safe fallback chain.
        final String finalEmail = resolveVerifiedEmail(authentication, oauthUser, finalLogin)
                .toLowerCase().trim();

        // ── Upsert user ───────────────────────────────────────────────────────
        // 1. Find by githubUsername (stable identity, survives email changes)
        // 2. Fallback to email lookup
        // 3. Create new user if not found
        User user = publicProfileRepository.findByGithubUsername(finalLogin)
                .map(UserPublicProfile::getUser)
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

        // ── Apply identity changes in a single save ───────────────────────────
        boolean dirty = false;
        if (!user.isGithubUser()) {
            user.markAsGithubUser();
            dirty = true;
        }
        if (reconcileLegacyEmail(user, finalEmail, finalLogin)) {
            dirty = true;
        }
        if (dirty) {
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

        log.info("GitHub OAuth login successful: user={} github={}", user.getId(), finalLogin);
        return jwtService.generateToken(user);
    }

    // ── Email resolution ──────────────────────────────────────────────────────

    /**
     * Resolves the user's email with a 3-level fallback:
     *   1. primary + verified email from GET /user/emails
     *   2. any verified email from GET /user/emails
     *   3. the "email" attribute from the OAuth payload (null when private)
     *   4. synthetic "<login>@github.oauth" (last resort, never blocks login)
     */
    private String resolveVerifiedEmail(Authentication authentication, OAuth2User oauthUser, String login) {
        String accessToken = extractAccessToken(authentication);
        List<GitHubEmail> emails = fetchGitHubEmails(accessToken);

        if (!emails.isEmpty()) {
            var primaryVerified = emails.stream()
                    .filter(e -> e.primary() && e.verified())
                    .map(GitHubEmail::email)
                    .findFirst();
            if (primaryVerified.isPresent()) {
                log.info("GitHub email resolved: source=primary_verified login={}", login);
                return primaryVerified.get();
            }

            var verified = emails.stream()
                    .filter(GitHubEmail::verified)
                    .map(GitHubEmail::email)
                    .findFirst();
            if (verified.isPresent()) {
                log.info("GitHub email resolved: source=verified login={}", login);
                return verified.get();
            }
        }

        // Fallback to the OAuth payload attribute (may be null when private).
        String payloadEmail = oauthUser.getAttribute("email");
        if (payloadEmail != null && !payloadEmail.isBlank()) {
            log.info("GitHub email resolved: source=payload login={}", login);
            return payloadEmail;
        }

        log.warn("GitHub email not resolvable; using synthetic fallback. login={}", login);
        return login + SYNTHETIC_EMAIL_SUFFIX;
    }

    /**
     * Migrates a legacy synthetic email ("<login>@github.oauth") to the real
     * verified email. Idempotent and safe: never overwrites a real email and
     * never collides with an email already owned by another account.
     */
    private boolean reconcileLegacyEmail(User user, String verifiedEmail, String githubLogin) {
        String current = user.getEmail();
        if (current == null || !current.endsWith(SYNTHETIC_EMAIL_SUFFIX)) {
            return false; // not a legacy synthetic account
        }
        if (verifiedEmail.equalsIgnoreCase(current)) {
            return false;
        }
        if (verifiedEmail.endsWith(SYNTHETIC_EMAIL_SUFFIX)) {
            return false; // resolved email is also synthetic; nothing to reconcile
        }
        if (userRepository.existsByEmail(verifiedEmail)) {
            log.warn("Skip email reconciliation: verified email already owned by another user. user={} github={}",
                    user.getId(), githubLogin);
            return false;
        }

        user.updateEmail(verifiedEmail);
        log.info("Reconciled legacy synthetic email to verified email. user={} github={}",
                user.getId(), githubLogin);
        return true;
    }

    // ── GitHub API access ─────────────────────────────────────────────────────

    private String extractAccessToken(Authentication authentication) {
        if (!(authentication instanceof OAuth2AuthenticationToken oauthToken)) {
            return null;
        }
        OAuth2AuthorizedClient client = authorizedClientService.loadAuthorizedClient(
                oauthToken.getAuthorizedClientRegistrationId(),
                authentication.getName());
        if (client == null || client.getAccessToken() == null) {
            return null;
        }
        return client.getAccessToken().getTokenValue();
    }

    private List<GitHubEmail> fetchGitHubEmails(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return List.of();
        }
        try {
            GitHubEmail[] emails = gitHubClient.get()
                    .uri("/user/emails")
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(GitHubEmail[].class);
            if (emails == null) return List.of();
            return Arrays.stream(emails).filter(Objects::nonNull).toList();
        } catch (Exception ex) {
            // Never let an upstream failure break the login flow.
            log.warn("Failed to fetch GitHub emails: {}", ex.getMessage());
            return List.of();
        }
    }

    private static ClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(5));
        return factory;
    }

    // ── GitHub /user/emails payload ───────────────────────────────────────────

    private record GitHubEmail(String email, boolean primary, boolean verified, String visibility) {}
}