package com.mtnrs.revenuesync.mapper;

import com.mtnrs.revenuesync.domain.Payment;
import com.mtnrs.revenuesync.dto.payment.PaymentResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MapStruct mapper for Payment entity to DTO conversions
 * Uses Spring component model for dependency injection
 */
@Mapper(componentModel = "spring")
public interface PaymentMapper {

    /**
     * Converts a Payment entity to a PaymentResponseDto
     *
     * @param payment the Payment entity to convert
     * @return the fully mapped PaymentResponseDto
     */
    @Mapping(target = "externalPaymentId", source = "externalId")
    @Mapping(target = "customerName", source = "payment", qualifiedByName = "extractCustomerName")
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatDateTime")
    PaymentResponseDto toDto(Payment payment);

    /**
     * Extracts the customer name directly from the database column.
     * The customer_name field is populated by the database migration V2.
     *
     * @param payment the Payment entity containing the customer name
     * @return the customer name, or null if payment is null
     */
    @Named("extractCustomerName")
    default String extractCustomerName(Payment payment) {
        if (payment == null) {
            return null;
        }
        return payment.getCustomerName();
    }

    /**
     * Formats an OffsetDateTime to ISO 8601 string format.
     * Example: 2026-03-08T19:47:09.636651Z
     *
     * @param dateTime the OffsetDateTime to format
     * @return formatted ISO string, or null if input is null
     */
    @Named("formatDateTime")
    default String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }
}