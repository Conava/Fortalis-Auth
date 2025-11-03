package io.fortalis.fortalisauth.service;

import io.fortalis.fortalisauth.web.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterServiceTest {

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService();
    }

    @Test
    void checkAndConsume_allowsRequestsUnderLimit() {
        var key = "test-key";
        var maxAttempts = 3;
        var windowSeconds = 60;

        assertDoesNotThrow(() -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));
        assertDoesNotThrow(() -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));
        assertDoesNotThrow(() -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));
    }

    @Test
    void checkAndConsume_throwsWhenLimitExceeded() {
        var key = "rate-limited-key";
        var maxAttempts = 2;
        var windowSeconds = 60;

        rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds);
        rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds);

        var exception = assertThrows(ApiException.class,
                () -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));

        assertEquals("rate_limited", exception.code);
        assertTrue(exception.getMessage().contains("Too many attempts"));
    }

    @Test
    void checkAndConsume_differentKeysAreIndependent() {
        var key1 = "user-1";
        var key2 = "user-2";
        var maxAttempts = 2;
        var windowSeconds = 60;

        rateLimiterService.checkAndConsume(key1, maxAttempts, windowSeconds);
        rateLimiterService.checkAndConsume(key1, maxAttempts, windowSeconds);

        // key2 should still work
        assertDoesNotThrow(() -> rateLimiterService.checkAndConsume(key2, maxAttempts, windowSeconds));
        assertDoesNotThrow(() -> rateLimiterService.checkAndConsume(key2, maxAttempts, windowSeconds));
    }

    @Test
    void clear_removesRateLimit() {
        var key = "clear-test";
        var maxAttempts = 1;
        var windowSeconds = 60;

        rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds);

        assertThrows(ApiException.class,
                () -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));

        rateLimiterService.clear(key);

        // Should work again after clear
        assertDoesNotThrow(() -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));
    }

    @Test
    void clear_onNonExistentKey_doesNotThrow() {
        assertDoesNotThrow(() -> rateLimiterService.clear("non-existent-key"));
    }

    @Test
    void checkAndConsume_resetsAfterWindow() throws InterruptedException {
        var key = "time-window-test";
        var maxAttempts = 1;
        var windowSeconds = 1; // 1 second window

        rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds);

        // Should be rate limited immediately
        assertThrows(ApiException.class,
                () -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));

        // Wait for window to expire
        Thread.sleep(1100);

        // Should work again after window expires
        assertDoesNotThrow(() -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));
    }

    @Test
    void checkAndConsume_withZeroAttempts_alwaysBlocks() {
        var key = "zero-attempts";
        var maxAttempts = 0;
        var windowSeconds = 60;

        assertThrows(ApiException.class,
                () -> rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds));
    }

    @Test
    void checkAndConsume_concurrentRequests_respectsLimit() throws InterruptedException {
        var key = "concurrent-test";
        var maxAttempts = 5;
        var windowSeconds = 60;

        var successCount = new java.util.concurrent.atomic.AtomicInteger(0);
        var failureCount = new java.util.concurrent.atomic.AtomicInteger(0);

        var threads = new Thread[10];
        for (int i = 0; i < 10; i++) {
            threads[i] = new Thread(() -> {
                try {
                    rateLimiterService.checkAndConsume(key, maxAttempts, windowSeconds);
                    successCount.incrementAndGet();
                } catch (ApiException e) {
                    failureCount.incrementAndGet();
                }
            });
            threads[i].start();
        }

        for (var thread : threads) {
            thread.join();
        }

        assertEquals(maxAttempts, successCount.get(), "Should allow exactly maxAttempts");
        assertEquals(10 - maxAttempts, failureCount.get(), "Remaining should be blocked");
    }
}
