package io.fortalis.fortalisauth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthResponseTest {

    @Test
    void constructor_createsRecordWithAllValues() {
        var accessToken = "eyJhbGciOiJSUzI1NiJ9...";
        var refreshToken = "refresh_abc123";
        var expiresIn = 900L;
        var displayName = "Test User";
        var mfaEnabled = true;

        var response = new AuthResponse(accessToken, refreshToken, expiresIn, displayName, mfaEnabled);

        assertEquals(accessToken, response.accessToken());
        assertEquals(refreshToken, response.refreshToken());
        assertEquals(expiresIn, response.expiresInSeconds());
        assertEquals(displayName, response.displayName());
        assertTrue(response.mfaEnabled());
    }

    @Test
    void constructor_handlesNullTokens() {
        var response = new AuthResponse(null, null, 900L, "User", false);

        assertNull(response.accessToken());
        assertNull(response.refreshToken());
    }

    @Test
    void mfaEnabled_canBeFalse() {
        var response = new AuthResponse("token", "refresh", 900L, "User", false);

        assertFalse(response.mfaEnabled());
    }

    @Test
    void expiresInSeconds_acceptsVariousValues() {
        var shortExpiry = new AuthResponse("token", "refresh", 60L, "User", false);
        var longExpiry = new AuthResponse("token", "refresh", 86400L, "User", false);
        var zeroExpiry = new AuthResponse("token", "refresh", 0L, "User", false);

        assertEquals(60L, shortExpiry.expiresInSeconds());
        assertEquals(86400L, longExpiry.expiresInSeconds());
        assertEquals(0L, zeroExpiry.expiresInSeconds());
    }

    @Test
    void equals_sameValues_returnsTrue() {
        var response1 = new AuthResponse("token", "refresh", 900L, "User", true);
        var response2 = new AuthResponse("token", "refresh", 900L, "User", true);

        assertEquals(response1, response2);
    }

    @Test
    void equals_differentValues_returnsFalse() {
        var response1 = new AuthResponse("token1", "refresh1", 900L, "User1", true);
        var response2 = new AuthResponse("token2", "refresh2", 900L, "User2", false);

        assertNotEquals(response1, response2);
    }

    @Test
    void hashCode_sameValues_returnsSameHash() {
        var response1 = new AuthResponse("token", "refresh", 900L, "User", true);
        var response2 = new AuthResponse("token", "refresh", 900L, "User", true);

        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        var response = new AuthResponse("test_token", "test_refresh", 900L, "TestUser", true);
        var toString = response.toString();

        assertTrue(toString.contains("test_token"));
        assertTrue(toString.contains("test_refresh"));
        assertTrue(toString.contains("900"));
        assertTrue(toString.contains("TestUser"));
        assertTrue(toString.contains("true"));
    }

    @Test
    void displayName_canBeNull() {
        var response = new AuthResponse("token", "refresh", 900L, null, false);

        assertNull(response.displayName());
    }
}
