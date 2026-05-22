package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.domain.Lead;
import com.mtnrs.revenuesync.repository.LeadRepository;
import com.mtnrs.revenuesync.service.CsvExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/leads")
@RequiredArgsConstructor
public class LeadController {

    private final LeadRepository  leadRepository;
    private final CsvExportService csvExportService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<Lead> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return leadRepository.findAll(
                PageRequest.of(page, size, Sort.by("createdAt").descending())
        );
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportCsv() {
        var leads = leadRepository.findAll(Sort.by("createdAt").descending());
        var csv   = csvExportService.exportLeadsToCsv(leads);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=leads.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}