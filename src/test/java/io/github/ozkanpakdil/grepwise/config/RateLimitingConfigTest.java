package io.github.ozkanpakdil.grepwise.config;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimitingConfigTest {

    private RateLimitingConfig rateLimitingConfig;

    @BeforeEach
    void setUp() {
        rateLimitingConfig = new RateLimitingConfig();
    }

    @Test
    public void testDefaultBucketCreation() {
        // When
        Bucket bucket = rateLimitingConfig.resolveBucket("client1", "default");
        
        // Then
        assertNotNull(bucket, "Default bucket should not be null");
        
        // Verify bucket has correct capacity by consuming tokens
        assertTrue(bucket.tryConsume(1), "Should be able to consume 1 token");
        assertTrue(bucket.tryConsume(50), "Should be able to consume 50 tokens");
        assertTrue(bucket.tryConsume(49), "Should be able to consume 49 tokens");
        assertFalse(bucket.tryConsume(1), "Should not be able to consume more tokens than capacity");
    }

    @Test
    public void testSearchBucketCreation() {
        // When
        Bucket bucket = rateLimitingConfig.resolveBucket("client1", "search");
        
        // Then
        assertNotNull(bucket, "Search bucket should not be null");
        
        // Verify bucket has correct capacity by consuming tokens
        assertTrue(bucket.tryConsume(1), "Should be able to consume 1 token");
        assertTrue(bucket.tryConsume(150), "Should be able to consume 150 tokens");
        assertTrue(bucket.tryConsume(149), "Should be able to consume 149 tokens");
        assertFalse(bucket.tryConsume(1), "Should not be able to consume more tokens than capacity");
    }

    @Test
    public void testAdminBucketCreation() {
        // When
        Bucket bucket = rateLimitingConfig.resolveBucket("client1", "admin");
        
        // Then
        assertNotNull(bucket, "Admin bucket should not be null");
        
        // Verify bucket has correct capacity by consuming tokens (current config: 60/min)
        assertTrue(bucket.tryConsume(1), "Should be able to consume 1 token");
        assertTrue(bucket.tryConsume(30), "Should be able to consume 30 tokens");
        assertTrue(bucket.tryConsume(29), "Should be able to consume 29 tokens");
        assertFalse(bucket.tryConsume(1), "Should not be able to consume more tokens than capacity");
    }

    @Test
    public void testResolveBucket_CachingSameClient() {
        // When
        Bucket result1 = rateLimitingConfig.resolveBucket("client1", "default");
        Bucket result2 = rateLimitingConfig.resolveBucket("client1", "default");

        // Then: same client and type should return same instance (cached)
        assertSame(result1, result2, "Should return cached bucket for same client and type");
    }

    @Test
    public void testResolveBucket_DifferentClients() {
        // When
        Bucket result1 = rateLimitingConfig.resolveBucket("client1", "default");
        Bucket result2 = rateLimitingConfig.resolveBucket("client2", "default");

        // Then: different clients should get different bucket instances
        assertNotSame(result1, result2, "Different clients should get different bucket instances");
    }

    @Test
    public void testResolveBucket_DifferentTypesSameClient() {
        // When
        Bucket defaultBucket = rateLimitingConfig.resolveBucket("client1", "default");
        Bucket searchBucket = rateLimitingConfig.resolveBucket("client1", "search");

        // Then: different types for same client should create different buckets
        assertNotSame(defaultBucket, searchBucket, "Different bucket types should produce different buckets for same client");
    }
}