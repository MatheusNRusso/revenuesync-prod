package com.mtnrs.revenuesync.dto.admin;

import com.mtnrs.revenuesync.domain.Merchant;

public record AdminMerchantResponse(
        Long id,
        Long userId,
        String name,
        String slug,
        String email,
        String walletAddress,
        boolean active
) {

    public static AdminMerchantResponse from(Merchant merchant) {
        return new AdminMerchantResponse(
                merchant.getId(),
                merchant.getUser() != null ? merchant.getUser().getId() : null,
                merchant.getName(),
                merchant.getSlug(),
                merchant.getEmail(),
                maskWallet(merchant.getWalletAddress()),
                merchant.isActive()
        );
    }

    private static String maskWallet(String wallet) {
        if (wallet == null || wallet.length() < 8) {
            return wallet;
        }

        return wallet.substring(0, 4) + "..." + wallet.substring(wallet.length() - 4);
    }
}