package io.fortalis.fortalisauth.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        var factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void constructor_createsRecordWithAllValues() {
        var emailOrUsername = "test@example.com";
        var password = "password123";
        var mfaCode = "123456";

        var request = new LoginRequest(emailOrUsername, password, mfaCode);

        assertEquals(emailOrUsername, request.emailOrUsername());
        assertEquals(password, request.password());
        assertEquals(mfaCode, request.mfaCode());
    }

    @Test
    void constructor_mfaCode_isOptional() {
        var request = new LoginRequest("test@example.com", "password123", null);

        assertNull(request.mfaCode());
    }

    @Test
    void validation_validRequest_noViolations() {
        var request = new LoginRequest("test@example.com", "password123", null);

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_validRequestWithMfa_noViolations() {
        var request = new LoginRequest("test@example.com", "password123", "123456");

        var violations = validator.validate(request);

        assertTrue(violations.isEmpty());
    }

    @Test
    void validation_blankEmailOrUsername_hasViolation() {
        var request = new LoginRequest("", "password123", null);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("emailOrUsername")));
    }

    @Test
    void validation_nullEmailOrUsername_hasViolation() {
        var request = new LoginRequest(null, "password123", null);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("emailOrUsername")));
    }

    @Test
    void validation_blankPassword_hasViolation() {
        var request = new LoginRequest("test@example.com", "", null);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void validation_nullPassword_hasViolation() {
        var request = new LoginRequest("test@example.com", null, null);

        var violations = validator.validate(request);

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().equals("password")));
    }

    @Test
    void validation_emptyMfaCode_isValid() {
        var request = new LoginRequest("test@example.com", "password123", "");

        var violations = validator.validate(request);

        // Empty mfaCode is allowed since it's optional
        assertTrue(violations.stream().noneMatch(v -> v.getPropertyPath().toString().equals("mfaCode")));
    }

    @Test
    void emailOrUsername_acceptsEmail() {
        var request = new LoginRequest("user@example.com", "password123", null);

        assertEquals("user@example.com", request.emailOrUsername());
    }

    @Test
    void emailOrUsername_acceptsUsername() {
        var request = new LoginRequest("username", "password123", null);

        assertEquals("username", request.emailOrUsername());
    }

    @Test
    void equals_sameValues_returnsTrue() {
        var request1 = new LoginRequest("test@example.com", "password123", "123456");
        var request2 = new LoginRequest("test@example.com", "password123", "123456");

        assertEquals(request1, request2);
    }

    @Test
    void equals_differentValues_returnsFalse() {
        var request1 = new LoginRequest("user1@example.com", "password1", "111111");
        var request2 = new LoginRequest("user2@example.com", "password2", "222222");

        assertNotEquals(request1, request2);
    }

    @Test
    void equals_nullMfaCode_consideredEqual() {
        var request1 = new LoginRequest("test@example.com", "password123", null);
        var request2 = new LoginRequest("test@example.com", "password123", null);

        assertEquals(request1, request2);
    }

    @Test
    void hashCode_sameValues_returnsSameHash() {
        var request1 = new LoginRequest("test@example.com", "password123", "123456");
        var request2 = new LoginRequest("test@example.com", "password123", "123456");

        assertEquals(request1.hashCode(), request2.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        var request = new LoginRequest("test@example.com", "password123", "123456");
        var toString = request.toString();

        assertTrue(toString.contains("test@example.com"));
        // Note: Ideally password shouldn't appear in toString for security, but records include all fields
    }
}
