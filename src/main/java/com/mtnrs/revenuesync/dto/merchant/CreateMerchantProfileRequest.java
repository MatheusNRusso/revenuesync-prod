package com.mtnrs.revenuesync.dto.merchant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record CreateMerchantProfileRequest(

        @NotBlank(message = "Business name is required")
        @Size(min = 2, max = 100, message = "Business name must be between 2 and 100 characters")
        String name,

        @NotBlank(message = "Business email is required")
        @Email(message = "Invalid email address")
        String email,

        @Size(max = 500, message = "Description must be at most 500 characters")
        String description,

        String avatarUrl,

        @NotBlank(message = "Wallet address is required")
        @Pattern(
            regexp = "^[1-9A-HJ-NP-Za-km-z]{32,44}$",
            message = "Invalid Solana wallet address (must be base58, 32-44 characters)"
        )
        String walletAddress,

        @Positive(message = "Service price must be positive")
        BigDecimal defaultAmountSol
) {}
