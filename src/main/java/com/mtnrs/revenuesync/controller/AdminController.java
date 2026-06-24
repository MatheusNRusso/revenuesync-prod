package com.mtnrs.revenuesync.controller;

import com.mtnrs.revenuesync.dto.admin.*;
import com.mtnrs.revenuesync.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AdminDashboardResponse> dashboard() {
        return ResponseEntity.ok(adminService.getDashboard());
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminPaymentResponse>> payments() {
        return ResponseEntity.ok(adminService.findPayments());
    }

    @GetMapping("/leads")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminLeadResponse>> leads() {
        return ResponseEntity.ok(adminService.findLeads());
    }

    @GetMapping("/merchants")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminMerchantResponse>> merchants() {
        return ResponseEntity.ok(adminService.findMerchants());
    }

    @GetMapping("/conversions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminConversionResponse>> conversions() {
        return ResponseEntity.ok(adminService.findConversions());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminUserResponse>> users() {
        return ResponseEntity.ok(adminService.findUsers());
    }

    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable Long userId) {
        adminService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/merchants/{merchantId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMerchant(@PathVariable Long merchantId) {
        adminService.deleteMerchant(merchantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateUser(@PathVariable Long userId) {
        adminService.activateUser(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/users/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateUser(@PathVariable Long userId) {
        adminService.deactivateUser(userId);
        return ResponseEntity.noContent().build();
    }
}
