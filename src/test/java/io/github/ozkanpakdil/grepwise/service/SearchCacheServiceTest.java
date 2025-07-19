package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SearchCacheServiceTest {

    private SearchCacheService searchCacheService;
    private List<LogEntry> testLogEntries;

    @BeforeEach
    void setUp() {
        searchCacheService = new SearchCacheService();
        ReflectionTestUtils.setField(searchCacheService, "maxCacheSize", 10);
        ReflectionTestUtils.setField(searchCacheService, "expirationMs", 1000);
        ReflectionTestUtils.setField(searchCacheService, "cacheEnabled", true);
        searchCacheService.init();

        // Create test log entries
        testLogEntries = new ArrayList<>();
        testLogEntries.add(new LogEntry("1", 1000L, "ERROR", "Test error message", "test.log", null, "Raw content 1"));
        testLogEntries.add(new LogEntry("2", 2000L, "INFO", "Test info message", "test.log", null, "Raw content 2"));
    }

    @Test
    void testCacheHit() {
        // Add to cache
        searchCacheService.addToCache("error", false, 0L, 3000L, testLogEntries);
        
        // Get from cache - should be a hit
        List<LogEntry> cachedEntries = searchCacheService.getFromCache("error", false, 0L, 3000L);
        
        // Verify
        assertNotNull(cachedEntries);
        assertEquals(2, cachedEntries.size());
        assertEquals("1", cachedEntries.get(0).id());
        
        // Check stats
        Map<String, Object> stats = searchCacheService.getCacheStats();
        assertEquals(1L, stats.get("hits"));
        assertEquals(0L, stats.get("misses"));
    }

    @Test
    void testCacheMiss() {
        // Get from cache without adding - should be a miss
        List<LogEntry> cachedEntries = searchCacheService.getFromCache("error", false, 0L, 3000L);
        
        // Verify
        assertNull(cachedEntries);
        
        // Check stats
        Map<String, Object> stats = searchCacheService.getCacheStats();
        assertEquals(0L, stats.get("hits"));
        assertEquals(1L, stats.get("misses"));
    }

    @Test
    void testCacheExpiration() throws InterruptedException {
        // Set a very short expiration time
        searchCacheService.setExpirationMs(100);
        
        // Add to cache
        searchCacheService.addToCache("error", false, 0L, 3000L, testLogEntries);
        
        // Wait for expiration
        Thread.sleep(200);
        
        // Get from cache - should be a miss due to expiration
        List<LogEntry> cachedEntries = searchCacheService.getFromCache("error", false, 0L, 3000L);
        
        // Verify
        assertNull(cachedEntries);
        
        // Check stats
        Map<String, Object> stats = searchCacheService.getCacheStats();
        assertEquals(0L, stats.get("hits"));
        assertEquals(1L, stats.get("misses"));
        assertEquals(1L, stats.get("evictions"));
    }

    @Test
    void testCacheEviction() {
        // Set a small cache size
        searchCacheService.setMaxCacheSize(2);
        
        // Add entries to fill the cache
        searchCacheService.addToCache("query1", false, 0L, 3000L, testLogEntries);
        searchCacheService.addToCache("query2", false, 0L, 3000L, testLogEntries);
        
        // Add one more entry to trigger eviction
        searchCacheService.addToCache("query3", false, 0L, 3000L, testLogEntries);
        
        // Verify the first entry was evicted
        List<LogEntry> cachedEntries = searchCacheService.getFromCache("query1", false, 0L, 3000L);
        assertNull(cachedEntries);
        
        // But the newer entries are still there
        cachedEntries = searchCacheService.getFromCache("query2", false, 0L, 3000L);
        assertNotNull(cachedEntries);
        
        cachedEntries = searchCacheService.getFromCache("query3", false, 0L, 3000L);
        assertNotNull(cachedEntries);
    }

    @Test
    void testCacheDisabled() {
        // Disable cache
        searchCacheService.setCacheEnabled(false);
        
        // Add to cache
        searchCacheService.addToCache("error", false, 0L, 3000L, testLogEntries);
        
        // Get from cache - should be a miss because cache is disabled
        List<LogEntry> cachedEntries = searchCacheService.getFromCache("error", false, 0L, 3000L);
        
        // Verify
        assertNull(cachedEntries);
    }

    @Test
    void testClearCache() {
        // Add to cache
        searchCacheService.addToCache("query1", false, 0L, 3000L, testLogEntries);
        searchCacheService.addToCache("query2", false, 0L, 3000L, testLogEntries);
        
        // Clear cache
        searchCacheService.clearCache();
        
        // Verify cache is empty
        List<LogEntry> cachedEntries = searchCacheService.getFromCache("query1", false, 0L, 3000L);
        assertNull(cachedEntries);
        
        cachedEntries = searchCacheService.getFromCache("query2", false, 0L, 3000L);
        assertNull(cachedEntries);
        
        // Check stats
        Map<String, Object> stats = searchCacheService.getCacheStats();
        assertEquals(0, stats.get("size"));
        assertEquals(2L, stats.get("evictions"));
    }

    @Test
    void testCacheStats() {
        // Add to cache
        searchCacheService.addToCache("query1", false, 0L, 3000L, testLogEntries);
        
        // Get from cache - hit
        searchCacheService.getFromCache("query1", false, 0L, 3000L);
        
        // Get from cache - miss
        searchCacheService.getFromCache("query2", false, 0L, 3000L);
        
        // Check stats
        Map<String, Object> stats = searchCacheService.getCacheStats();
        assertEquals(true, stats.get("enabled"));
        assertEquals(1, stats.get("size"));
        assertEquals(10, stats.get("maxSize"));
        assertEquals(1000, stats.get("expirationMs"));
        assertEquals(1L, stats.get("hits"));
        assertEquals(1L, stats.get("misses"));
        assertEquals(0L, stats.get("evictions"));
        assertEquals(0.5, stats.get("hitRatio"));
    }
}