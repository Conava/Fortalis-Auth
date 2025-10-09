package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.web.ApiException;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimiterService {
    private static class Bucket { int count; long resetEpoch; }
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
                throw ApiException.unauthorized("rate_limited", "Too many attempts. Try later.");
            }
            b.count++;
        }
    }

    public void clear(String key) {
        buckets.remove(key);
    }
}

