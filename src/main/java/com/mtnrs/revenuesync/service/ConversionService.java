package com.mtnrs.revenuesync.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtnrs.revenuesync.client.GoogleAdsClient;
import com.mtnrs.revenuesync.client.MetaCapiClient;
import com.mtnrs.revenuesync.domain.Conversion;
import com.mtnrs.revenuesync.domain.Payment;
import com.mtnrs.revenuesync.domain.enums.ConversionPlatform;
import com.mtnrs.revenuesync.repository.ConversionRepository;
import com.mtnrs.revenuesync.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ConversionService {

    private final MetaCapiClient metaCapiClient;
    private final GoogleAdsClient googleAdsClient;
    private final ConversionRepository conversionRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public ConversionService(
            MetaCapiClient metaCapiClient,
            GoogleAdsClient googleAdsClient,
            ConversionRepository conversionRepository,
            PaymentRepository paymentRepository,
            ObjectMapper objectMapper
    ) {
        this.metaCapiClient = metaCapiClient;
        this.googleAdsClient = googleAdsClient;
        this.conversionRepository = conversionRepository;
        this.paymentRepository = paymentRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Conversion sendToMeta(Long paymentId, BigDecimal value, String currency, String requestJson) {
        String response = metaCapiClient.sendPurchaseEvent(requestJson);
        Payment payment = getPayment(paymentId);

        Conversion conv = Conversion.of(
                paymentId,
                payment.getMerchant().getId(),
                payment.getLead() != null ? payment.getLead().getId() : null,
                ConversionPlatform.META,
                value,
                currency,
                ensureJson(requestJson),
                ensureJson(response)
        );

        return conversionRepository.save(conv);
    }

    @Transactional
    public Conversion sendToGoogle(Long paymentId, BigDecimal value, String currency, String requestJson) {
        String response = googleAdsClient.sendPurchaseConversion(requestJson);
        Payment payment = getPayment(paymentId);

        Conversion conv = Conversion.of(
                paymentId,
                payment.getMerchant().getId(),
                payment.getLead() != null ? payment.getLead().getId() : null,
                ConversionPlatform.GOOGLE,
                value,
                currency,
                ensureJson(requestJson),
                ensureJson(response)
        );

        return conversionRepository.save(conv);
    }

    @Transactional(readOnly = true)
    public List<Conversion> findAll() {
        return conversionRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Conversion> findByPaymentId(Long paymentId) {
        return conversionRepository.findByPaymentIdOrderByCreatedAtDesc(paymentId);
    }

    @Transactional(readOnly = true)
    public List<Conversion> findByPlatform(ConversionPlatform platform) {
        return conversionRepository.findByPlatformOrderByCreatedAtDesc(platform);
    }

    private Payment getPayment(Long paymentId) {
        if (paymentId == null) {
            throw new IllegalArgumentException("paymentId must not be null");
        }

        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));
    }

    private String ensureJson(String payload) {
        if (payload == null || payload.isBlank()) return null;

        String p = payload.trim();
        if (p.startsWith("{") || p.startsWith("[")) return p;

        return "\"" + p.replace("\"", "\\\"") + "\"";
    }
}