package com.mtnrs.revenuesync.dto.chat;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long merchantId,
        String merchantName,
        String merchantSlug,
        Long buyerId,
        String buyerName,
        String status,
        long unreadCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
