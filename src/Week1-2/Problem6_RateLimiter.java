import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Problem 6: Distributed Rate Limiter for API Gateway
 * Concepts: Hash table for client tracking, time-based operations, token bucket algorithm
 */
public class Problem6_RateLimiter {

    // Token Bucket per client
    private static class TokenBucket {
        final AtomicLong tokens;          // current token count
        volatile long lastRefillTime;     // epoch ms
        final int maxTokens;
        final int refillRatePerHour;

        TokenBucket(int maxTokens) {
            this.maxTokens = maxTokens;
            this.refillRatePerHour = maxTokens;
            this.tokens = new AtomicLong(maxTokens);
            this.lastRefillTime = System.currentTimeMillis();
        }

        // Refill tokens if an hour has passed
        synchronized void refillIfNeeded() {
            long now = System.currentTimeMillis();
            long elapsed = now - lastRefillTime;

            if (elapsed >= 3600_000L) { // 1 hour in ms
                tokens.set(maxTokens);
                lastRefillTime = now;
            } else {
                // Partial refill: proportional to elapsed time
                long newTokens = (elapsed * refillRatePerHour) / 3600_000L;
                if (newTokens > 0) {
                    tokens.set(Math.min(maxTokens, tokens.get() + newTokens));
                    lastRefillTime = now;
                }
            }
        }

        // Try to consume 1 token. Returns true if allowed.
        boolean tryConsume() {
            refillIfNeeded();
            return tokens.getAndUpdate(t -> t > 0 ? t - 1 : t) > 0;
        }

        long secondsUntilReset() {
            long elapsed = System.currentTimeMillis() - lastRefillTime;
            return Math.max(0, (3600_000L - elapsed) / 1000);
        }

        long getUsed() {
            return maxTokens - tokens.get();
        }
    }

    private static final int DEFAULT_LIMIT = 1000; // requests per hour

    // HashMap<clientId, TokenBucket>
    private final ConcurrentHashMap<String, TokenBucket> clientBuckets = new ConcurrentHashMap<>();

    // Custom limits per client (premium clients)
    private final ConcurrentHashMap<String, Integer> customLimits = new ConcurrentHashMap<>();

    // Get or create bucket for a client — O(1) amortized
    private TokenBucket getBucket(String clientId) {
        return clientBuckets.computeIfAbsent(clientId,
                k -> new TokenBucket(customLimits.getOrDefault(k, DEFAULT_LIMIT)));
    }

    // Set a custom rate limit for a client
    public void setCustomLimit(String clientId, int limit) {
        customLimits.put(clientId, limit);
    }

    // Main rate limit check — responds within ~1ms
    public RateLimitResult checkRateLimit(String clientId) {
        long start = System.nanoTime();
        TokenBucket bucket = getBucket(clientId);
        boolean allowed = bucket.tryConsume();
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        long remaining = bucket.tokens.get();
        long secondsUntilReset = bucket.secondsUntilReset();

        if (allowed) {
            System.out.printf("checkRateLimit(\"%s\") → Allowed (%d requests remaining) [%dms]%n",
                    clientId, remaining, elapsedMs);
        } else {
            System.out.printf("checkRateLimit(\"%s\") → Denied (0 remaining, retry after %ds) [%dms]%n",
                    clientId, secondsUntilReset, elapsedMs);
        }

        return new RateLimitResult(allowed, remaining, secondsUntilReset, elapsedMs);
    }

    // Get current rate limit status for a client
    public void getRateLimitStatus(String clientId) {
        TokenBucket bucket = getBucket(clientId);
        System.out.printf("getRateLimitStatus(\"%s\") → {used: %d, limit: %d, resetIn: %ds}%n",
                clientId, bucket.getUsed(), bucket.maxTokens, bucket.secondsUntilReset());
    }

    // Result object
    record RateLimitResult(boolean allowed, long remaining, long secondsUntilReset, long responseTimeMs) {}

    // Concurrent load test
    public void runLoadTest(String clientId, int numRequests) throws InterruptedException {
        System.out.println("\n--- Load Test: " + numRequests + " concurrent requests for " + clientId + " ---");
        ExecutorService executor = Executors.newFixedThreadPool(50);
        AtomicInteger allowed = new AtomicInteger(0);
        AtomicInteger denied = new AtomicInteger(0);
        CountDownLatch latch = new CountDownLatch(numRequests);

        for (int i = 0; i < numRequests; i++) {
            executor.submit(() -> {
                TokenBucket bucket = getBucket(clientId);
                if (bucket.tryConsume()) allowed.incrementAndGet();
                else denied.incrementAndGet();
                latch.countDown();
            });
        }

        latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        System.out.println("Allowed: " + allowed.get() + ", Denied: " + denied.get());
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Problem 6: Distributed Rate Limiter ===\n");

        Problem6_RateLimiter limiter = new Problem6_RateLimiter();

        // Standard client
        limiter.checkRateLimit("abc123");
        limiter.checkRateLimit("abc123");

        // Exhaust remaining tokens quickly for demo
        TokenBucket bucket = limiter.getBucket("abc123");
        bucket.tokens.set(0); // simulate all tokens consumed

        limiter.checkRateLimit("abc123"); // Should be Denied

        limiter.getRateLimitStatus("abc123");

        // Premium client with higher limit
        limiter.setCustomLimit("premium_client", 10000);
        limiter.checkRateLimit("premium_client");

        // Concurrent load test with 1500 requests (limit is 1000)
        limiter.runLoadTest("load_test_client", 1500);
    }
}
