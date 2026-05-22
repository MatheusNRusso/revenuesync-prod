package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.Lead;
import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.domain.Payment;
import com.mtnrs.revenuesync.domain.enums.PaymentStatus;
import com.mtnrs.revenuesync.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;

    @Transactional(readOnly = true)
    public List<Payment> findAll() {
        log.debug("Fetching all payments");
        return paymentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getById(Long id) {
        log.debug("Fetching payment by id: {}", id);
        return paymentRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<Payment> getByExternalId(String externalId) {
        log.debug("Fetching payment by externalId: {}", externalId);
        return paymentRepository.findByExternalId(externalId);
    }

    @Transactional(readOnly = true)
    public boolean existsByExternalId(String externalId) {
        log.debug("Checking existence by externalId: {}", externalId);
        return paymentRepository.existsByExternalId(externalId);
    }

    @Transactional
    public Payment upsertFromStripe(
            Merchant merchant,
            String externalId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String customerName,
            String customerEmail,
            String rawPayload,
            String eventId
    ) {
        return upsertFromStripe(
                merchant,
                null,
                externalId,
                amount,
                currency,
                status,
                customerName,
                customerEmail,
                rawPayload,
                eventId
        );
    }

    @Transactional
    public Payment upsertFromStripe(
            Merchant merchant,
            Lead lead,
            String externalId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String customerName,
            String customerEmail,
            String rawPayload,
            String eventId
    ) {
        log.info("Upserting payment from Stripe: externalId={}", externalId);

        return paymentRepository.findByExternalId(externalId)
                .map(existing -> {
                    log.debug("Updating existing payment: id={}", existing.getId());
                    existing.transitionTo(status);
                    return existing;
                })
                .orElseGet(() -> {
                    log.debug("Creating new payment for externalId: {}", externalId);
                    return paymentRepository.save(
                            Payment.of(
                                    merchant,
                                    lead,
                                    externalId,
                                    amount,
                                    currency,
                                    status,
                                    customerName,
                                    customerEmail,
                                    rawPayload,
                                    eventId
                            )
                    );
                });
    }

    @Transactional
    public Payment upsertFromSolana(
            Merchant merchant,
            String externalId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String customerName,
            String customerEmail,
            String rawPayload,
            String eventId
    ) {
        return upsertFromSolana(
                merchant,
                null,
                externalId,
                amount,
                currency,
                status,
                customerName,
                customerEmail,
                rawPayload,
                eventId
        );
    }

    @Transactional
    public Payment upsertFromSolana(
            Merchant merchant,
            Lead lead,
            String externalId,
            BigDecimal amount,
            String currency,
            PaymentStatus status,
            String customerName,
            String customerEmail,
            String rawPayload,
            String eventId
    ) {
        log.info("Upserting Solana payment: externalId={}", externalId);

        return paymentRepository.findByExternalId(externalId)
                .map(existing -> {
                    log.debug("Updating existing Solana payment: id={}", existing.getId());
                    existing.transitionTo(status);
                    return existing;
                })
                .orElseGet(() -> {
                    log.debug("Creating new Solana payment for externalId: {}", externalId);
                    return paymentRepository.save(
                            Payment.of(
                                    merchant,
                                    lead,
                                    externalId,
                                    amount,
                                    currency,
                                    status,
                                    customerName,
                                    customerEmail,
                                    rawPayload,
                                    eventId
                            )
                    );
                });
    }
}