package com.mtnrs.revenuesync.domain;

import com.mtnrs.revenuesync.domain.enums.LeadSource;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Getter
@Entity
@Table(
        name = "leads",
        indexes = {
                @Index(name = "idx_leads_email", columnList = "email"),
                @Index(name = "idx_leads_created_at", columnList = "createdAt")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
public class Lead {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String email;

    @Column(length = 180)
    private String name;

    @Column(name = "wallet_address", unique = true, length = 64)
    private String walletAddress;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(length = 60)
    private LeadSource source;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public static Lead of(
            String email,
            String name,
            LeadSource source,
            String rawPayload) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be null or blank");
        }
        return Lead.builder()
                .email(email.trim().toLowerCase())
                .name(name)
                .source(source != null ? source : LeadSource.UNKNOWN)
                .rawPayload(rawPayload)
                .build();
    }

    public static Lead walletOnly(String walletAddress, String name) {
        if (walletAddress == null || walletAddress.isBlank()) {
            throw new IllegalArgumentException("walletAddress must not be null or blank");
        }
        return Lead.builder()
                .email(("wallet+" + walletAddress.trim().toLowerCase() + "@lead.local"))
                .name(name)
                .walletAddress(walletAddress.trim())
                .source(LeadSource.UNKNOWN)
                .build();
    }

    public void assignUser(User user) { this.user = user; }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}