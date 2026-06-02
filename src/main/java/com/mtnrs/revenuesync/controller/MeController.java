package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.dto.merchant.MeProfileResponse;
import com.mtnrs.revenuesync.dto.merchant.CreateMerchantProfileRequest;
import com.mtnrs.revenuesync.dto.merchant.MerchantProfileResponse;
import com.mtnrs.revenuesync.dto.payment.PaymentResponseDto;
import com.mtnrs.revenuesync.dto.profile.PublicProfileResponse;
import com.mtnrs.revenuesync.dto.profile.UpsertPublicProfileRequest;
import com.mtnrs.revenuesync.mapper.PaymentMapper;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.repository.PaymentRepository;
import com.mtnrs.revenuesync.repository.SolanaPaymentRepository;
import com.mtnrs.revenuesync.repository.UserRepository;
import com.mtnrs.revenuesync.service.MerchantProfileService;
import com.mtnrs.revenuesync.service.PublicProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final PaymentRepository       paymentRepository;
    private final PaymentMapper           paymentMapper;
    private final MerchantRepository      merchantRepository;
    private final MerchantProfileService  merchantProfileService;
    private final SolanaPaymentRepository solanaPaymentRepository;
    private final UserRepository          userRepository;
    private final PublicProfileService    publicProfileService;

    // ── Public profile ────────────────────────────────────────────────────────

    @GetMapping("/public-profile")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(
            @AuthenticationPrincipal User user
    ) {
        return ResponseEntity.ok(publicProfileService.getMyProfile(user));
    }

    @PutMapping("/public-profile")
    public ResponseEntity<PublicProfileResponse> upsertPublicProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpsertPublicProfileRequest request
    ) {
        return ResponseEntity.ok(publicProfileService.upsert(user, request));
    }

    @PatchMapping("/public-profile/visibility")
    public ResponseEntity<PublicProfileResponse> setVisibility(
            @AuthenticationPrincipal User user,
            @RequestParam boolean isPublic
    ) {
        return ResponseEntity.ok(publicProfileService.setVisibility(user, isPublic));
    }

    // ── Dashboard ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> dashboard(
            @AuthenticationPrincipal User user
    ) {
        var merchants = merchantRepository.findAllByUserId(user.getId());

        var merchantSummaries = merchants.stream()
                .map(merchant -> {
                    var totalPayments    = paymentRepository.countByMerchant(merchant);
                    var totalRevenue     = paymentRepository.sumSucceededAmountByMerchant(merchant);
                    var totalRevenueSol  = paymentRepository.sumSucceededAmountByMerchantAndCurrency(merchant, "SOL");

                    return Map.<String, Object>of(
                            "id",              merchant.getId(),
                            "name",            merchant.getName(),
                            "email",           merchant.getEmail(),
                            "slug",            merchant.getSlug(),
                            "walletAddress",   merchant.getWalletAddress(),
                            "active",          merchant.isActive(),
                            "totalPayments",   totalPayments,
                            "totalRevenue",    totalRevenue    != null ? totalRevenue    : BigDecimal.ZERO,
                            "totalRevenueSol", totalRevenueSol != null ? totalRevenueSol : BigDecimal.ZERO
                    );
                })
                .toList();

        var totalPayments   = merchantSummaries.stream()
                .mapToLong(s -> ((Number) s.get("totalPayments")).longValue()).sum();
        var totalRevenue    = merchantSummaries.stream()
                .map(s -> (BigDecimal) s.get("totalRevenue"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalRevenueSol = merchantSummaries.stream()
                .map(s -> (BigDecimal) s.get("totalRevenueSol"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "userId",          user.getId(),
                "name",            user.getName(),
                "email",           user.getEmail(),
                "hasMerchants",    !merchants.isEmpty(),
                "totalMerchants",  merchants.size(),
                "totalPayments",   totalPayments,
                "totalRevenue",    totalRevenue,
                "totalRevenueSol", totalRevenueSol,
                "merchants",       merchantSummaries
        ));
    }

    // ── Payments ──────────────────────────────────────────────────────────────

    @GetMapping("/payments")
    public ResponseEntity<Page<PaymentResponseDto>> payments(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        var merchants = merchantRepository.findAllByUserId(user.getId());
        if (merchants.isEmpty()) return ResponseEntity.ok(Page.empty(pageable));
        return ResponseEntity.ok(
                paymentRepository.findByMerchantIn(merchants, pageable)
                        .map(paymentMapper::toDto)
        );
    }

    @GetMapping("/merchants/{merchantId}/payments")
    public ResponseEntity<Page<PaymentResponseDto>> paymentsByMerchant(
            @AuthenticationPrincipal User user,
            @PathVariable Long merchantId,
            Pageable pageable
    ) {
        var merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        if (!merchant.belongsTo(user)) return ResponseEntity.status(403).build();
        return ResponseEntity.ok(
                paymentRepository.findByMerchant(merchant, pageable)
                        .map(paymentMapper::toDto)
        );
    }

    // ── Purchases ─────────────────────────────────────────────────────────────

    @GetMapping("/purchases")
    public ResponseEntity<List<Map<String, Object>>> purchases(
            @AuthenticationPrincipal User user
    ) {
        var purchases = solanaPaymentRepository
                .findByCustomerEmailOrderByCreatedAtDesc(user.getEmail())
                .stream()
                .map(payment -> {
                    String merchantName = merchantRepository.findById(payment.getMerchantId())
                            .map(m -> m.getName())
                            .orElse("Unknown merchant");
                    return Map.<String, Object>of(
                            "reference",    payment.getReference(),
                            "merchantId",   payment.getMerchantId(),
                            "merchantName", merchantName,
                            "amount",       payment.getAmount().toPlainString(),
                            "currency",     payment.getCurrency(),
                            "status",       payment.getStatus().name(),
                            "createdAt",    payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "",
                            "confirmedAt",  payment.getConfirmedAt() != null ? payment.getConfirmedAt().toString() : ""
                    );
                })
                .toList();
        return ResponseEntity.ok(purchases);
    }
}
