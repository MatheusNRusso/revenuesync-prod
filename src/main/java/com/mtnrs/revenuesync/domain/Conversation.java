package com.mtnrs.revenuesync.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "conversations")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "archived_by_buyer", nullable = false)
    private boolean archivedByBuyer;

    @Column(name = "archived_by_merchant", nullable = false)
    private boolean archivedByMerchant;

    @Column(name = "deleted_by_buyer", nullable = false)
    private boolean deletedByBuyer;

    @Column(name = "deleted_by_merchant", nullable = false)
    private boolean deletedByMerchant;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Factory ───────────────────────────────────────────────────────────────

    public static Conversation start(Merchant merchant, User buyer) {
        if (merchant == null) throw new IllegalArgumentException("Merchant is required");
        if (buyer == null) throw new IllegalArgumentException("Buyer is required");

        return Conversation.builder()
                .merchant(merchant)
                .buyer(buyer)
                .status("ACTIVE")
                .build();
    }

    // ── Mutators ──────────────────────────────────────────────────────────────

    public void close()   { this.status = "CLOSED";   }
    public void archive() { this.status = "ARCHIVED"; }
    public void reopen() { this.status = "ACTIVE"; }

    public boolean isActive() { return "ACTIVE".equals(this.status); }

    public boolean involves(User user) {
        if (user == null || user.getId() == null) return false;
        Long uid = user.getId();
        return uid.equals(buyer.getId())
                || (merchant.getUser() != null && uid.equals(merchant.getUser().getId()));
    }

    // ── Side-specific organization (WhatsApp model) ─────────────────────────

    private boolean isMerchantSide(User user) {
        return merchant.getUser() != null
                && user.getId().equals(merchant.getUser().getId());
    }

    public void archiveFor(User user) {
        if (isMerchantSide(user)) this.archivedByMerchant = true;
        else                      this.archivedByBuyer = true;
    }

    public void unarchiveFor(User user) {
        if (isMerchantSide(user)) this.archivedByMerchant = false;
        else                      this.archivedByBuyer = false;
    }

    public void deleteFor(User user) {
        if (isMerchantSide(user)) this.deletedByMerchant = true;
        else                      this.deletedByBuyer = true;
    }

    /** New message resurrects the conversation for both sides (WhatsApp behavior). */
    public void resurrectOnNewMessage() {
        this.deletedByBuyer = false;
        this.deletedByMerchant = false;
        this.archivedByBuyer = false;
        this.archivedByMerchant = false;
    }

    public boolean isVisibleFor(User user) {
        return isMerchantSide(user) ? !deletedByMerchant : !deletedByBuyer;
    }

    public boolean isArchivedFor(User user) {
        return isMerchantSide(user) ? archivedByMerchant : archivedByBuyer;
    }

    // ── JPA hooks ─────────────────────────────────────────────────────────────

    @PrePersist
    void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void preUpdate() { updatedAt = LocalDateTime.now(); }
}
