package io.fortalis.fortalisauth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructor_normalizesEmailToLowercase() {
        var request = new RegisterRequest("TEST@EXAMPLE.COM", "password123", "TestUser");

        assertEquals("test@example.com", request.email());
    }

    @Test
    void constructor_preservesLowercaseEmail() {
        var request = new RegisterRequest("test@example.com", "password123", "TestUser");

        assertEquals("test@example.com", request.email());
    }

    @Test
    void constructor_handlesMixedCaseEmail() {
        var request = new RegisterRequest("TeSt@ExAmPlE.CoM", "password123", "TestUser");

        assertEquals("test@example.com", request.email());
    }

    @Test
    void constructor_doesNotModifyPasswordOrDisplayName() {
        var password = "MyP@ssw0rd";
        var displayName = "John DOE";

        var request = new RegisterRequest("test@example.com", password, displayName);

        assertEquals(password, request.password());
        assertEquals(displayName, request.displayName());
    }

    @Test
    void validation_validRequest_noViolations() {
        var request = new RegisterRequest("test@example.com", "password123", "TestUser");

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_blankEmail_hasViolation() {
        var request = new RegisterRequest("", "password123", "TestUser");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void validation_nullEmail_hasViolation() {
        var request = new RegisterRequest(null, "password123", "TestUser");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void validation_invalidEmailFormat_hasViolation() {
        var request = new RegisterRequest("not-an-email", "password123", "TestUser");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("email")));
    }

    @Test
    void validation_blankPassword_hasViolation() {
        var request = new RegisterRequest("test@example.com", "", "TestUser");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void validation_shortPassword_hasViolation() {
        var request = new RegisterRequest("test@example.com", "short", "TestUser");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> 
                v.getPropertyPath().toString().equals("password") && 
                v.getMessage().contains("size")));
    }

    @Test
    void validation_minimumPasswordLength_valid() {
        var request = new RegisterRequest("test@example.com", "12345678", "TestUser");

        var violations = validator.validate(request);

        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void validation_tooLongPassword_hasViolation() {
        var longPassword = "a".repeat(201);
        var request = new RegisterRequest("test@example.com", longPassword, "TestUser");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void validation_blankDisplayName_hasViolation() {
        var request = new RegisterRequest("test@example.com", "password123", "");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("displayName")));
    }

    @Test
    void validation_shortDisplayName_hasViolation() {
        var request = new RegisterRequest("test@example.com", "password123", "ab");

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("displayName")));
    }

    @Test
    void validation_minimumDisplayNameLength_valid() {
        var request = new RegisterRequest("test@example.com", "password123", "abc");

        var violations = validator.validate(request);

        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("displayName")));
    }

    @Test
    void validation_maximumDisplayNameLength_valid() {
        var longName = "a".repeat(32);
        var request = new RegisterRequest("test@example.com", "password123", longName);

        var violations = validator.validate(request);

        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("displayName")));
    }

    @Test
    void validation_tooLongDisplayName_hasViolation() {
        var longName = "a".repeat(33);
        var request = new RegisterRequest("test@example.com", "password123", longName);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("displayName")));
    }

    @Test
    void equals_sameValues_returnsTrue() {
        var request1 = new RegisterRequest("test@example.com", "password123", "TestUser");
        var request2 = new RegisterRequest("TEST@EXAMPLE.COM", "password123", "TestUser");

        assertEquals(request1, request2, "Emails should be normalized so records are equal");
    }

    @Test
    void constructor_handlesEmailWithSpaces() {
        var request = new RegisterRequest("  TEST@EXAMPLE.COM  ", "password123", "TestUser");

        // Note: trim is not applied, only toLowerCase
        assertEquals("  test@example.com  ", request.email());
    }
}
