package io.fortalis.fortalisauth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Returned when initiating TOTP setup; used to render QR in the client.
 */
public record MfaTotpSetupResponse(
        @JsonProperty("secret") String secretBase32,
        String otpauthUrl,
        List<String> backupCodes
) {
}
