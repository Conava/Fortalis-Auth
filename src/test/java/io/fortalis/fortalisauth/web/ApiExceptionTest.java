package io.fortalis.fortalisauth.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

    @Test
    void badRequest_createsExceptionWithCorrectStatus() {
        var code = "invalid_input";
        var message = "The input is invalid";

        var exception = ApiException.badRequest(code, message);

        assertNotNull(exception);
        assertEquals(HttpStatus.BAD_REQUEST, exception.status);
        assertEquals(code, exception.code);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void unauthorized_createsExceptionWithCorrectStatus() {
        var code = "invalid_credentials";
        var message = "Invalid username or password";

        var exception = ApiException.unauthorized(code, message);

        assertNotNull(exception);
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status);
        assertEquals(code, exception.code);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void badRequest_withNullCode_createsException() {
        var exception = ApiException.badRequest(null, "message");

        assertNotNull(exception);
        assertNull(exception.code);
        assertEquals("message", exception.getMessage());
    }

    @Test
    void badRequest_withNullMessage_createsException() {
        var exception = ApiException.badRequest("code", null);

        assertNotNull(exception);
        assertEquals("code", exception.code);
        assertNull(exception.getMessage());
    }

    @Test
    void unauthorized_withEmptyStrings_createsException() {
        var exception = ApiException.unauthorized("", "");

        assertNotNull(exception);
        assertEquals("", exception.code);
        assertEquals("", exception.getMessage());
    }

    @Test
    void exception_isThrowable() {
        var exception = ApiException.badRequest("test", "test message");

        assertThrows(ApiException.class, () -> {
            throw exception;
        });
    }

    @Test
    void exception_preservesStackTrace() {
        ApiException exception = null;
        try {
            throw ApiException.unauthorized("auth_failed", "Authentication failed");
        } catch (ApiException e) {
            exception = e;
        }

        assertNotNull(exception);
        assertNotNull(exception.getStackTrace());
        assertTrue(exception.getStackTrace().length > 0);
    }

    @Test
    void badRequest_status400() {
        var exception = ApiException.badRequest("error", "message");
        assertEquals(400, exception.status.value());
    }

    @Test
    void unauthorized_status401() {
        var exception = ApiException.unauthorized("error", "message");
        assertEquals(401, exception.status.value());
    }

    @Test
    void differentExceptions_areDistinct() {
        var ex1 = ApiException.badRequest("code1", "message1");
        var ex2 = ApiException.badRequest("code2", "message2");

        assertNotSame(ex1, ex2);
        assertNotEquals(ex1.code, ex2.code);
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
    }
}
