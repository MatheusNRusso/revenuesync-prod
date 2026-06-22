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

import com.mtnrs.revenuesync.dto.auth.ChangePasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final MerchantRepository merchantRepository;
    private final MerchantProfileService merchantProfileService;
    private final SolanaPaymentRepository solanaPaymentRepository;
    private final UserRepository userRepository;
    private final PublicProfileService publicProfileService;
    private final PasswordEncoder passwordEncoder;

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
                    var totalPayments = paymentRepository.countByMerchant(merchant);
                    var totalRevenue = paymentRepository.sumSucceededAmountByMerchant(merchant);
                    var totalRevenueSol = paymentRepository.sumSucceededAmountByMerchantAndCurrency(merchant, "SOL");

                    return Map.<String, Object>of(
                            "id", merchant.getId(),
                            "name", merchant.getName(),
                            "email", merchant.getEmail(),
                            "slug", merchant.getSlug(),
                            "walletAddress", merchant.getWalletAddress(),
                            "active", merchant.isActive(),
                            "totalPayments", totalPayments,
                            "totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO,
                            "totalRevenueSol", totalRevenueSol != null ? totalRevenueSol : BigDecimal.ZERO
                    );
                })
                .toList();

        var totalPayments = merchantSummaries.stream()
                .mapToLong(s -> ((Number) s.get("totalPayments")).longValue()).sum();
        var totalRevenue = merchantSummaries.stream()
                .map(s -> (BigDecimal) s.get("totalRevenue"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var totalRevenueSol = merchantSummaries.stream()
                .map(s -> (BigDecimal) s.get("totalRevenueSol"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return ResponseEntity.ok(Map.of(
                "userId", user.getId(),
                "name", user.getName(),
                "email", user.getEmail(),
                "hasMerchants", !merchants.isEmpty(),
                "totalMerchants", merchants.size(),
                "totalPayments", totalPayments,
                "totalRevenue", totalRevenue,
                "totalRevenueSol", totalRevenueSol,
                "merchants", merchantSummaries
        ));
    }

    // ── Merchant CRUD ─────────────────────────────────────────────────────────
    @PostMapping("/merchant-profile")
    public ResponseEntity<?> createMerchantProfile(
            @AuthenticationPrincipal User user,
            @RequestBody CreateMerchantProfileRequest request
    ) {
        try {
            MerchantProfileResponse response
                    = merchantProfileService.createMerchantProfile(user, request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PutMapping("/merchants/{merchantId}/wallet")
    public ResponseEntity<?> updateWallet(
            @AuthenticationPrincipal User user,
            @PathVariable Long merchantId,
            @RequestBody Map<String, String> body
    ) {
        try {
            MerchantProfileResponse response = merchantProfileService.updateWalletAddress(
                    user, merchantId, body.get("walletAddress")
            );
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    // ── Merchant management ───────────────────────────────────────────────────
    @PatchMapping("/merchants/{merchantId}/activate")
    public ResponseEntity<?> activateMerchant(
            @AuthenticationPrincipal User user,
            @PathVariable Long merchantId
    ) {
        try {
            merchantProfileService.activateMerchant(user, merchantId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @PatchMapping("/merchants/{merchantId}/deactivate")
    public ResponseEntity<?> deactivateMerchant(
            @AuthenticationPrincipal User user,
            @PathVariable Long merchantId
    ) {
        try {
            merchantProfileService.deactivateMerchant(user, merchantId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }

    @DeleteMapping("/merchants/{merchantId}")
    public ResponseEntity<?> deleteMerchant(
            @AuthenticationPrincipal User user,
            @PathVariable Long merchantId
    ) {
        try {
            merchantProfileService.deleteMerchant(user, merchantId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
        }
    }
    // ── Payments ──────────────────────────────────────────────────────────────

    @GetMapping("/payments")
    public ResponseEntity<Page<PaymentResponseDto>> payments(
            @AuthenticationPrincipal User user,
            Pageable pageable
    ) {
        var merchants = merchantRepository.findAllByUserId(user.getId());
        if (merchants.isEmpty()) {
            return ResponseEntity.ok(Page.empty(pageable));
        }
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
        if (!merchant.belongsTo(user)) {
            return ResponseEntity.status(403).build();
        }
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
                            "reference", payment.getReference(),
                            "merchantId", payment.getMerchantId(),
                            "merchantName", merchantName,
                            "amount", payment.getAmount().toPlainString(),
                            "currency", payment.getCurrency(),
                            "status", payment.getStatus().name(),
                            "createdAt", payment.getCreatedAt() != null ? payment.getCreatedAt().toString() : "",
                            "confirmedAt", payment.getConfirmedAt() != null ? payment.getConfirmedAt().toString() : ""
                    );
                })
                .toList();
        return ResponseEntity.ok(purchases);
    }

    // ── Account management ───────────────────────────────────────────────────
    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal User user
    ) {
        var managed = userRepository.findById(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        managed.deactivate();
        userRepository.save(managed);
        return ResponseEntity.noContent().build();
    }

    // ── Password management ───────────────────────────────────────────────────
    @GetMapping("/has-password")
    public ResponseEntity<?> hasPassword(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(Map.of("hasPassword", user.hasPassword()));
    }

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        if (!request.newPassword().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Passwords do not match"));
        }

        // If user already has a real password, require current password
        if (user.hasPassword() && request.currentPassword() != null) {
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Current password is incorrect"));
            }
        }

        user.updatePassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of("message", "Password updated successfully"));
    }
}
