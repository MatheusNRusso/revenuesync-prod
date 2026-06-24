package com.mtnrs.revenuesync.domain;

import com.mtnrs.revenuesync.domain.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserRole role;

    /**
     * Set to true once the user completes the onboarding intent screen.
     * Prevents the screen from reappearing on subsequent logins.
     */
    @Column(name = "onboarding_completed", nullable = false)
    @Builder.Default
    private boolean onboardingCompleted = false;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "email_verified", nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    /**
     * True if this account was created via GitHub OAuth.
     * Used to determine whether the user has set a real password.
     */
    @Column(name = "github_user", nullable = false)
    @Builder.Default
    private boolean githubUser = false;

    @Column(name = "verification_token", length = 6)
    private String verificationToken;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static User of(String name, String email, String password, UserRole role) {
        return User.builder()
                .name(name)
                .email(email.trim().toLowerCase())
                .password(password)
                .role(role)
                .onboardingCompleted(false)
                .build();
    }

    // ── Mutators ──────────────────────────────────────────────────────────────

    public void completeOnboarding() {
        this.onboardingCompleted = true;
    }

    public void deactivate() {
        this.active = false;
        this.deletedAt = LocalDateTime.now();
    }

    public void activate() {
        this.active = true;
        this.deletedAt = null;
    }

    public void markAsGithubUser() {
        this.githubUser = true;
    }

    /**
     * Updates the user's password. Expects an already-encoded value.
     * Also clears the githubUser flag so the user can login with email/password.
     */
    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
        this.githubUser = false;
    }

    /**
     * Returns true if the user has set a real password.
     * GitHub OAuth users have githubUser = true until they set a password.
     */
    public boolean hasPassword() {
        return !this.githubUser;
    }

    public boolean isGithubUser() {
        return this.githubUser;
    }

    public void setVerificationToken(String token) {
        this.verificationToken = token;
        this.tokenExpiresAt = LocalDateTime.now().plusHours(24);
    }

    public boolean verifyEmail(String token) {
        if (token == null || !token.equals(this.verificationToken)) return false;
        if (this.tokenExpiresAt == null || LocalDateTime.now().isAfter(this.tokenExpiresAt)) return false;
        this.emailVerified = true;
        this.verificationToken = null;
        this.tokenExpiresAt = null;
        return true;
    }

    // ── JPA hooks ─────────────────────────────────────────────────────────────

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── UserDetails ───────────────────────────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override public String  getUsername()             { return email; }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return this.active; }
}
