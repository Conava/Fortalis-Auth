package io.fortalis.fortalisauth.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void constructor_createsRecordWithValues() {
        var error = "invalid_input";
        var message = "The input provided is invalid";

        var response = new ErrorResponse(error, message);

        assertEquals(error, response.error());
        assertEquals(message, response.message());
    }

    @Test
    void constructor_handlesNullValues() {
        var response = new ErrorResponse(null, null);

        assertNull(response.error());
        assertNull(response.message());
    }

    @Test
    void equals_sameValues_returnsTrue() {
        var response1 = new ErrorResponse("error1", "message1");
        var response2 = new ErrorResponse("error1", "message1");

        assertEquals(response1, response2);
    }

    @Test
    void equals_differentValues_returnsFalse() {
        var response1 = new ErrorResponse("error1", "message1");
        var response2 = new ErrorResponse("error2", "message2");

        assertNotEquals(response1, response2);
    }

    @Test
    void hashCode_sameValues_returnsSameHash() {
        var response1 = new ErrorResponse("error", "message");
        var response2 = new ErrorResponse("error", "message");

        assertEquals(response1.hashCode(), response2.hashCode());
    }

    @Test
    void toString_containsFieldValues() {
        var response = new ErrorResponse("test_error", "test message");
        var toString = response.toString();

        assertTrue(toString.contains("test_error"));
        assertTrue(toString.contains("test message"));
    }
}
