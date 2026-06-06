package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.ChatMessage;
import com.mtnrs.revenuesync.domain.Conversation;
import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.dto.chat.ChatMessageResponse;
import com.mtnrs.revenuesync.dto.chat.ConversationResponse;
import com.mtnrs.revenuesync.repository.ChatMessageRepository;
import com.mtnrs.revenuesync.repository.ConversationRepository;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final MerchantRepository merchantRepository;

    @Transactional
    public ConversationResponse startOrResumeConversation(Long merchantId, User buyer) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));

        Conversation conversation = conversationRepository
                .findByMerchantIdAndBuyerId(merchantId, buyer.getId())
                .orElseGet(() -> conversationRepository.save(Conversation.start(merchant, buyer)));

        return toConversationResponse(conversation, buyer.getId());
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversationsForUser(User user) {
        List<Conversation> asBuyer = conversationRepository
                .findByBuyerIdOrderByUpdatedAtDesc(user.getId());

        List<Conversation> asMerchantOwner = conversationRepository
                .findByMerchantUserIdOrderByUpdatedAtDesc(user.getId());

        return java.util.stream.Stream.concat(asBuyer.stream(), asMerchantOwner.stream())
                .distinct()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .map(c -> toConversationResponse(c, user.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageResponse> getMessages(Long conversationId, User user) {
        Conversation conversation = findConversation(conversationId);
        validateAccess(conversation, user);

        return chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .map(this::toMessageResponse)
                .toList();
    }

    @Transactional
    public ChatMessageResponse sendMessage(Long conversationId, User sender, String content) {
        Conversation conversation = findConversation(conversationId);
        validateAccess(conversation, sender);

        ChatMessage message = ChatMessage.create(conversation, sender, content);
        chatMessageRepository.save(message);

        return toMessageResponse(message);
    }

    @Transactional
    public void markAsRead(Long conversationId, User user) {
        Conversation conversation = findConversation(conversationId);
        validateAccess(conversation, user);

        chatMessageRepository
                .findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream()
                .filter(m -> !m.getSender().getId().equals(user.getId()))
                .filter(m -> !m.isRead())
                .forEach(ChatMessage::markAsRead);
    }

    @Transactional
    public void closeConversation(Long conversationId, User user) {
        Conversation conversation = findConversation(conversationId);
        validateAccess(conversation, user);
        conversation.close();
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Conversation findConversation(Long id) {
        return conversationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Conversation not found"));
    }

    private void validateAccess(Conversation conversation, User user) {
        if (!conversation.involves(user)) {
            throw new SecurityException("Access denied to this conversation");
        }
    }

    private ConversationResponse toConversationResponse(Conversation c, Long userId) {
        long unread = chatMessageRepository
                .countByConversationIdAndReadFalseAndSenderIdNot(c.getId(), userId);

        return new ConversationResponse(
                c.getId(),
                c.getMerchant().getId(),
                c.getMerchant().getName(),
                c.getMerchant().getSlug(),
                c.getBuyer().getId(),
                c.getBuyer().getName(),
                c.getStatus(),
                unread,
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private ChatMessageResponse toMessageResponse(ChatMessage m) {
        return new ChatMessageResponse(
                m.getId(),
                m.getSender().getId(),
                m.getSender().getName(),
                m.getContent(),
                m.isRead(),
                m.getCreatedAt()
        );
    }
}
