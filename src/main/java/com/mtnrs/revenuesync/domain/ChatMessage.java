package com.mtnrs.revenuesync.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

import com.mtnrs.revenuesync.domain.enums.MessageType;

@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PRIVATE)
@EqualsAndHashCode(of = "id")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "message_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageType messageType;

    @Column(name = "payment_token", length = 100)
    private String paymentToken;

    @Column(name = "payment_amount_sol", precision = 19, scale = 9)
    private java.math.BigDecimal paymentAmountSol;

    @Column(name = "payment_status", length = 20)
    private String paymentStatus;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Factory ───────────────────────────────────────────────────────────────
    public static ChatMessage create(Conversation conversation, User sender, String content) {
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation is required");
        }
        if (sender == null) {
            throw new IllegalArgumentException("Sender is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("Message content is required");
        }
        if (!conversation.isActive()) {
            throw new IllegalStateException("Cannot send message to a closed conversation");
        }

        return ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content(content.trim())
                .messageType(MessageType.TEXT)
                .read(false)
                .build();
    }

    public static ChatMessage paymentRequest(
            Conversation conversation, User sender,
            java.math.BigDecimal amountSol, String token) {
        if (conversation == null) {
            throw new IllegalArgumentException("Conversation is required");
        }
        if (!conversation.isActive()) {
            throw new IllegalStateException("Conversation is closed");
        }
        return ChatMessage.builder()
                .conversation(conversation)
                .sender(sender)
                .content("Payment request: ◎ " + amountSol + " SOL")
                .messageType(MessageType.PAYMENT_REQUEST)
                .paymentToken(token)
                .paymentAmountSol(amountSol)
                .paymentStatus("PENDING")
                .read(false)
                .build();
    }

    // ── Mutators ──────────────────────────────────────────────────────────────
    public void markAsRead() {
        this.read = true;
    }

    // ── JPA hooks ─────────────────────────────────────────────────────────────
    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
