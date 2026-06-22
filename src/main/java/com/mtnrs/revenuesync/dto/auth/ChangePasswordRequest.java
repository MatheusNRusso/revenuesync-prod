package com.mtnrs.revenuesync.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

    String currentPassword,

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    String newPassword,

    @NotBlank
    String confirmPassword
) {}
