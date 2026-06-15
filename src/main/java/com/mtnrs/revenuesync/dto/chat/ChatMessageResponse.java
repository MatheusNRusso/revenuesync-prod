package com.mtnrs.revenuesync.dto.chat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        Long senderId,
        String senderName,
        String content,
        boolean read,
        LocalDateTime createdAt,
        String messageType,
        String paymentToken,
        BigDecimal paymentAmountSol,
        String paymentStatus
) {}
