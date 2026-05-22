package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.enums.ConversionPlatform;
import com.mtnrs.revenuesync.dto.admin.AdminConversionResponse;
import com.mtnrs.revenuesync.dto.admin.AdminDashboardResponse;
import com.mtnrs.revenuesync.dto.admin.AdminLeadResponse;
import com.mtnrs.revenuesync.dto.admin.AdminMerchantResponse;
import com.mtnrs.revenuesync.dto.admin.AdminPaymentResponse;
import com.mtnrs.revenuesync.repository.ConversionRepository;
import com.mtnrs.revenuesync.repository.LeadRepository;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import com.mtnrs.revenuesync.repository.PaymentRepository;
import com.mtnrs.revenuesync.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final MerchantRepository merchantRepository;
    private final LeadRepository leadRepository;
    private final PaymentRepository paymentRepository;
    private final ConversionRepository conversionRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        var latestPageable = PageRequest.of(
                0,
                10,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        var latestPayments = paymentRepository.findAll(latestPageable)
                .map(AdminPaymentResponse::from)
                .getContent();

        var latestLeads = leadRepository.findAll(latestPageable)
                .map(AdminLeadResponse::from)
                .getContent();

        var latestMerchants = merchantRepository.findAll(latestPageable)
                .map(AdminMerchantResponse::from)
                .getContent();

        var latestConversions = conversionRepository.findAll(latestPageable)
                .map(AdminConversionResponse::from)
                .getContent();

        return new AdminDashboardResponse(
                userRepository.count(),
                merchantRepository.count(),
                leadRepository.count(),
                paymentRepository.count(),
                sumSucceededSolRevenue(),
                conversionRepository.count(),
                countConversionsByPlatform(ConversionPlatform.META),
                countConversionsByPlatform(ConversionPlatform.GOOGLE),
                latestPayments,
                latestLeads,
                latestMerchants,
                latestConversions
        );
    }

    private BigDecimal sumSucceededSolRevenue() {
        return paymentRepository.findAll()
                .stream()
                .filter(payment -> payment.getStatus() != null)
                .filter(payment -> payment.getStatus().name().equals("SUCCEEDED"))
                .filter(payment -> "SOL".equalsIgnoreCase(payment.getCurrency()))
                .map(payment -> payment.getAmount() != null ? payment.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long countConversionsByPlatform(ConversionPlatform platform) {
        return conversionRepository.findByPlatformOrderByCreatedAtDesc(platform).size();
    }

    @Transactional(readOnly = true)
    public List<AdminPaymentResponse> findPayments() {
        return paymentRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
                .stream()
                .map(AdminPaymentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminLeadResponse> findLeads() {
        return leadRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
                .stream()
                .map(AdminLeadResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminMerchantResponse> findMerchants() {
        return merchantRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "id")
                )
                .stream()
                .map(AdminMerchantResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminConversionResponse> findConversions() {
        return conversionRepository.findAll(
                        Sort.by(Sort.Direction.DESC, "createdAt")
                )
                .stream()
                .map(AdminConversionResponse::from)
                .toList();
    }
}