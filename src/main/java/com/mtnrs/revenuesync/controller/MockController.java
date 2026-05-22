package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.service.SolanaVerificationJob;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;



import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/mock")
public class MockController {

    private final SolanaVerificationJob solanaVerificationJob;

    public MockController(SolanaVerificationJob solanaVerificationJob) {
        this.solanaVerificationJob = solanaVerificationJob;
    }

    @PostMapping(value = "/google", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> google(@RequestBody(required = false) String body) {
        return ResponseEntity.ok("""
            {"status":"OK","provider":"GOOGLE","mode":"MOCK"}
        """);
    }

    @PostMapping(value = "/meta", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> meta(@RequestBody(required = false) String body) {
        return ResponseEntity.ok("""
            {"status":"OK","provider":"META","mode":"MOCK"}
        """);
    }

    @PostMapping(value = "/pipedrive", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> pipedrive(@RequestBody(required = false) String body) {
        return ResponseEntity.ok("""
            {"status":"OK","provider":"PIPEDRIVE","mode":"MOCK"}
        """);
    }
    @PostMapping(value = "/solana/confirm/{reference}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> confirmSolanaPayment(
            @PathVariable String reference
    ) {
        String txSignature = "mock-solana-tx-" + UUID.randomUUID();

        Map<String, Object> response = solanaVerificationJob.confirmManually(
                reference,
                txSignature
        );

        return ResponseEntity.ok(response);
    }
}
