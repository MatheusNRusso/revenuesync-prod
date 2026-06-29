package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    long countByConversationIdAndReadFalseAndSenderIdNot(Long conversationId, Long userId);

    long countByConversationId(long conversationId);
}
