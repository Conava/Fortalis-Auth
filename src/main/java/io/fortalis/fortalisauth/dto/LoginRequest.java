package io.fortalis.fortalisauth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Login with email (case-insensitive) and password.
 * If MFA is enabled, the server may require a TOTP code alongside.
 */
public record LoginRequest(
        @NotBlank String emailOrUsername,
        @NotBlank String password,
        String mfaCode    // optional TOTP code when MFA enabled
) {
}
