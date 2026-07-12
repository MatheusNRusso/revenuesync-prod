package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.domain.Payment;
import com.mtnrs.revenuesync.dto.payment.PaymentResponseDto;
import com.mtnrs.revenuesync.mapper.PaymentMapper;
import com.mtnrs.revenuesync.service.CsvExportService;
import com.mtnrs.revenuesync.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Payment management endpoints")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;

    private final CsvExportService csvExportService;

    @GetMapping
    @Operation(summary = "Get all payments", description = "Returns a list of all payments in the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved list",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDto.class)))
    })
    public ResponseEntity<List<PaymentResponseDto>> findAll() {
        log.info("REST request to get all payments");

        List<PaymentResponseDto> payments = paymentService.findAll()
                .stream()
                .map(paymentMapper::toDto)
                .toList();

        log.debug("Retrieved {} payments", payments.size());
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get payment by ID", description = "Returns a single payment based on its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content)
    })
    public ResponseEntity<PaymentResponseDto> getById(
            @Parameter(description = "ID of the payment to retrieve", required = true)
            @PathVariable Long id) {

        log.info("REST request to get payment by id: {}", id);

        return paymentService.getById(id)
                .map(paymentMapper::toDto)
                .map(payment -> {
                    log.debug("Found payment: {}", payment.id());
                    return ResponseEntity.ok(payment);
                })
                .orElseGet(() -> {
                    log.warn("Payment not found for id: {}", id);
                    return ResponseEntity.notFound().build();
                });
    }

    @GetMapping("/external/{externalId}")
    @Operation(summary = "Get payment by external ID",
            description = "Returns a single payment based on its Stripe session ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Payment found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PaymentResponseDto.class))),
            @ApiResponse(responseCode = "404", description = "Payment not found",
                    content = @Content)
    })
    public ResponseEntity<PaymentResponseDto> getByExternalId(
            @Parameter(description = "Payment external ID (e.g. solana:<tx_signature>)", required = true)
            @PathVariable String externalId) {

        log.info("REST request to get payment by externalId: {}", externalId);

        return paymentService.getByExternalId(externalId)
                .map(paymentMapper::toDto)
                .map(payment -> {
                    log.debug("Found payment with externalId: {}", externalId);
                    return ResponseEntity.ok(payment);
                })
                .orElseGet(() -> {
                    log.warn("Payment not found for externalId: {}", externalId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Exports all payments to CSV format.
     * @return CSV file as downloadable attachment
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportPaymentsCsv() {
        List<Payment> payments = paymentService.findAll();
        byte[] csvContent = csvExportService.exportPaymentsToCsv(payments);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"payments-export-" +
                                java.time.LocalDateTime.now().format(
                                        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                                ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }
}