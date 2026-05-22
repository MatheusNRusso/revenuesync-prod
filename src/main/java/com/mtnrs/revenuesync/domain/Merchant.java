package com.mtnrs.revenuesync.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.Locale;

@Getter
@Entity
@Table(name = "merchants")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
public class Merchant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(unique = true, length = 100)
    private String slug;

    @Column(length = 500)
    private String description;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(name = "wallet_address", length = 64)
    private String walletAddress;

    /**
     * Default payment amount in SOL shown on the public checkout QR.
     * Set by the merchant when creating or editing their service.
     * Null = application falls back to DEFAULT_AMOUNT_SOL in PublicController.
     */
    @Column(name = "default_amount_sol", precision = 19, scale = 9)
    private BigDecimal defaultAmountSol;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Factory ───────────────────────────────────────────────────────────────

    /**
     * Creates a new merchant profile linked to the authenticated user.
     *
     * @param defaultAmountSol Amount in SOL that buyers will be charged on the
     *                         public checkout. Must be > 0 when provided.
     *                         Pass null to use the application-level default.
     */
    public static Merchant createProfile(
            User user,
            String name,
            String email,
            String description,
            String avatarUrl,
            String walletAddress,
            BigDecimal defaultAmountSol
    ) {
        validateUser(user);
        validateName(name);
        validateEmail(email);
        validateWalletAddress(walletAddress);
        validateDefaultAmountSol(defaultAmountSol);

        return Merchant.builder()
                .user(user)
                .name(name.trim())
                .email(normalizeEmail(email))
                .description(normalizeNullable(description))
                .avatarUrl(normalizeNullable(avatarUrl))
                .walletAddress(walletAddress.trim())
                .defaultAmountSol(defaultAmountSol)
                .active(true)
                .slug(generateSlug(name, email))
                .build();
    }

    // ── Mutators ──────────────────────────────────────────────────────────────

    public void updateProfile(
            String name,
            String email,
            String description,
            String avatarUrl
    ) {
        validateName(name);
        validateEmail(email);

        this.name        = name.trim();
        this.email       = normalizeEmail(email);
        this.description = normalizeNullable(description);
        this.avatarUrl   = normalizeNullable(avatarUrl);
        this.slug        = generateSlug(name, email);
    }

    public void updateWalletAddress(String walletAddress) {
        validateWalletAddress(walletAddress);
        this.walletAddress = walletAddress.trim();
    }

    /**
     * Updates the default SOL amount shown on the public checkout QR.
     * Pass null to clear the override and fall back to the application default.
     */
    public void updateDefaultAmountSol(BigDecimal defaultAmountSol) {
        validateDefaultAmountSol(defaultAmountSol);
        this.defaultAmountSol = defaultAmountSol;
    }

    public void deactivate() { this.active = false; }
    public void activate()   { this.active = true;  }

    public boolean belongsTo(User user) {
        return user != null
                && this.user != null
                && this.user.getId() != null
                && this.user.getId().equals(user.getId());
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

    // ── Validation ────────────────────────────────────────────────────────────

    private static void validateUser(User user) {
        if (user == null || user.getId() == null)
            throw new IllegalArgumentException("Authenticated user is required");
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Merchant name is required");
        if (name.trim().length() > 100)
            throw new IllegalArgumentException("Merchant name must have at most 100 characters");
    }

    private static void validateEmail(String email) {
        if (email == null || email.isBlank())
            throw new IllegalArgumentException("Merchant email is required");
        if (email.trim().length() > 100)
            throw new IllegalArgumentException("Merchant email must have at most 100 characters");
        if (!email.contains("@"))
            throw new IllegalArgumentException("Merchant email must be valid");
    }

    private static void validateWalletAddress(String walletAddress) {
        if (walletAddress == null || walletAddress.isBlank())
            throw new IllegalArgumentException("Wallet address is required");
        if (walletAddress.trim().length() > 64)
            throw new IllegalArgumentException("Wallet address must have at most 64 characters");
    }

    /**
     * Allows null (application default will be used) but rejects zero or
     * negative values — a merchant cannot advertise a free or negative-cost service.
     */
    private static void validateDefaultAmountSol(BigDecimal amount) {
        if (amount != null && amount.signum() <= 0)
            throw new IllegalArgumentException("defaultAmountSol must be greater than 0");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeNullable(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    private static String generateSlug(String name, String email) {
        String base = name != null && !name.isBlank()
                ? name
                : email.split("@")[0];

        String normalized = Normalizer.normalize(base, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (normalized.isBlank()) normalized = "merchant";

        return normalized + "-" + System.currentTimeMillis() % 10000;
    }
}