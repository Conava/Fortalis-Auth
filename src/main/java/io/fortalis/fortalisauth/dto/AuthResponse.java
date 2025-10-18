package io.fortalis.fortalisauth.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String displayName,
        boolean mfaEnabled
) {
}
