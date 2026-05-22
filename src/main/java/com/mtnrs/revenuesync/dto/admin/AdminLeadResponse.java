package com.mtnrs.revenuesync.dto.admin;

import com.mtnrs.revenuesync.domain.Lead;
import com.mtnrs.revenuesync.domain.enums.LeadSource;

import java.time.OffsetDateTime;

public record AdminLeadResponse(
        Long id,
        String email,
        String name,
        LeadSource source,
        Long userId,
        String walletAddress,
        OffsetDateTime  createdAt
) {

    public static AdminLeadResponse from(Lead lead) {
        return new AdminLeadResponse(
                lead.getId(),
                lead.getEmail(),
                lead.getName(),
                lead.getSource(),
                lead.getUser() != null ? lead.getUser().getId() : null,
                maskWallet(lead.getWalletAddress()),
                lead.getCreatedAt()
        );
    }

    private static String maskWallet(String wallet) {
        if (wallet == null || wallet.length() < 8) {
            return wallet;
        }

        return wallet.substring(0, 4) + "..." + wallet.substring(wallet.length() - 4);
    }
}