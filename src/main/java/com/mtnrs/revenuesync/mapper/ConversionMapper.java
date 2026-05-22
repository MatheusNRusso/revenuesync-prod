package com.mtnrs.revenuesync.mapper;

import com.mtnrs.revenuesync.domain.Conversion;
import com.mtnrs.revenuesync.dto.conversion.ConversionResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

/**
 * MapStruct mapper for Conversion entity to DTO conversions.
 * Uses Spring component model for dependency injection.
 * Target DTO is a Java record for immutability.
 */
@Mapper(componentModel = "spring")
public interface ConversionMapper {

    /**
     * Converts a Conversion entity to a ConversionResponseDto record.
     * All field mappings follow camelCase convention for frontend compatibility.
     *
     * @param conversion the Conversion entity to convert
     * @return the fully mapped ConversionResponseDto record
     */
    @Mapping(target = "createdAt", source = "createdAt", qualifiedByName = "formatDateTime")
    ConversionResponseDto toDto(Conversion conversion);

    /**
     * Formats an OffsetDateTime to ISO 8601 string format.
     * Example: 2026-03-10T14:30:00.123456Z
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

    /**
     * Maps a list of Conversion entities to a list of ConversionResponseDto records.
     * MapStruct auto-generates this implementation.
     *
     * @param conversions list of Conversion entities
     * @return list of ConversionResponseDto records
     */
    java.util.List<ConversionResponseDto> toDtoList(java.util.List<Conversion> conversions);
}