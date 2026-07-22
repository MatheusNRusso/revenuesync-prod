package com.mtnrs.revenuesync.infra.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory rate limiter using Bucket4j.
 * Buckets are keyed by clientIP + rateLimitType to isolate limits per endpoint group.
 * Limits are read from RateLimitConfig (application.yml).
 * For production with multiple instances, consider Redis-backed bucket store.
 */
@Component
public class RateLimiter {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final RateLimitConfig config;

    public RateLimiter(RateLimitConfig config) {
        this.config = config;
    }

    public boolean isAllowed(String clientIp, RateLimitType type) {
        // Composite key: isolates buckets per client AND per rate limit type
        String bucketKey = clientIp + ":" + type.name();
        Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> createBucket(type));
        return bucket.tryConsume(1);
    }

    private Bucket createBucket(RateLimitType type) {
        Bandwidth limit;

        switch (type) {
            case AUTH:
                limit = Bandwidth.classic(
                        config.getAuthEndpoints().getCapacity(),
                        Refill.intervally(config.getAuthEndpoints().getRefillTokens(), Duration.ofMinutes(1))
                );
                break;
            case PUBLIC:
            default:
                limit = Bandwidth.classic(
                        config.getPublicEndpoints().getCapacity(),
                        Refill.intervally(config.getPublicEndpoints().getRefillTokens(), Duration.ofMinutes(1))
                );
                break;
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }


    public void clear() {
        buckets.clear();
    }
}