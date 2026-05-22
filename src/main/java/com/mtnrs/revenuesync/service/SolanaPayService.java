package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.config.SolanaConfig;
import com.mtnrs.revenuesync.domain.Lead;
import com.mtnrs.revenuesync.domain.SolanaPayment;
import com.mtnrs.revenuesync.domain.enums.SolanaPaymentStatus;
import com.mtnrs.revenuesync.dto.solana.SolanaPayDtos.*;
import com.mtnrs.revenuesync.infra.exception.BusinessException;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.repository.LeadRepository;
import com.mtnrs.revenuesync.repository.SolanaPaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SolanaPayService {

    private final SolanaPaymentRepository solanaPaymentRepository;
    private final MerchantRepository merchantRepository;
    private final LeadRepository leadRepository;
    private final SolanaConfig solanaConfig;
    private final WebClient  webClient;

    private final SecureRandom secureRandom = new SecureRandom();

    // ── Create payment ────────────────────────────────────────────────────────

    @Transactional
    public CreatePaymentResponse createPayment(Long merchantId, CreatePaymentRequest req) {
        return createPayment(merchantId, null, req);
    }

    @Transactional
    public CreatePaymentResponse createPayment(Long merchantId, Long leadId, CreatePaymentRequest req) {

        var merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new BusinessException("Merchant not found: " + merchantId));
        if (req.customerEmail() == null || req.customerEmail().isBlank()) {
            throw new BusinessException("Customer email is required to generate QR payment");
        }
        if (merchant.getWalletAddress() == null || merchant.getWalletAddress().isBlank()) {
            throw new BusinessException("Merchant wallet is not configured");
        }

        if (leadId != null) {
            leadRepository.findById(leadId)
                    .orElseThrow(() -> new BusinessException("Lead not found: " + leadId));
        }

        String reference       = generateReference();
        String recipientWallet = merchant.getWalletAddress();
        int    expiration      = req.resolvedExpiration();

        SolanaPayment payment = SolanaPayment.create(
                merchantId,
                leadId,
                reference,
                recipientWallet,
                req.amount(),
                req.currency() != null ? req.currency() : "SOL",
                req.splToken(),
                req.customerEmail(),
                req.label(),
                req.message(),
                expiration
        );

        solanaPaymentRepository.save(payment);
        log.info("Solana payment created: reference={} merchant={} amount={} {}",
                reference, merchantId, req.amount(), payment.getCurrency());

        String solanaPayUrl = buildSolanaPayUrl(payment);

        return new CreatePaymentResponse(
                reference,
                solanaPayUrl,
                solanaPayUrl,   // qrCodeData — the URL itself (wallets scan it directly)
                payment.getStatus().name(),
                payment.getAmount().toPlainString(),
                payment.getCurrency(),
                recipientWallet,
                formatDateTime(payment.getExpiresAt()),
                formatDateTime(payment.getCreatedAt())
        );
    }

    // ── Get status ────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public PaymentStatusResponse getStatus(String reference) {
        SolanaPayment payment = solanaPaymentRepository.findByReference(reference)
                .orElseThrow(() -> new BusinessException("Payment not found: " + reference));

        return new PaymentStatusResponse(
                payment.getReference(),
                payment.getStatus().name(),
                payment.getTxSignature(),
                payment.getPaymentId(),
                formatDateTime(payment.getConfirmedAt()),
                formatDateTime(payment.getExpiresAt())
        );
    }

    // ── Verify transaction on-chain ───────────────────────────────────────────

    /**
     * Queries the Solana RPC to check whether a transaction referencing
     * this payment's reference address has been confirmed on-chain.
     *
     * @return Optional containing the confirmed transaction signature, or empty if not found
     */
    public Optional<String> verifyOnChain(String reference) {
        try {
            Map<String, Object> requestBody = Map.of(
                    "jsonrpc", "2.0",
                    "id", 1,
                    "method", "getSignaturesForAddress",
                    "params", List.of(
                            reference,
                            Map.of("limit", 5, "commitment", "confirmed")
                    )
            );

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient
                    .post()
                    .uri(solanaConfig.rpcUrl())
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null || response.containsKey("error")) {
                log.warn("Solana RPC error for reference {}: {}", reference,
                        response != null ? response.get("error") : "null response");
                return Optional.empty();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("result");

            if (results == null || results.isEmpty()) {
                return Optional.empty();
            }

            // Return the first confirmed transaction with no errors
            return results.stream()
                    .filter(tx -> tx.get("err") == null)
                    .map(tx -> (String) tx.get("signature"))
                    .filter(sig -> sig != null && !sig.isBlank())
                    .findFirst();

        } catch (Exception e) {
            log.error("Error verifying Solana transaction for reference {}: {}", reference, e.getMessage());
            return Optional.empty();
        }
    }

    // ── List by merchant ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<SolanaPayment> listByMerchant(Long merchantId) {
        return solanaPaymentRepository.findByMerchantIdOrderByCreatedAtDesc(merchantId);
    }

    // ── Helpers for the verification job ─────────────────────────────────────

    public List<SolanaPayment> findPendingActive() {
        return solanaPaymentRepository.findActiveByStatus(
                SolanaPaymentStatus.PENDING,
                OffsetDateTime.now()
        );
    }

    public List<SolanaPayment> findExpired() {
        return solanaPaymentRepository.findExpired(OffsetDateTime.now());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Builds a Solana Pay URL per the official spec:
     * solana:<recipient>?amount=<amount>&reference=<ref>&label=<label>&message=<msg>
     */
    private String buildSolanaPayUrl(SolanaPayment payment) {
        StringBuilder url = new StringBuilder("solana:");
        url.append(payment.getRecipientWallet());
        url.append("?amount=").append(payment.getAmount().toPlainString());
        url.append("&reference=").append(payment.getReference());

        if (payment.getSplToken() != null && !payment.getSplToken().isBlank()) {
            url.append("&spl-token=").append(payment.getSplToken());
        }
        if (payment.getLabel() != null && !payment.getLabel().isBlank()) {
            url.append("&label=").append(encode(payment.getLabel()));
        }
        if (payment.getMessage() != null && !payment.getMessage().isBlank()) {
            url.append("&message=").append(encode(payment.getMessage()));
        }

        return url.toString();
    }

    /**
     * Generates a unique 32-byte reference encoded as URL-safe base64.
     * Per the Solana Pay spec, the reference must be a 32-byte base58 public key.
     * URL-safe base64 is used here for simplicity and is spec-compatible.
     */

    /**
     * Generates a unique 32-byte reference encoded as Base58.
     * The Solana RPC requires the reference to be a valid Base58 public key format.
     */
    private String generateReference() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return encodeBase58(bytes);
    }

    private static final char[] BASE58_ALPHABET =
            "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz".toCharArray();

    private String encodeBase58(byte[] input) {
        BigInteger value = new BigInteger(1, input);
        StringBuilder sb = new StringBuilder();
        BigInteger base  = BigInteger.valueOf(58);

        while (value.compareTo(BigInteger.ZERO) > 0) {
            BigInteger[] divRem = value.divideAndRemainder(base);
            sb.append(BASE58_ALPHABET[divRem[1].intValue()]);
            value = divRem[0];
        }

        for (byte b : input) {
            if (b == 0) sb.append(BASE58_ALPHABET[0]);
            else break;
        }

        return sb.reverse().toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String formatDateTime(OffsetDateTime dt) {
        return dt != null ? dt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME) : null;
    }

}