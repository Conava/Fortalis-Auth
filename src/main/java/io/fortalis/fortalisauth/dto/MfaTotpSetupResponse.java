package io.fortalis.fortalisauth.dto;

import java.util.List;

/**
 * Returned when initiating TOTP setup; used to render QR in the client.
 */
public record MfaTotpSetupResponse(
        String secretBase32,
        String otpauthUrl,
        List<String> backupCodes
) {
}
