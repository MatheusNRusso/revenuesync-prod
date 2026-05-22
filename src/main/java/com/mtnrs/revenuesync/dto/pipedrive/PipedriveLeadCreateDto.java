package com.mtnrs.revenuesync.dto.pipedrive;

public record PipedriveLeadCreateDto(
        String title,
        String name,
        String email,
        String source
) {}
