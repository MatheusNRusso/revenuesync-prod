package com.mtnrs.revenuesync.repository;

import com.mtnrs.revenuesync.domain.Conversion;
import com.mtnrs.revenuesync.domain.enums.ConversionPlatform;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversionRepository extends JpaRepository<Conversion, Long> {

    /**
     * Finds all conversions ordered by creation date (newest first).
     */
    List<Conversion> findAllByOrderByCreatedAtDesc();

    /**
     * Finds conversions by payment ID.
     */
    List<Conversion> findByPaymentIdOrderByCreatedAtDesc(Long paymentId);

    /**
     * Finds conversions by platform enum value.
     * Exact match only - no IgnoreCase (not supported with Enum types).
     *
     * @param platform the ConversionPlatform enum value (META or GOOGLE)
     * @return list of conversions for the specified platform
     */
    List<Conversion> findByPlatformOrderByCreatedAtDesc(ConversionPlatform platform);
}