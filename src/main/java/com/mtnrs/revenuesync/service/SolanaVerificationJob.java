package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.domain.SolanaPayment;
import com.mtnrs.revenuesync.domain.enums.LeadSource;
import com.mtnrs.revenuesync.domain.enums.PaymentStatus;
import com.mtnrs.revenuesync.domain.enums.SolanaPaymentStatus;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.repository.SolanaPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.mtnrs.revenuesync.repository.LeadRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Scheduled job responsible for: 1. Scanning PENDING Solana payments and
 * verifying them on-chain 2. On confirmation: triggering the existing pipeline
 * (Payment → Chat → Conversion → Lead) 3. Expiring timed-out payments
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SolanaVerificationJob {

    private final SolanaPayService solanaPayService;
    private final SolanaPaymentRepository solanaPaymentRepository;
    private final PaymentService paymentService;
    private final ConversionService conversionService;
    private final LeadService leadService;
    private final MerchantRepository merchantRepository;
    private final LeadRepository leadRepository;
    private final ChatService chatService;

    // ── Verify pending payments ───────────────────────────────────────────────
    @Scheduled(fixedDelayString = "${solana.verification-interval-ms:5000}")
    public void verifyPendingPayments() {
        List<SolanaPayment> pending = solanaPayService.findPendingActive();

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Solana verification job: {} pending payment(s) to check", pending.size());

        for (SolanaPayment solanaPayment : pending) {
            try {
                verifyAndProcess(solanaPayment);
            } catch (Exception e) {
                log.error("Error processing Solana payment reference={}: {}",
                        solanaPayment.getReference(), e.getMessage());
            }
        }
    }

    @Transactional
    protected void verifyAndProcess(SolanaPayment solanaPayment) {
        Optional<String> txSignature = solanaPayService.verifyOnChain(solanaPayment.getReference());

        if (txSignature.isEmpty()) {
            log.trace("No confirmed tx yet for reference={}", solanaPayment.getReference());
            return;
        }

        // Guard: duplicate on-chain payment — reference already CONFIRMED
        if (solanaPayment.getStatus() == SolanaPaymentStatus.CONFIRMED) {
            log.warn("Duplicate on-chain payment detected — reference={} already CONFIRMED. Ignoring tx={}",
                    solanaPayment.getReference(), txSignature.get());
            return;
        }

        log.info("Solana payment confirmed — reference={} tx={}",
                solanaPayment.getReference(), txSignature.get());

        // 1. Upsert into the existing PaymentService (idempotent)
        Merchant merchant = merchantRepository.findById(solanaPayment.getMerchantId())
                .orElseThrow(() -> new IllegalStateException(
                "Merchant not found: " + solanaPayment.getMerchantId()));

        var lead = solanaPayment.getLeadId() != null
                ? leadRepository.findById(solanaPayment.getLeadId()).orElse(null)
                : null;

        var payment = paymentService.upsertFromSolana(
                merchant,
                lead,
                "solana:" + txSignature.get(), // unique externalId
                solanaPayment.getAmount(),
                solanaPayment.getCurrency(),
                PaymentStatus.SUCCEEDED,
                null,
                solanaPayment.getCustomerEmail(),
                buildRawPayload(solanaPayment, txSignature.get()),
                txSignature.get()
        );

        // 2. Mark the SolanaPayment as confirmed
        solanaPayment.confirm(txSignature.get(), payment.getId());
        solanaPaymentRepository.save(solanaPayment);

        // 3. Notify chat conversation (highest-priority side-effect — user-facing)
        try {
            chatService.sendPaymentConfirmedMessage(
                    solanaPayment.getMerchantId(),
                    solanaPayment.getCustomerEmail(),
                    solanaPayment.getAmount(),
                    txSignature.get()
            );
        } catch (Exception e) {
            log.error("Failed to send chat notification for payment id={}: {}",
                    payment.getId(), e.getMessage());
        }

        // 4. Dispatch conversions to Meta CAPI and Google Ads (external tracking — non-critical)
        try {
            String metaJson = buildMetaJson(solanaPayment, txSignature.get());
            String googleJson = buildGoogleJson(solanaPayment, txSignature.get());

            conversionService.sendToMeta(payment.getId(), solanaPayment.getAmount(), solanaPayment.getCurrency(), metaJson);
            conversionService.sendToGoogle(payment.getId(), solanaPayment.getAmount(), solanaPayment.getCurrency(), googleJson);

            log.info("Conversions dispatched for Solana payment id={}", payment.getId());
        } catch (Exception e) {
            log.error("Failed to dispatch conversions for payment id={}: {}",
                    payment.getId(), e.getMessage());
        }

        // 5. Create lead in Pipedrive if email is present (external CRM — non-critical)
        try {
            if (solanaPayment.getCustomerEmail() != null && !solanaPayment.getCustomerEmail().isBlank()) {
                String leadJson = buildLeadJson(solanaPayment);
                leadService.createLead(
                        solanaPayment.getCustomerEmail(),
                        null,
                        LeadSource.SOLANA_PAY,
                        leadJson
                );
                log.info("Lead created for Solana payment email={}", solanaPayment.getCustomerEmail());
            }
        } catch (Exception e) {
            log.error("Failed to create lead for payment id={}: {}",
                    payment.getId(), e.getMessage());
        }
    }

    // ── Expire timed-out payments ─────────────────────────────────────────────
    @Scheduled(fixedDelay = 60_000) // every 1 minute
    @Transactional
    public void expireOldPayments() {
        List<SolanaPayment> expired = solanaPayService.findExpired();

        if (expired.isEmpty()) {
            return;
        }

        log.info("Expiring {} Solana payment(s)", expired.size());

        expired.forEach(p -> {
            p.expire();
            solanaPaymentRepository.save(p);
        });
    }

    // ── JSON payload builders ─────────────────────────────────────────────────
    private String buildRawPayload(SolanaPayment p, String txSignature) {
        return String.format(
                "{\"source\":\"SOLANA_PAY\",\"reference\":\"%s\",\"txSignature\":\"%s\","
                + "\"amount\":\"%s\",\"currency\":\"%s\",\"recipientWallet\":\"%s\"}",
                p.getReference(), txSignature,
                p.getAmount().toPlainString(), p.getCurrency(), p.getRecipientWallet()
        );
    }

    private String buildMetaJson(SolanaPayment p, String txSignature) {
        return String.format(
                "{\"event\":\"Purchase\",\"value\":%s,\"currency\":\"%s\","
                + "\"source\":\"SOLANA_PAY\",\"txSignature\":\"%s\"}",
                p.getAmount().toPlainString(), p.getCurrency(), txSignature
        );
    }

    private String buildGoogleJson(SolanaPayment p, String txSignature) {
        return String.format(
                "{\"conversion\":\"Purchase\",\"value\":%s,\"currency\":\"%s\","
                + "\"source\":\"SOLANA_PAY\",\"txSignature\":\"%s\"}",
                p.getAmount().toPlainString(), p.getCurrency(), txSignature
        );
    }

    private String buildLeadJson(SolanaPayment p) {
        return String.format(
                "{\"email\":\"%s\",\"source\":\"SOLANA_PAY\",\"reference\":\"%s\"}",
                p.getCustomerEmail(), p.getReference()
        );
    }

    @Transactional
    public Map<String, Object> confirmManually(String reference, String txSignature) {
        SolanaPayment solanaPayment = solanaPaymentRepository.findByReference(reference)
                .orElseThrow(() -> new IllegalArgumentException("Solana payment not found"));

        if (solanaPayment.getStatus() != SolanaPaymentStatus.PENDING) {
            throw new IllegalArgumentException("Only pending Solana payments can be manually confirmed");
        }

        Merchant merchant = merchantRepository.findById(solanaPayment.getMerchantId())
                .orElseThrow(() -> new IllegalStateException(
                "Merchant not found: " + solanaPayment.getMerchantId()));

        var lead = solanaPayment.getLeadId() != null
                ? leadRepository.findById(solanaPayment.getLeadId()).orElse(null)
                : null;

        var payment = paymentService.upsertFromSolana(
                merchant,
                lead,
                "solana:" + txSignature,
                solanaPayment.getAmount(),
                solanaPayment.getCurrency(),
                PaymentStatus.SUCCEEDED,
                null,
                solanaPayment.getCustomerEmail(),
                buildRawPayload(solanaPayment, txSignature),
                txSignature
        );

        solanaPayment.confirm(txSignature, payment.getId());
        solanaPaymentRepository.save(solanaPayment);

        // Notify chat conversation (highest-priority side-effect — user-facing)
        try {
            chatService.sendPaymentConfirmedMessage(
                    solanaPayment.getMerchantId(),
                    solanaPayment.getCustomerEmail(),
                    solanaPayment.getAmount(),
                    txSignature
            );
        } catch (Exception e) {
            log.error("Failed to send chat notification for payment id={}: {}",
                    payment.getId(), e.getMessage());
        }

        // Dispatch conversions (external tracking — non-critical)
        try {
            String metaJson = buildMetaJson(solanaPayment, txSignature);
            String googleJson = buildGoogleJson(solanaPayment, txSignature);

            conversionService.sendToMeta(
                    payment.getId(),
                    solanaPayment.getAmount(),
                    solanaPayment.getCurrency(),
                    metaJson
            );

            conversionService.sendToGoogle(
                    payment.getId(),
                    solanaPayment.getAmount(),
                    solanaPayment.getCurrency(),
                    googleJson
            );
        } catch (Exception e) {
            log.error("Failed to dispatch conversions for payment id={}: {}",
                    payment.getId(), e.getMessage());
        }

        return Map.of(
                "message", "Solana payment manually confirmed",
                "reference", solanaPayment.getReference(),
                "txSignature", txSignature,
                "solanaPaymentId", solanaPayment.getId(),
                "paymentId", payment.getId(),
                "status", solanaPayment.getStatus().name()
        );
    }
}