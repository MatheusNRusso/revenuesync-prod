package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.Lead;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    java.util.Optional<Lead> findByUserId(Long userId);
    java.util.Optional<Lead> findByEmail(String email);
    java.util.Optional<Lead> findByWalletAddress(String walletAddress);
}