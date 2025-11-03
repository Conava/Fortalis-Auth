package io.fortalis.fortalisauth.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RefreshTokenTest {

    @Test
    void prePersist_setsIssuedTs_whenNull() {
        var token = new RefreshToken();
        assertNull(token.getIssuedTs());

        token.prePersist();

        assertNotNull(token.getIssuedTs());
        assertTrue(token.getIssuedTs().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(token.getIssuedTs().isAfter(Instant.now().minusSeconds(1)));
    }

    @Test
    void prePersist_doesNotOverwriteExistingIssuedTs() {
        var token = new RefreshToken();
        var existingTimestamp = Instant.now().minusSeconds(7200);
        token.setIssuedTs(existingTimestamp);

        token.prePersist();

        assertEquals(existingTimestamp, token.getIssuedTs());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        var token = new RefreshToken();
        var id = UUID.randomUUID();
        var accountId = UUID.randomUUID();
        var tokenHash = "hashed_token_value";
        var issuedTs = Instant.now();
        var expiresTs = Instant.now().plusSeconds(86400);
        var userAgent = "Mozilla/5.0";
        var deviceLabel = "Chrome on Windows";

        token.setId(id);
        token.setAccountId(accountId);
        token.setTokenHash(tokenHash);
        token.setIssuedTs(issuedTs);
        token.setExpiresTs(expiresTs);
        token.setUserAgent(userAgent);
        token.setDeviceLabel(deviceLabel);
        token.setRevoked(true);

        assertEquals(id, token.getId());
        assertEquals(accountId, token.getAccountId());
        assertEquals(tokenHash, token.getTokenHash());
        assertEquals(issuedTs, token.getIssuedTs());
        assertEquals(expiresTs, token.getExpiresTs());
        assertEquals(userAgent, token.getUserAgent());
        assertEquals(deviceLabel, token.getDeviceLabel());
        assertTrue(token.isRevoked());
    }

    @Test
    void revoked_defaultsToFalse() {
        var token = new RefreshToken();
        assertFalse(token.isRevoked());
    }

    @Test
    void tokenHash_canStoreHash() {
        var token = new RefreshToken();
        var hash = "sha256:abcdef1234567890";

        token.setTokenHash(hash);

        assertEquals(hash, token.getTokenHash());
    }

    @Test
    void expiresTs_canBeSetInFuture() {
        var token = new RefreshToken();
        var futureExpiry = Instant.now().plusSeconds(2592000); // 30 days

        token.setExpiresTs(futureExpiry);

        assertEquals(futureExpiry, token.getExpiresTs());
        assertTrue(token.getExpiresTs().isAfter(Instant.now()));
    }

    @Test
    void userAgent_canStoreVariousFormats() {
        var token = new RefreshToken();
        var userAgents = new String[]{
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
                "curl/7.68.0",
                "PostmanRuntime/7.26.8",
                null
        };

        for (var ua : userAgents) {
            token.setUserAgent(ua);
            assertEquals(ua, token.getUserAgent());
        }
    }

    @Test
    void deviceLabel_canBeNull() {
        var token = new RefreshToken();
        token.setDeviceLabel(null);

        assertNull(token.getDeviceLabel());
    }

    @Test
    void accountId_isRequired() {
        var token = new RefreshToken();
        var accountId = UUID.randomUUID();

        token.setAccountId(accountId);

        assertNotNull(token.getAccountId());
        assertEquals(accountId, token.getAccountId());
    }

    @Test
    void multipleTokens_canHaveDifferentProperties() {
        var token1 = new RefreshToken();
        token1.setAccountId(UUID.randomUUID());
        token1.setTokenHash("hash1");
        token1.setRevoked(false);

        var token2 = new RefreshToken();
        token2.setAccountId(UUID.randomUUID());
        token2.setTokenHash("hash2");
        token2.setRevoked(true);

        assertNotEquals(token1.getAccountId(), token2.getAccountId());
        assertNotEquals(token1.getTokenHash(), token2.getTokenHash());
        assertNotEquals(token1.isRevoked(), token2.isRevoked());
    }
}
