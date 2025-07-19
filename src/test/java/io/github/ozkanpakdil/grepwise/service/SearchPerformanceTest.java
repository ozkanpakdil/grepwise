package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for search functionality.
 * These tests measure search response times for different scenarios.
 */
@SpringBootTest
@ActiveProfiles("test")
public class SearchPerformanceTest {

    @Autowired
    private LuceneService luceneService;

    private Path testIndexPath;
    private List<LogEntry> testLogs;
    private static final int SMALL_DATASET_SIZE = 1000;
    private static final int MEDIUM_DATASET_SIZE = 10000;
    private static final int LARGE_DATASET_SIZE = 50000;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary directory for the test index
        testIndexPath = Files.createTempDirectory("test-lucene-index");
        
        // Configure the LuceneService to use the test index path
        // Use reflection to set the index path and disable partitioning
        try {
            org.springframework.test.util.ReflectionTestUtils.setField(luceneService, "indexPath", testIndexPath.toString());
            org.springframework.test.util.ReflectionTestUtils.setField(luceneService, "partitioningEnabled", false);
        } catch (Exception e) {
            System.err.println("Failed to configure LuceneService: " + e.getMessage());
        }
        
        // Initialize the service
        luceneService.init();
        
        // Create test logs
        testLogs = new ArrayList<>();
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up the test index
        if (Files.exists(testIndexPath)) {
            Files.walk(testIndexPath)
                    .sorted((a, b) -> -a.compareTo(b))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (IOException e) {
                            System.err.println("Failed to delete " + path + ": " + e.getMessage());
                        }
                    });
        }
    }

    /**
     * Test search performance with a small dataset.
     */
    @Test
    void testSearchPerformanceSmallDataset() throws IOException {
        // Generate and index a small dataset
        generateAndIndexTestData(SMALL_DATASET_SIZE);
        
        // Measure search performance
        Map<String, Long> metrics = measureSearchPerformance();
        
        // Log the results
        System.out.println("Search Performance (Small Dataset - " + SMALL_DATASET_SIZE + " logs):");
        logMetrics(metrics);
        
        // Assert that search operations complete within reasonable time
        assertTrue(metrics.get("simpleQueryTime") < 1000, "Simple query should complete in less than 1 second");
        assertTrue(metrics.get("complexQueryTime") < 2000, "Complex query should complete in less than 2 seconds");
        assertTrue(metrics.get("wildcardQueryTime") < 3000, "Wildcard query should complete in less than 3 seconds");
    }

    /**
     * Test search performance with a medium dataset.
     */
    @Test
    void testSearchPerformanceMediumDataset() throws IOException {
        // Generate and index a medium dataset
        generateAndIndexTestData(MEDIUM_DATASET_SIZE);
        
        // Measure search performance
        Map<String, Long> metrics = measureSearchPerformance();
        
        // Log the results
        System.out.println("Search Performance (Medium Dataset - " + MEDIUM_DATASET_SIZE + " logs):");
        logMetrics(metrics);
        
        // Assert that search operations complete within reasonable time
        assertTrue(metrics.get("simpleQueryTime") < 2000, "Simple query should complete in less than 2 seconds");
        assertTrue(metrics.get("complexQueryTime") < 4000, "Complex query should complete in less than 4 seconds");
        assertTrue(metrics.get("wildcardQueryTime") < 6000, "Wildcard query should complete in less than 6 seconds");
    }

    /**
     * Test search performance with a large dataset.
     */
    @Test
    void testSearchPerformanceLargeDataset() throws IOException {
        // Generate and index a large dataset
        generateAndIndexTestData(LARGE_DATASET_SIZE);
        
        // Measure search performance
        Map<String, Long> metrics = measureSearchPerformance();
        
        // Log the results
        System.out.println("Search Performance (Large Dataset - " + LARGE_DATASET_SIZE + " logs):");
        logMetrics(metrics);
        
        // Assert that search operations complete within reasonable time
        assertTrue(metrics.get("simpleQueryTime") < 5000, "Simple query should complete in less than 5 seconds");
        assertTrue(metrics.get("complexQueryTime") < 10000, "Complex query should complete in less than 10 seconds");
        assertTrue(metrics.get("wildcardQueryTime") < 15000, "Wildcard query should complete in less than 15 seconds");
    }

    /**
     * Generate and index test data.
     */
    private void generateAndIndexTestData(int count) throws IOException {
        testLogs.clear();
        
        // Generate test logs
        for (int i = 0; i < count; i++) {
            String logLevel = (i % 5 == 0) ? "ERROR" : (i % 3 == 0) ? "WARN" : "INFO";
            String source = "test-source-" + (i % 10);
            String message = "Test log message " + i + " with some random content " + UUID.randomUUID().toString();
            
            // Add some specific content to a subset of logs for search testing
            if (i % 20 == 0) {
                message += " IMPORTANT CRITICAL ISSUE";
            }
            if (i % 25 == 0) {
                message += " database connection failed";
            }
            
            testLogs.add(createTestLogEntry(i, System.currentTimeMillis(), logLevel, message, source));
        }
        
        // Index the logs
        int indexed = luceneService.indexLogEntries(testLogs);
        assertEquals(count, indexed, "All logs should be indexed");
        
        // Ensure the logs are searchable
        List<LogEntry> results = luceneService.search("Test", false, null, null);
        assertTrue(results.size() > 0, "Indexed logs should be searchable");
    }

    /**
     * Measure search performance for different query types.
     */
    private Map<String, Long> measureSearchPerformance() {
        Map<String, Long> metrics = new HashMap<>();
        List<LogEntry> results;
        
        try {
            // Measure simple query performance
            Instant start = Instant.now();
            results = luceneService.search("Test", false, null, null);
            long simpleQueryTime = Duration.between(start, Instant.now()).toMillis();
            metrics.put("simpleQueryTime", simpleQueryTime);
            metrics.put("simpleQueryResults", (long) results.size());
            
            // Measure complex query performance (multiple terms)
            start = Instant.now();
            results = luceneService.search("IMPORTANT CRITICAL ISSUE", false, null, null);
            long complexQueryTime = Duration.between(start, Instant.now()).toMillis();
            metrics.put("complexQueryTime", complexQueryTime);
            metrics.put("complexQueryResults", (long) results.size());
            
            // Measure wildcard query performance
            start = Instant.now();
            results = luceneService.search("database*failed", false, null, null);
            long wildcardQueryTime = Duration.between(start, Instant.now()).toMillis();
            metrics.put("wildcardQueryTime", wildcardQueryTime);
            metrics.put("wildcardQueryResults", (long) results.size());
            
            // Measure level filter performance
            start = Instant.now();
            results = luceneService.findByLevel("ERROR");
            long levelFilterTime = Duration.between(start, Instant.now()).toMillis();
            metrics.put("levelFilterTime", levelFilterTime);
            metrics.put("levelFilterResults", (long) results.size());
            
            // Measure source filter performance
            start = Instant.now();
            results = luceneService.findBySource("test-source-1");
            long sourceFilterTime = Duration.between(start, Instant.now()).toMillis();
            metrics.put("sourceFilterTime", sourceFilterTime);
            metrics.put("sourceFilterResults", (long) results.size());
        } catch (IOException e) {
            System.err.println("Error during search performance measurement: " + e.getMessage());
            e.printStackTrace();
            // Add error metrics
            metrics.put("error", 1L);
            metrics.put("errorMessage", (long) e.getMessage().hashCode());
        }
        
        return metrics;
    }

    /**
     * Log performance metrics.
     */
    private void logMetrics(Map<String, Long> metrics) {
        System.out.println("  Simple Query: " + metrics.get("simpleQueryTime") + "ms (" + metrics.get("simpleQueryResults") + " results)");
        System.out.println("  Complex Query: " + metrics.get("complexQueryTime") + "ms (" + metrics.get("complexQueryResults") + " results)");
        System.out.println("  Wildcard Query: " + metrics.get("wildcardQueryTime") + "ms (" + metrics.get("wildcardQueryResults") + " results)");
        System.out.println("  Level Filter: " + metrics.get("levelFilterTime") + "ms (" + metrics.get("levelFilterResults") + " results)");
        System.out.println("  Source Filter: " + metrics.get("sourceFilterTime") + "ms (" + metrics.get("sourceFilterResults") + " results)");
    }

    /**
     * Create a test log entry.
     */
    private LogEntry createTestLogEntry(int id, long timestamp, String level, String message, String source) {
        Map<String, String> fields = new HashMap<>();
        fields.put("host", "test-host-" + (id % 5));
        fields.put("thread", "thread-" + (id % 10));
        
        return new LogEntry(
                "test-" + id,
                timestamp,
                timestamp,
                level,
                message,
                source,
                fields,
                "Raw content: " + message
        );
    }
}