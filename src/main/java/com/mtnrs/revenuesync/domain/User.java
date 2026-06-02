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
     * Set to true once the user completes the onboarding intent screen
     * (chose "I want to consume" or "I want to offer my service").
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

    /**
     * Marks onboarding as complete. Idempotent — safe to call multiple times.
     */
    public void completeOnboarding() {
        this.onboardingCompleted = true;

    }
    /**
     * Soft-deletes the account. Prevents login via Spring Security isEnabled() check.
     * Idempotent — safe to call multiple times.
     */
    public void deactivate() {
        this.active = false;
        this.deletedAt = java.time.LocalDateTime.now();
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

    @Override public String   getUsername()              { return email; }
    @Override public boolean  isAccountNonExpired()      { return true; }
    @Override public boolean  isAccountNonLocked()       { return true; }
    @Override public boolean  isCredentialsNonExpired()  { return true; }
    @Override public boolean  isEnabled()                { return this.active; }
}