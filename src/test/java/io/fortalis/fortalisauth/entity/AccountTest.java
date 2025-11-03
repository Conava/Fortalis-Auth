package io.fortalis.fortalisauth.entity;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AccountTest {

    @Test
    void prePersist_setsCreatedTs_whenNull() {
        var account = new Account();
        assertNull(account.getCreatedTs());

        account.prePersist();

        assertNotNull(account.getCreatedTs());
        assertTrue(account.getCreatedTs().isBefore(Instant.now().plusSeconds(1)));
        assertTrue(account.getCreatedTs().isAfter(Instant.now().minusSeconds(1)));
    }

    @Test
    void prePersist_doesNotOverwriteExistingCreatedTs() {
        var account = new Account();
        var existingTimestamp = Instant.now().minusSeconds(3600);
        account.setCreatedTs(existingTimestamp);

        account.prePersist();

        assertEquals(existingTimestamp, account.getCreatedTs());
    }

    @Test
    void prePersist_normalizesEmailToLowercase() {
        var account = new Account();
        account.setEmail("Test@EXAMPLE.COM");

        account.prePersist();

        assertEquals("test@example.com", account.getEmail());
    }

    @Test
    void prePersist_handlesNullEmail() {
        var account = new Account();
        account.setEmail(null);

        assertDoesNotThrow(account::prePersist);
        assertNull(account.getEmail());
    }

    @Test
    void prePersist_handlesEmptyEmail() {
        var account = new Account();
        account.setEmail("");

        account.prePersist();

        assertEquals("", account.getEmail());
    }

    @Test
    void preUpdate_normalizesEmailToLowercase() {
        var account = new Account();
        account.setEmail("UPDATED@EMAIL.COM");

        account.preUpdate();

        assertEquals("updated@email.com", account.getEmail());
    }

    @Test
    void preUpdate_handlesNullEmail() {
        var account = new Account();
        account.setEmail(null);

        assertDoesNotThrow(account::preUpdate);
        assertNull(account.getEmail());
    }

    @Test
    void preUpdate_preservesMixedCaseInNonEmailFields() {
        var account = new Account();
        account.setEmail("Test@Example.COM");
        account.setDisplayName("John DOE");

        account.preUpdate();

        assertEquals("test@example.com", account.getEmail());
        assertEquals("John DOE", account.getDisplayName(), "Display name should not be modified");
    }

    @Test
    void emailNormalization_handlesSpecialCharacters() {
        var account = new Account();
        account.setEmail("Test+Tag@EXAMPLE.COM");

        account.prePersist();

        assertEquals("test+tag@example.com", account.getEmail());
    }

    @Test
    void emailNormalization_handlesUnicode() {
        var account = new Account();
        account.setEmail("Tëst@EXAMPLE.COM");

        account.prePersist();

        assertEquals("tëst@example.com", account.getEmail());
    }

    @Test
    void settersAndGetters_workCorrectly() {
        var account = new Account();
        var email = "test@example.com";
        var passwordHash = "hashed_password";
        var displayName = "Test User";
        var createdTs = Instant.now();

        account.setEmail(email);
        account.setPasswordHash(passwordHash);
        account.setDisplayName(displayName);
        account.setCreatedTs(createdTs);
        account.setEmailVerified(true);

        assertEquals(email, account.getEmail());
        assertEquals(passwordHash, account.getPasswordHash());
        assertEquals(displayName, account.getDisplayName());
        assertEquals(createdTs, account.getCreatedTs());
        assertTrue(account.isEmailVerified());
    }

    @Test
    void emailVerified_defaultsToFalse() {
        var account = new Account();
        assertFalse(account.isEmailVerified());
    }

    @Test
    void id_canBeSetAndRetrieved() {
        var account = new Account();
        var id = java.util.UUID.randomUUID();

        account.setId(id);

        assertEquals(id, account.getId());
    }
}
