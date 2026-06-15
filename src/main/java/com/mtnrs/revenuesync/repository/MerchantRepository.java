package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    Optional<Merchant> findByEmail(String email);

    boolean existsByEmail(String email);

    /*
     * Legacy helper.
     * Avoid using this in new flows because a user can own multiple merchants.
     */
    Optional<Merchant> findByUserId(Long userId);

    List<Merchant> findAllByUserId(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    /*
     * Legacy helper.
     * Wallet address can be shared across multiple merchants.
     * Do not use this to block merchant creation.
     */
    boolean existsByWalletAddress(String walletAddress);

    Optional<Merchant> findBySlug(String slug);

    List<Merchant> findByActiveTrue();

    boolean existsByUserId(Long userId);
}