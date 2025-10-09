package io.fortalis.fortalisauth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto")
public record CryptoProperties(
        String mfaKeyId,          // e.g., "v1"
        String mfaEncryptionKey   // base64-encoded 256-bit key
) {
}
