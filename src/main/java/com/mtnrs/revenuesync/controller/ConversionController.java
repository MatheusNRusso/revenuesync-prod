package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.domain.Conversion;
import com.mtnrs.revenuesync.domain.enums.ConversionPlatform;
import com.mtnrs.revenuesync.dto.conversion.ConversionResponseDto;
import com.mtnrs.revenuesync.service.ConversionService;
import com.mtnrs.revenuesync.mapper.ConversionMapper;
import com.mtnrs.revenuesync.service.CsvExportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for conversion tracking operations.
 * Provides endpoints for the Angular frontend to query conversion data.
 */
@RestController
@RequestMapping("/api/conversions")
@RequiredArgsConstructor
@Tag(name = "Conversions", description = "Operations for querying conversion tracking data")
public class ConversionController {

    private final ConversionService conversionService;
    private final ConversionMapper conversionMapper;
    private final CsvExportService csvExportService;

    /**
     * Returns all conversions for audit and dashboard display.
     *
     * @return list of ConversionResponseDto in camelCase JSON format
     */
    @GetMapping
    @Operation(summary = "List all conversion records")
    public ResponseEntity<List<ConversionResponseDto>> listConversions() {
        var conversions = conversionService.findAll();
        var dtos = conversionMapper.toDtoList(conversions);
        return ResponseEntity.ok(dtos);
    }
    /**
     * Returns conversions filtered by payment ID.
     *
     * @param paymentId the ID of the payment to filter by
     * @return list of ConversionResponseDto for the specified payment
     */
    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "List conversions for a specific payment")
    public ResponseEntity<List<ConversionResponseDto>> listByPayment(
            @PathVariable Long paymentId) {
        var conversions = conversionService.findByPaymentId(paymentId);
        var dtos = conversionMapper.toDtoList(conversions);
        return ResponseEntity.ok(dtos);
    }
    /**
     * Returns conversions filtered by platform.
     *
     * @param platform the platform to filter by (META or GOOGLE)
     * @return list of ConversionResponseDto for the specified platform
     */
    @GetMapping("/platform/{platform}")
    @Operation(summary = "List conversions by advertising platform")
    public ResponseEntity<List<ConversionResponseDto>> listByPlatform(
            @PathVariable ConversionPlatform platform) {
        var conversions = conversionService.findByPlatform(platform);
        var dtos = conversionMapper.toDtoList(conversions);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Exports all conversions to CSV format.
     * @return CSV file as downloadable attachment
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportConversionsCsv() {
        List<Conversion> conversions = conversionService.findAll();
        byte[] csvContent = csvExportService.exportConversionsToCsv(conversions);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"conversions-export-" +
                                java.time.LocalDateTime.now().format(
                                        java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) +
                                ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csvContent);
    }
}