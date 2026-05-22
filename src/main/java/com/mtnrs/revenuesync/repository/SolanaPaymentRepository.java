package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.SolanaPayment;
import com.mtnrs.revenuesync.domain.enums.SolanaPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SolanaPaymentRepository extends JpaRepository<SolanaPayment, Long> {

    Optional<SolanaPayment> findByReference(String reference);

    /** Returns active pending payments not yet expired — consumed by the verification job */
    @Query("""
        SELECT s FROM SolanaPayment s
        WHERE s.status = :status
          AND s.expiresAt > :now
        ORDER BY s.createdAt ASC
    """)
    List<SolanaPayment> findActiveByStatus(SolanaPaymentStatus status, OffsetDateTime now);

    /** Returns pending payments that have already expired — used for cleanup */
    @Query("""
        SELECT s FROM SolanaPayment s
        WHERE s.status = 'PENDING'
          AND s.expiresAt <= :now
    """)
    List<SolanaPayment> findExpired(OffsetDateTime now);

    List<SolanaPayment> findByMerchantIdOrderByCreatedAtDesc(Long merchantId);

    /**
     * Returns all payments initiated by a buyer identified by email,
     * ordered most-recent first. Used by GET /api/me/purchases to build
     * the buyer's consumption history.
     */
    List<SolanaPayment> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
}