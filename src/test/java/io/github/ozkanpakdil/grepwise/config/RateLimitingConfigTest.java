package io.github.ozkanpakdil.grepwise.config;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateLimitingConfigTest {

    @Spy
    @InjectMocks
    private RateLimitingConfig rateLimitingConfig;

    @Test
    public void testDefaultBucketCreation() {
        // When
        Bucket bucket = rateLimitingConfig.defaultBucket();
        
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
        Bucket bucket = rateLimitingConfig.searchBucket();
        
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
        Bucket bucket = rateLimitingConfig.adminBucket();
        
        // Then
        assertNotNull(bucket, "Admin bucket should not be null");
        
        // Verify bucket has correct capacity by consuming tokens
        assertTrue(bucket.tryConsume(1), "Should be able to consume 1 token");
        assertTrue(bucket.tryConsume(10), "Should be able to consume 10 tokens");
        assertTrue(bucket.tryConsume(9), "Should be able to consume 9 tokens");
        assertFalse(bucket.tryConsume(1), "Should not be able to consume more tokens than capacity");
    }

    @Test
    public void testResolveBucket_Default() {
        // Given
        Bucket defaultBucket = mock(Bucket.class);
        doReturn(defaultBucket).when(rateLimitingConfig).defaultBucket();
        
        // When
        Bucket result = rateLimitingConfig.resolveBucket("client1", "default");
        
        // Then
        assertSame(defaultBucket, result, "Should return default bucket");
        
        // When called again with same client
        Bucket result2 = rateLimitingConfig.resolveBucket("client1", "default");
        
        // Then should return same bucket instance (cached)
        assertSame(result, result2, "Should return cached bucket for same client");
        
        // Verify defaultBucket() was called only once due to caching
        verify(rateLimitingConfig, times(1)).defaultBucket();
    }

    @Test
    public void testResolveBucket_Search() {
        // Given
        Bucket searchBucket = mock(Bucket.class);
        doReturn(searchBucket).when(rateLimitingConfig).searchBucket();
        
        // When
        Bucket result = rateLimitingConfig.resolveBucket("client1", "search");
        
        // Then
        assertSame(searchBucket, result, "Should return search bucket");
        verify(rateLimitingConfig).searchBucket();
    }

    @Test
    public void testResolveBucket_Admin() {
        // Given
        Bucket adminBucket = mock(Bucket.class);
        doReturn(adminBucket).when(rateLimitingConfig).adminBucket();
        
        // When
        Bucket result = rateLimitingConfig.resolveBucket("client1", "admin");
        
        // Then
        assertSame(adminBucket, result, "Should return admin bucket");
        verify(rateLimitingConfig).adminBucket();
    }

    @Test
    public void testResolveBucket_DifferentClients() {
        // Given
        Bucket defaultBucket1 = mock(Bucket.class);
        Bucket defaultBucket2 = mock(Bucket.class);
        
        // First call for client1 returns defaultBucket1
        doReturn(defaultBucket1).when(rateLimitingConfig).defaultBucket();
        
        // When
        Bucket result1 = rateLimitingConfig.resolveBucket("client1", "default");
        
        // Then
        assertSame(defaultBucket1, result1, "Should return first default bucket");
        
        // For second client, return different bucket
        doReturn(defaultBucket2).when(rateLimitingConfig).defaultBucket();
        
        // When
        Bucket result2 = rateLimitingConfig.resolveBucket("client2", "default");
        
        // Then
        assertSame(defaultBucket2, result2, "Should return second default bucket");
        assertNotSame(result1, result2, "Different clients should get different bucket instances");
    }
}