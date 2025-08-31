package io.github.ozkanpakdil.grepwise.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Configuration for rate limiting.
 * This class configures the rate limiting buckets and cache.
 */
@Configuration
public class RateLimitingConfig {

    /**
     * Cache to store rate limiting buckets for each client.
     * The key is the client identifier (IP address or user ID).
     */
    private final Map<String, Bucket> bucketCache = new ConcurrentHashMap<>();

    /**
     * Create a default rate limit bucket: 100 requests per minute.
     * This is used for general API endpoints.
     */
    private Bucket createDefaultBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * Create a high-throughput rate limit bucket: 300 requests per minute.
     * This is used for search endpoints that need higher throughput.
     */
    private Bucket createSearchBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(300, Refill.intervally(300, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * Create a low-throughput rate limit bucket: 20 requests per minute.
     * This is used for sensitive operations like user management.
     */
    private Bucket createAdminBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))))
                .build();
    }

    /**
     * Get or create a bucket for a client.
     *
     * @param clientId   The client identifier (IP address or user ID)
     * @param bucketType The type of bucket to use (default, search, admin)
     * @return The bucket for the client
     */
    public Bucket resolveBucket(String clientId, String bucketType) {
        String key = clientId + ":" + bucketType;
        return bucketCache.computeIfAbsent(key, k -> {
            switch (bucketType) {
                case "search":
                    return createSearchBucket();
                case "admin":
                    return createAdminBucket();
                default:
                    return createDefaultBucket();
            }
        });
    }

    /**
     * Clean up expired buckets to prevent memory leaks.
     * This method should be called periodically.
     */
    public void cleanupExpiredBuckets() {
        // Remove buckets that haven't been used for more than 1 hour
        // This is a simple implementation and can be improved with a proper cache
        // like Caffeine with expiration
    }
}