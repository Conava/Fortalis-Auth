package io.fortalis.fortalisauth.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

/**
 * Exception thrown when rate limits are exceeded.
 * Includes Retry-After information per RFC 6585.
 */
public class RateLimitExceededException extends RuntimeException {
    private final String type;
    private final long retryAfterSeconds;

    public RateLimitExceededException(String type, String detail, long retryAfterSeconds) {
        super(detail);
        this.type = type;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public String getType() {
        return type;
    }

    public ProblemDetail toProblemDetail(String requestPath) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
            HttpStatus.TOO_MANY_REQUESTS,
            getMessage()
        );
        problem.setType(URI.create("https://auth.fortalis.game/errors/" + type));
        problem.setTitle("Too Many Requests");
        if (requestPath != null) {
            problem.setInstance(URI.create(requestPath));
        }
        problem.setProperty("retryAfter", retryAfterSeconds);
        return problem;
    }
}
