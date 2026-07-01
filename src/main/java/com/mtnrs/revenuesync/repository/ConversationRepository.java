package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByMerchantIdAndBuyerId(Long merchantId, Long buyerId);

    List<Conversation> findByBuyerIdOrderByUpdatedAtDesc(Long buyerId);

    List<Conversation> findByMerchantUserIdOrderByUpdatedAtDesc(Long userId);

    @Query("SELECT c FROM Conversation c WHERE c.merchant.id = :merchantId AND c.buyer.email = :email")
    Optional<Conversation> findByMerchantIdAndBuyerEmail(
            @Param("merchantId") Long merchantId,
            @Param("email") String email);
}
