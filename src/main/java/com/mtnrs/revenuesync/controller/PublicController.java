package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.dto.solana.SolanaPayDtos.*;
import com.mtnrs.revenuesync.domain.Lead;
import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.repository.LeadRepository;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.service.SolanaPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    /**
     * Default payment amount (SOL) used when neither the request body
     * nor the merchant entity carries a configured price.
     *
     * This is a temporary fallback. The correct long-term fix is to add
     * a defaultAmountSol column to the merchants table and drive the price
     * from there, so each merchant controls their own checkout price.
     */
    private static final BigDecimal DEFAULT_AMOUNT_SOL = new BigDecimal("0.01");

    private final MerchantRepository merchantRepository;
    private final LeadRepository     leadRepository;
    private final SolanaPayService   solanaPayService;

    @GetMapping("/merchants")
    public ResponseEntity<List<Map<String, Object>>> listMerchants() {
        var merchants = merchantRepository.findByActiveTrue()
                .stream()
                .map(m -> Map.<String, Object>of(
                        "id",          m.getId(),
                        "name",        m.getName(),
                        "slug",        m.getSlug()        != null ? m.getSlug()        : "",
                        "description", m.getDescription() != null ? m.getDescription() : "",
                        "avatarUrl",   m.getAvatarUrl()   != null ? m.getAvatarUrl()   : ""
                ))
                .toList();
        return ResponseEntity.ok(merchants);
    }

    @GetMapping("/merchants/{slug}")
    public ResponseEntity<Map<String, Object>> getMerchant(@PathVariable String slug) {
        return merchantRepository.findBySlug(slug)
                .map(m -> ResponseEntity.ok(Map.<String, Object>of(
                        "id",          m.getId(),
                        "name",        m.getName(),
                        "slug",        m.getSlug(),
                        "description", m.getDescription() != null ? m.getDescription() : "",
                        "avatarUrl",   m.getAvatarUrl()   != null ? m.getAvatarUrl()   : ""
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/pay/{slug}")
    public ResponseEntity<CreatePaymentResponse> pay(
            @PathVariable String slug,
            @RequestBody CreatePaymentRequest request,
            @AuthenticationPrincipal User user
    ) {
        var merchant = merchantRepository.findBySlug(slug)
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        var lead = leadRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    var created = Lead.of(user.getEmail(), user.getName(), null, null);
                    created.assignUser(user);
                    return leadRepository.save(created);
                });

        // ── Amount resolution ─────────────────────────────────────────────────
        //
        // Priority order:
        //   1. merchant.getDefaultAmountSol()  — when that field is added (recommended)
        //   2. request.amount()                — explicit override from the frontend (admin/test flows)
        //   3. DEFAULT_AMOUNT_SOL              — safe fallback so the endpoint never throws
        //
        // The buyer must NOT set the price on a public checkout. Once defaultAmountSol
        // is added to the Merchant entity, remove the request.amount() fallback here.
        //
        // merchant.getDefaultAmountSol() is always the authoritative price source.
        // request.amount() is an explicit override accepted only from trusted flows
        // (e.g. admin or merchant self-test). On public buyer checkouts the frontend
        // sends an empty body, so request.amount() is null and the merchant price wins.
        BigDecimal resolvedAmount = resolveAmount(
                merchant.getDefaultAmountSol(),
                request.amount()
        );

        var payRequest = new CreatePaymentRequest(
                resolvedAmount,
                request.currency(),
                request.splToken(),
                lead.getEmail(),
                request.label(),
                request.message(),
                request.expirationMinutes()
        );

        var response = solanaPayService.createPayment(merchant.getId(), lead.getId(), payRequest);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/leads/register")
    public ResponseEntity<Map<String, Object>> registerLead(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String name  = body.get("name");
        if (email == null || email.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email is required"));
        }
        Lead lead = leadRepository.findByEmail(email.trim().toLowerCase())
                .orElseGet(() -> leadRepository.save(Lead.of(email, name, null, null)));
        return ResponseEntity.ok(Map.of("leadId", lead.getId(), "email", lead.getEmail()));
    }

    @PostMapping("/leads/connect-wallet")
    public ResponseEntity<Map<String, Object>> connectWallet(@RequestBody Map<String, String> body) {
        String walletAddress = body.get("walletAddress");
        String name          = body.get("name");
        if (walletAddress == null || walletAddress.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "walletAddress is required"));
        }
        Lead lead = leadRepository.findByWalletAddress(walletAddress.trim())
                .orElseGet(() -> leadRepository.save(Lead.walletOnly(walletAddress, name)));
        return ResponseEntity.ok(Map.of("leadId", lead.getId(), "walletAddress", lead.getWalletAddress()));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Returns the first non-null positive amount from the candidate values,
     * falling back to the module-level default.
     */
    private BigDecimal resolveAmount(BigDecimal... candidates) {
        for (BigDecimal candidate : candidates) {
            if (candidate != null && candidate.signum() > 0) {
                return candidate;
            }
        }
        return DEFAULT_AMOUNT_SOL;
    }
}