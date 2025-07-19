package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the search cache functionality.
 * This test verifies that the caching mechanism improves search performance.
 */
@SpringBootTest
@ActiveProfiles("test")
public class SearchCacheIntegrationTest {

    @Autowired
    private LuceneService luceneService;

    @Autowired
    private SearchCacheService searchCacheService;

    @BeforeEach
    void setUp() {
        // Clear the cache before each test
        searchCacheService.clearCache();
    }

    /**
     * Test that verifies the performance improvement from caching.
     * This test performs the same search multiple times and measures
     * the execution time with and without caching.
     */
    @Test
    void testSearchPerformanceImprovement() throws IOException {
        // Enable caching
        searchCacheService.setCacheEnabled(true);

        // Create a search query
        String query = "test message";
        boolean isRegex = false;
        long startTime = System.currentTimeMillis() - 3600000; // 1 hour ago
        long endTime = System.currentTimeMillis();

        // First search (cache miss)
        long startExecution = System.currentTimeMillis();
        List<LogEntry> firstResults = luceneService.search(query, isRegex, startTime, endTime);
        long firstExecutionTime = System.currentTimeMillis() - startExecution;

        // Second search (cache hit)
        startExecution = System.currentTimeMillis();
        List<LogEntry> secondResults = luceneService.search(query, isRegex, startTime, endTime);
        long secondExecutionTime = System.currentTimeMillis() - startExecution;

        // Verify that the second search was faster
        assertTrue(secondExecutionTime < firstExecutionTime, 
                "Second search (cache hit) should be faster than first search (cache miss)");

        // Verify that the results are the same
        assertEquals(firstResults.size(), secondResults.size(), 
                "Both searches should return the same number of results");

        // Verify cache statistics
        Map<String, Object> stats = searchCacheService.getCacheStats();
        assertEquals(1L, stats.get("hits"), "Cache should have 1 hit");
        assertEquals(1L, stats.get("misses"), "Cache should have 1 miss");

        // Disable caching and verify that searches take similar time
        searchCacheService.setCacheEnabled(false);

        // First search without cache
        startExecution = System.currentTimeMillis();
        luceneService.search(query, isRegex, startTime, endTime);
        long firstUncachedTime = System.currentTimeMillis() - startExecution;

        // Second search without cache
        startExecution = System.currentTimeMillis();
        luceneService.search(query, isRegex, startTime, endTime);
        long secondUncachedTime = System.currentTimeMillis() - startExecution;

        // The times should be similar (within a reasonable margin)
        // We use a 50% margin to account for JVM warmup and other factors
        double ratio = (double) Math.max(firstUncachedTime, secondUncachedTime) / 
                       Math.min(firstUncachedTime, secondUncachedTime);
        
        assertTrue(ratio < 1.5, 
                "Without caching, search times should be similar (within 50% margin)");

        // Re-enable caching for cleanup
        searchCacheService.setCacheEnabled(true);
    }

    /**
     * Test that verifies the cache is correctly invalidated when cleared.
     * This test focuses on cache statistics rather than actual search results.
     */
    @Test
    void testCacheInvalidation() throws IOException {
        // Clear the cache at the beginning to start with a clean state
        searchCacheService.clearCache();
        
        // Get initial cache statistics
        Map<String, Object> initialStats = searchCacheService.getCacheStats();
        long initialHits = (long) initialStats.get("hits");
        long initialMisses = (long) initialStats.get("misses");
        
        // Use a unique query to avoid interference from other tests
        String query = "unique_test_query_" + System.currentTimeMillis();
        boolean isRegex = false;
        long startTime = System.currentTimeMillis() - 3600000; // 1 hour ago
        long endTime = System.currentTimeMillis();

        // First search (cache miss)
        luceneService.search(query, isRegex, startTime, endTime);
        
        // Search again (cache hit)
        luceneService.search(query, isRegex, startTime, endTime);
        
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
        luceneService.search(query, isRegex, startTime, endTime);
        
        // Verify updated cache statistics
        stats = searchCacheService.getCacheStats();
        currentHits = (long) stats.get("hits");
        currentMisses = (long) stats.get("misses");
        
        // Hits should not have increased after clearing the cache
        assertEquals(hitsBeforeClear, currentHits, "Cache hits should not increase after clearing");
        assertEquals(initialMisses + 2, currentMisses, "Cache misses should have increased by 2 total");
    }
}