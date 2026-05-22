package com.mtnrs.revenuesync.dto.conversion;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

/**
 * Response DTO for Conversion entities.
 * Implemented as a record for immutability and conciseness.
 * All fields use camelCase convention for Angular/TypeScript compatibility.
 *
 * @param id Internal conversion ID
 * @param paymentId ID of the associated payment
 * @param platform Target advertising platform (META or GOOGLE)
 * @param value Conversion value
 * @param requestPayload Raw JSON payload sent to the platform API
 * @param responsePayload Raw JSON response received from the platform API
 * @param createdAt Creation timestamp in ISO 8601 format
 */
@Schema(description = "Response DTO for conversion tracking records")
public record ConversionResponseDto(
        @Schema(description = "Internal conversion ID", example = "1")
        Long id,

        @Schema(description = "ID of the associated payment", example = "42")
        Long paymentId,

        @Schema(description = "Target advertising platform", example = "META", allowableValues = {"META", "GOOGLE"})
        String platform,

        @Schema(description = "Conversion value", example = "19.90")
        BigDecimal value,

        @Schema(description = "Currency code in ISO 4217 format", example = "SOL")
        String currency,

        @Schema(description = "Raw JSON payload sent to the platform API")
        String requestPayload,

        @Schema(description = "Raw JSON response received from the platform API")
        String responsePayload,

        @Schema(description = "Creation timestamp in ISO 8601 format", example = "2026-03-10T14:30:00.123456Z")
        String createdAt
) {}