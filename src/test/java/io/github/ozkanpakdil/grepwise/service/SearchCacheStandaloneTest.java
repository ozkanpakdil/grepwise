package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Standalone test for the search cache functionality.
 * This test verifies that the caching mechanism improves search performance.
 * It does not use Spring Boot or any Spring context, avoiding any conflicts with other beans.
 */
public class SearchCacheStandaloneTest {

    private LuceneService luceneService;
    private SearchCacheService searchCacheService;

    @BeforeEach
    void setUp() {
        // Create instances of the services manually
        luceneService = new LuceneService();
        searchCacheService = new SearchCacheService();
        
        // Set up the SearchCacheService in the LuceneService
        luceneService.setSearchCacheService(searchCacheService);
        
        // Configure sane defaults since @Value isn't processed in standalone tests
        searchCacheService.setMaxCacheSize(100);
        searchCacheService.setExpirationMs(300000); // 5 minutes
        
        // Clear the cache before each test
        searchCacheService.clearCache();
    }

    /**
     * Test that verifies the cache is correctly invalidated when cleared.
     * This test focuses on cache statistics rather than actual search results.
     */
    @Test
    void testCacheInvalidation() throws IOException {
        // Clear the cache at the beginning to start with a clean state
        searchCacheService.clearCache();
        // Ensure cache is enabled for this standalone test (no Spring @Value processing)
        searchCacheService.setCacheEnabled(true);
        
        // Get initial cache statistics
        Map<String, Object> initialStats = searchCacheService.getCacheStats();
        long initialHits = (long) initialStats.get("hits");
        long initialMisses = (long) initialStats.get("misses");
        
        // Use a unique query to avoid interference from other tests
        String query = "unique_test_query_" + System.currentTimeMillis();
        boolean isRegex = false;
        long startTime = System.currentTimeMillis() - 3600000; // 1 hour ago
        long endTime = System.currentTimeMillis();

        // Mock the search results since we're not using a real index
        List<LogEntry> mockResults = new ArrayList<>();
        mockResults.add(new LogEntry("log3", System.currentTimeMillis(), "INFO", "test message 1", "test", new HashMap<>(), "test message 1"));
        
        // Mock the LuceneService.search method to return the mock results and interact with cache
        luceneService = new LuceneService() {
            @Override
            public List<LogEntry> search(String queryStr, boolean isRegex, Long startTime, Long endTime) {
                List<LogEntry> cached = searchCacheService.getFromCache(queryStr, isRegex, startTime, endTime);
                if (cached != null) {
                    return new ArrayList<>(cached);
                }
                List<LogEntry> results = new ArrayList<>(mockResults);
                searchCacheService.addToCache(queryStr, isRegex, startTime, endTime, results);
                return results;
            }
        };
        // Ensure the overridden service shares the same cache instance
        luceneService.setSearchCacheService(searchCacheService);

        // First search (cache miss)
        List<LogEntry> results = luceneService.search(query, isRegex, startTime, endTime);
        
        // Search again (cache hit)
        results = luceneService.search(query, isRegex, startTime, endTime);
        
        // Verify cache statistics have increased
        Map<String, Object> stats = searchCacheService.getCacheStats();
        long currentHits = (long) stats.get("hits");
        long currentMisses = (long) stats.get("misses");
        
        assertEquals(initialHits + 1, currentHits, "Cache hits should have increased by 1");
        assertEquals(initialMisses + 1, currentMisses, "Cache misses should have increased by 1");

        // Record the current statistics
        long hitsBeforeClear = currentHits;
        
        // Clear the cache
        searchCacheService.clearCache();

        // Search again (cache miss after clearing)
        // Call luceneService.search directly to avoid double-counting a miss
        results = luceneService.search(query, isRegex, startTime, endTime);
        
        // Verify updated cache statistics
        stats = searchCacheService.getCacheStats();
        currentHits = (long) stats.get("hits");
        currentMisses = (long) stats.get("misses");
        
        // Hits should not have increased after clearing the cache
        assertEquals(hitsBeforeClear, currentHits, "Cache hits should not increase after clearing");
        assertEquals(initialMisses + 2, currentMisses, "Cache misses should have increased by 2 total");
    }
}