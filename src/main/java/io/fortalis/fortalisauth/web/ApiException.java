package io.fortalis.fortalisauth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * API exception that produces RFC 7807 Problem Details responses.
 * Extends RuntimeException to integrate with Spring's exception handling.
 */
public class ApiException extends RuntimeException {
    public final HttpStatus status;
    public final String type;
    private final String instance;

    private ApiException(HttpStatus status, String type, String title, String detail, String instance) {
        super(detail);
        this.status = status;
        this.type = type;
        this.instance = instance;
    }

    /**
     * Creates a ProblemDetail for this exception.
     * @param requestPath the path of the current request
     * @return RFC 7807 Problem Details object
     */
    public ProblemDetail toProblemDetail(String requestPath) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, getMessage());
        problem.setType(URI.create("https://auth.fortalis.game/errors/" + type));
        problem.setTitle(status.getReasonPhrase());
        if (instance != null) {
            problem.setInstance(URI.create(instance));
        } else if (requestPath != null) {
            problem.setInstance(URI.create(requestPath));
        }
        return problem;
    }

    // Factory methods for common error types

    public static ApiException badRequest(String type, String detail) {
        return new ApiException(HttpStatus.BAD_REQUEST, type, "Bad Request", detail, null);
    }

    public static ApiException unauthorized(String type, String detail) {
        return new ApiException(HttpStatus.UNAUTHORIZED, type, "Unauthorized", detail, null);
    }

    public static ApiException forbidden(String type, String detail) {
        return new ApiException(HttpStatus.FORBIDDEN, type, "Forbidden", detail, null);
    }

    public static ApiException notFound(String type, String detail) {
        return new ApiException(HttpStatus.NOT_FOUND, type, "Not Found", detail, null);
    }

    public static ApiException conflict(String type, String detail) {
        return new ApiException(HttpStatus.CONFLICT, type, "Conflict", detail, null);
    }

    public static ApiException tooManyRequests(String type, String detail) {
        return new ApiException(HttpStatus.TOO_MANY_REQUESTS, type, "Too Many Requests", detail, null);
    }

    public static ApiException internalServerError(String type, String detail) {
        return new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, type, "Internal Server Error", detail, null);
    }
}
