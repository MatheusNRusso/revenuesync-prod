package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.dto.profile.PublicProfileResponse;
import com.mtnrs.revenuesync.dto.solana.SolanaPayDtos.*;
import com.mtnrs.revenuesync.domain.Lead;
import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.repository.LeadRepository;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.service.PublicProfileService;
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

    private static final BigDecimal DEFAULT_AMOUNT_SOL = new BigDecimal("0.01");

    private final MerchantRepository   merchantRepository;
    private final LeadRepository       leadRepository;
    private final SolanaPayService     solanaPayService;
    private final PublicProfileService publicProfileService;

    // ── Developer profiles ────────────────────────────────────────────────────

    @GetMapping("/profiles")
    public ResponseEntity<List<PublicProfileResponse>> listProfiles(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(publicProfileService.listPublic(category, search));
    }

    @GetMapping("/profiles/{slug}")
    public ResponseEntity<PublicProfileResponse> getProfile(@PathVariable String slug) {
        return ResponseEntity.ok(publicProfileService.getBySlug(slug));
    }

    // ── Merchants ─────────────────────────────────────────────────────────────

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

        return ResponseEntity.ok(solanaPayService.createPayment(merchant.getId(), lead.getId(), payRequest));
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

    // ── Helpers ───────────────────────────────────────────────────────────────

    private BigDecimal resolveAmount(BigDecimal... candidates) {
        for (BigDecimal candidate : candidates) {
            if (candidate != null && candidate.signum() > 0) return candidate;
        }
        return DEFAULT_AMOUNT_SOL;
    }
}
