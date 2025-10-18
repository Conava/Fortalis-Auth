package io.fortalis.fortalisauth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * factor: e.g., "TOTP" (future: "WEBAUTHN", "RECOVERY_CODE", ...)
 * For TOTP, set `code`.
 */
public record LoginCompleteRequest(
        @NotBlank String loginTicket,
        @NotBlank String factor,
        String code
) {}
