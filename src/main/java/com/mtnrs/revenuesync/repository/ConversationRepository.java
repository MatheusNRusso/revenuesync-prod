package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByMerchantIdAndBuyerId(Long merchantId, Long buyerId);

    List<Conversation> findByBuyerIdOrderByUpdatedAtDesc(Long buyerId);

    List<Conversation> findByMerchantUserIdOrderByUpdatedAtDesc(Long userId);
}
