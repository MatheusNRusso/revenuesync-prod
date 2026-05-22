package com.mtnrs.revenuesync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtnrs.revenuesync.client.PipedriveClient;
import com.mtnrs.revenuesync.domain.Lead;
import com.mtnrs.revenuesync.domain.enums.LeadSource;
import com.mtnrs.revenuesync.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {

    private final PipedriveClient pipedriveClient;
    private final LeadRepository leadRepository;
    private final ObjectMapper objectMapper;

    public LeadService(PipedriveClient pipedriveClient, LeadRepository leadRepository, ObjectMapper objectMapper) {
        this.pipedriveClient = pipedriveClient;
        this.leadRepository = leadRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Lead createLead(String email, String name, LeadSource source, String requestJson) {
        Lead lead = Lead.of(email, name, source, requestJson);
        leadRepository.save(lead);

        pipedriveClient.createLead(requestJson);
        return lead;
    }
}
