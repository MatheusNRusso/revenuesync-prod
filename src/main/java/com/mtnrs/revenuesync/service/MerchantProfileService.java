package com.mtnrs.revenuesync.service;

import com.mtnrs.revenuesync.domain.Merchant;
import com.mtnrs.revenuesync.domain.User;
import com.mtnrs.revenuesync.dto.merchant.CreateMerchantProfileRequest;
import com.mtnrs.revenuesync.dto.merchant.MeProfileResponse;
import com.mtnrs.revenuesync.dto.merchant.MerchantProfileResponse;
import com.mtnrs.revenuesync.repository.MerchantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MerchantProfileService {

    private final MerchantRepository merchantRepository;

    @Transactional(readOnly = true)
    public MeProfileResponse getCurrentUserProfile(User user) {
        var merchants = merchantRepository.findAllByUserId(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();

        return new MeProfileResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                !merchants.isEmpty(),
                user.isOnboardingCompleted(),
                merchants
        );
    }

    @Transactional
    public MerchantProfileResponse createMerchantProfile(
            User user,
            CreateMerchantProfileRequest request
    ) {
        String normalizedName = request.name() != null ? request.name().trim() : null;
        if (normalizedName != null
                && merchantRepository.existsByUserIdAndNameIgnoreCase(user.getId(), normalizedName)) {
            throw new IllegalArgumentException("You already have a merchant with this name");
        }

        String normalizedEmail = request.email() != null
                ? request.email().trim().toLowerCase()
                : null;
        if (normalizedEmail != null && merchantRepository.existsByEmail(normalizedEmail)) {
            throw new IllegalArgumentException("Merchant email already in use");
        }

        Merchant merchant = Merchant.createProfile(
                user,
                request.name(),
                request.email(),
                request.description(),
                request.avatarUrl(),
                request.walletAddress(),
                request.defaultAmountSol()
        );

        merchantRepository.save(merchant);
        return toResponse(merchant);
    }

    @Transactional
    public MerchantProfileResponse updateWalletAddress(
            User user, Long merchantId, String walletAddress
    ) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        if (!merchant.belongsTo(user)) {
            throw new IllegalArgumentException("Merchant does not belong to authenticated user");
        }
        merchant.updateWalletAddress(walletAddress);
        merchantRepository.save(merchant);
        return toResponse(merchant);
    }

    @Transactional
    public void activateMerchant(User user, Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        if (!merchant.belongsTo(user))
            throw new IllegalArgumentException("Merchant does not belong to authenticated user");
        merchant.activate();
        merchantRepository.save(merchant);
    }

    @Transactional
    public void deactivateMerchant(User user, Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        if (!merchant.belongsTo(user))
            throw new IllegalArgumentException("Merchant does not belong to authenticated user");
        merchant.deactivate();
        merchantRepository.save(merchant);
    }

    @Transactional
    public void deleteMerchant(User user, Long merchantId) {
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new IllegalArgumentException("Merchant not found"));
        if (!merchant.belongsTo(user))
            throw new IllegalArgumentException("Merchant does not belong to authenticated user");
        merchantRepository.delete(merchant);
    }

    public MerchantProfileResponse toResponse(Merchant merchant) {
        return new MerchantProfileResponse(
                merchant.getId(),
                merchant.getName(),
                merchant.getEmail(),
                merchant.getSlug(),
                merchant.getDescription(),
                merchant.getAvatarUrl(),
                merchant.getWalletAddress(),
                merchant.isActive()
        );
    }
}