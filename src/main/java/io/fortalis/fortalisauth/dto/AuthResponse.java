package io.fortalis.fortalisauth.dto;

import java.util.UUID;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresInSeconds,
        String displayName,
        boolean mfaEnabled
) {
}
