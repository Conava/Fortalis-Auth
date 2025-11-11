package io.fortalis.fortalisauth.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class ApiExceptionTest {

    @Test
    void badRequest_createsExceptionWithCorrectStatus() {
        var type = "invalid_input";
        var message = "The input is invalid";

        var exception = ApiException.badRequest(type, message);

        assertNotNull(exception);
        assertEquals(HttpStatus.BAD_REQUEST, exception.status);
        assertEquals(type, exception.type);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void unauthorized_createsExceptionWithCorrectStatus() {
        var type = "invalid_credentials";
        var message = "Invalid username or password";

        var exception = ApiException.unauthorized(type, message);

        assertNotNull(exception);
        assertEquals(HttpStatus.UNAUTHORIZED, exception.status);
        assertEquals(type, exception.type);
        assertEquals(message, exception.getMessage());
    }

    @Test
    void forbidden_createsExceptionWithCorrectStatus() {
        var exception = ApiException.forbidden("access_denied", "Access denied");

        assertNotNull(exception);
        assertEquals(HttpStatus.FORBIDDEN, exception.status);
        assertEquals("access_denied", exception.type);
    }

    @Test
    void notFound_createsExceptionWithCorrectStatus() {
        var exception = ApiException.notFound("resource_not_found", "Resource not found");

        assertNotNull(exception);
        assertEquals(HttpStatus.NOT_FOUND, exception.status);
        assertEquals("resource_not_found", exception.type);
    }

    @Test
    void conflict_createsExceptionWithCorrectStatus() {
        var exception = ApiException.conflict("duplicate_entry", "Entry already exists");

        assertNotNull(exception);
        assertEquals(HttpStatus.CONFLICT, exception.status);
        assertEquals("duplicate_entry", exception.type);
    }

    @Test
    void tooManyRequests_createsExceptionWithCorrectStatus() {
        var exception = ApiException.tooManyRequests("rate_limit", "Too many requests");

        assertNotNull(exception);
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, exception.status);
        assertEquals("rate_limit", exception.type);
    }

    @Test
    void internalServerError_createsExceptionWithCorrectStatus() {
        var exception = ApiException.internalServerError("server_error", "Internal error");

        assertNotNull(exception);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, exception.status);
        assertEquals("server_error", exception.type);
    }

    @Test
    void toProblemDetail_createsCorrectStructure() {
        var exception = ApiException.badRequest("validation_error", "Invalid input");
        var problemDetail = exception.toProblemDetail("/api/test");

        assertNotNull(problemDetail);
        assertEquals(400, problemDetail.getStatus());
        assertEquals("Invalid input", problemDetail.getDetail());
        assertEquals("Bad Request", problemDetail.getTitle());
        assertTrue(problemDetail.getType().toString().contains("validation_error"));
        assertTrue(problemDetail.getInstance().toString().contains("/api/test"));
    }

    @Test
    void toProblemDetail_withNullPath_stillWorks() {
        var exception = ApiException.unauthorized("auth_failed", "Authentication failed");
        var problemDetail = exception.toProblemDetail(null);

        assertNotNull(problemDetail);
        assertEquals(401, problemDetail.getStatus());
        assertNull(problemDetail.getInstance());
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
    void differentExceptions_areDistinct() {
        var ex1 = ApiException.badRequest("type1", "message1");
        var ex2 = ApiException.badRequest("type2", "message2");

        assertNotSame(ex1, ex2);
        assertNotEquals(ex1.type, ex2.type);
        assertNotEquals(ex1.getMessage(), ex2.getMessage());
    }
}
