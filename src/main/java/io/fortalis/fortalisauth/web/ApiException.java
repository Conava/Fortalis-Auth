package io.fortalis.fortalisauth.web;

import org.springframework.http.HttpStatus;

/**
 * Lightweight API exception with a machine code and message.
 */
public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public final String code;

    private ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException badRequest(String code, String msg) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, msg);
    }

    public static ApiException unauthorized(String code, String msg) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, msg);
    }
}
