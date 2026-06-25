package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.domain.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByExternalId(String externalId);

    boolean existsByExternalId(String externalId);

    long countByMerchant(Merchant merchant);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.merchant = :merchant AND p.status = 'SUCCEEDED'")
    BigDecimal sumSucceededAmountByMerchant(Merchant merchant);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.merchant = :merchant AND p.status = 'SUCCEEDED' AND p.currency = :currency")
    BigDecimal sumSucceededAmountByMerchantAndCurrency(@Param("merchant") Merchant merchant, @Param("currency") String currency);

    Page<Payment> findByMerchant(Merchant merchant, Pageable pageable);

    Page<Payment> findByMerchantIn(List<Merchant> merchants, Pageable pageable);

    void deleteAllByMerchant(Merchant merchant);
}