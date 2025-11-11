package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.web.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimiterService {
    private static class Bucket {
        int count;
        long resetEpoch;
    }

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public void checkAndConsume(String key, int maxAttempts, int windowSeconds) {
        long now = Instant.now().getEpochSecond();
        Bucket b = buckets.computeIfAbsent(key, k -> new Bucket());
        synchronized (b) {
            if (b.resetEpoch <= now) {
                b.count = 0;
                b.resetEpoch = now + windowSeconds;
            }
            if (b.count >= maxAttempts) {
                long retryAfter = b.resetEpoch - now;
                throw new RateLimitExceededException(
                        "rate-limit-exceeded",
                        "Too many requests. Please try again later.",
                        retryAfter
                );
            }
            b.count++;
        }
    }

    public void clear(String key) {
        buckets.remove(key);
    }
}
