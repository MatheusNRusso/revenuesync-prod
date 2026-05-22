package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.dto.solana.SolanaPayDtos.*;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.service.SolanaPayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/solana")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Solana Pay", description = "Endpoints for Solana Pay payment processing")
public class SolanaPaymentController {

    private final SolanaPayService solanaPayService;
    private final MerchantRepository merchantRepository;

    /**
     * Creates a new Solana Pay payment.
     * Returns the solana:... URL that the frontend should render as a QR code
     * or open directly in the user's wallet app.
     */
    @PostMapping("/payment")
    @Operation(summary = "Create a Solana Pay payment")
    public ResponseEntity<CreatePaymentResponse> createPayment(
            @RequestBody @Valid CreatePaymentRequest request,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        Merchant merchant = merchantRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Merchant not found"));

        log.info("Creating Solana payment for merchant={} amount={} {}",
                merchant.getId(), request.amount(), request.currency());

        CreatePaymentResponse response = solanaPayService.createPayment(merchant.getId(), request);
        return ResponseEntity.ok(response);
    }

    /**
     * Returns the current status of a payment by its reference.
     * The frontend should poll this endpoint after displaying the QR code.
     */
    @GetMapping("/status/{reference}")
    @Operation(
            summary = "Get payment status",
            description = "Returns the current status: PENDING, CONFIRMED, EXPIRED, or FAILED"
    )
    public ResponseEntity<PaymentStatusResponse> getStatus(@PathVariable String reference) {
        return ResponseEntity.ok(solanaPayService.getStatus(reference));
    }

    /**
     * Transaction Request GET — Solana Pay spec.
     * Called by the wallet to retrieve the merchant's label and icon.
     */
    @GetMapping("/transaction-request/{merchantId}")
    @Operation(
            summary = "Transaction Request GET (Solana Pay spec)",
            description = "Returns label and icon for the wallet to display to the user"
    )
    public ResponseEntity<TransactionRequestMetadata> getTransactionMetadata(
            @PathVariable Long merchantId
    ) {
        return ResponseEntity.ok(new TransactionRequestMetadata(
                "RevenueSync",
                "https://revenuesync.com/icon.png"
        ));
    }

    /**
     * Transaction Request POST — Solana Pay spec.
     * The wallet sends the user's account (pubkey) and expects a serialized transaction back.
     * This endpoint is reserved for advanced custom transaction scenarios.
     * For standard transfers, use POST /api/solana/payment/{merchantId}.
     */
    @PostMapping("/transaction-request/{merchantId}")
    @Operation(
            summary = "Transaction Request POST (Solana Pay spec)",
            description = "Receives the user account and returns a serialized transaction to sign"
    )
    public ResponseEntity<TransactionRequestResponse> postTransactionRequest(
            @PathVariable Long merchantId,
            @RequestBody TransactionRequestBody body
    ) {
        log.info("Transaction request from account={} merchant={}", body.account(), merchantId);

        return ResponseEntity.ok(new TransactionRequestResponse(
                "Use POST /api/solana/payment/" + merchantId + " to create a transfer payment",
                null
        ));
    }

    // ── Inner records for Transaction Request spec ────────────────────────────

    public record TransactionRequestMetadata(String label, String icon) {}

    public record TransactionRequestBody(String account) {}

    public record TransactionRequestResponse(String message, String transaction) {}
}
