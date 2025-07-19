package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for log ingestion functionality.
 * These tests measure ingestion throughput for different scenarios.
 */
@SpringBootTest
@ActiveProfiles("test")
public class IngestionPerformanceTest {

    @Autowired
    private LogBufferService logBufferService;

    @Autowired
    private LuceneService luceneService;

    private Path testIndexPath;
    private static final int SMALL_BATCH_SIZE = 100;
    private static final int MEDIUM_BATCH_SIZE = 1000;
    private static final int LARGE_BATCH_SIZE = 5000;
    private static final int CONCURRENT_THREADS = 4;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary directory for the test index
        testIndexPath = Files.createTempDirectory("test-lucene-index");
        
        // Configure the LuceneService to use the test index path
        try {
            // Use setter methods instead of reflection
            luceneService.setIndexPath(testIndexPath.toString());
            luceneService.setPartitioningEnabled(false);
            
            // Create and set a mock RealTimeUpdateService to avoid circular dependency
            luceneService.setRealTimeUpdateService(new MockRealTimeUpdateService());
        } catch (Exception e) {
            System.err.println("Failed to configure LuceneService: " + e.getMessage());
        }
        
        // Initialize the services
        luceneService.init();
        
        // Configure the LogBufferService
        try {
            ReflectionTestUtils.setField(logBufferService, "maxBufferSize", 1000);
            ReflectionTestUtils.setField(logBufferService, "flushIntervalMs", 5000);
        } catch (Exception e) {
            System.err.println("Failed to configure LogBufferService: " + e.getMessage());
        }
    }

    /**
     * Test ingestion performance with a small batch of logs.
     */
    @Test
    void testIngestionPerformanceSmallBatch() {
        // Generate test logs
        List<LogEntry> testLogs = generateTestLogs(SMALL_BATCH_SIZE);
        
        // Measure direct ingestion performance (bypassing buffer)
        Map<String, Object> directMetrics = measureDirectIngestionPerformance(testLogs);
        
        // Measure buffered ingestion performance
        Map<String, Object> bufferedMetrics = measureBufferedIngestionPerformance(testLogs);
        
        // Log the results
        System.out.println("Ingestion Performance (Small Batch - " + SMALL_BATCH_SIZE + " logs):");
        System.out.println("  Direct Ingestion: " + directMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second: " + directMetrics.get("logsPerSecond"));
        System.out.println("  Buffered Ingestion: " + bufferedMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second (buffered): " + bufferedMetrics.get("logsPerSecond"));
        
        // Assert that ingestion completes within reasonable time
        assertTrue((Long)directMetrics.get("ingestionTime") < 5000, 
                "Small batch direct ingestion should complete in less than 5 seconds");
        assertTrue((Long)bufferedMetrics.get("ingestionTime") < 1000, 
                "Small batch buffered ingestion should complete in less than 1 second");
    }

    /**
     * Test ingestion performance with a medium batch of logs.
     */
    @Test
    void testIngestionPerformanceMediumBatch() {
        // Generate test logs
        List<LogEntry> testLogs = generateTestLogs(MEDIUM_BATCH_SIZE);
        
        // Measure direct ingestion performance (bypassing buffer)
        Map<String, Object> directMetrics = measureDirectIngestionPerformance(testLogs);
        
        // Measure buffered ingestion performance
        Map<String, Object> bufferedMetrics = measureBufferedIngestionPerformance(testLogs);
        
        // Log the results
        System.out.println("Ingestion Performance (Medium Batch - " + MEDIUM_BATCH_SIZE + " logs):");
        System.out.println("  Direct Ingestion: " + directMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second: " + directMetrics.get("logsPerSecond"));
        System.out.println("  Buffered Ingestion: " + bufferedMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second (buffered): " + bufferedMetrics.get("logsPerSecond"));
        
        // Assert that ingestion completes within reasonable time
        assertTrue((Long)directMetrics.get("ingestionTime") < 10000, 
                "Medium batch direct ingestion should complete in less than 10 seconds");
        assertTrue((Long)bufferedMetrics.get("ingestionTime") < 2000, 
                "Medium batch buffered ingestion should complete in less than 2 seconds");
    }

    /**
     * Test ingestion performance with a large batch of logs.
     */
    @Test
    void testIngestionPerformanceLargeBatch() {
        // Generate test logs
        List<LogEntry> testLogs = generateTestLogs(LARGE_BATCH_SIZE);
        
        // Measure direct ingestion performance (bypassing buffer)
        Map<String, Object> directMetrics = measureDirectIngestionPerformance(testLogs);
        
        // Measure buffered ingestion performance
        Map<String, Object> bufferedMetrics = measureBufferedIngestionPerformance(testLogs);
        
        // Log the results
        System.out.println("Ingestion Performance (Large Batch - " + LARGE_BATCH_SIZE + " logs):");
        System.out.println("  Direct Ingestion: " + directMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second: " + directMetrics.get("logsPerSecond"));
        System.out.println("  Buffered Ingestion: " + bufferedMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second (buffered): " + bufferedMetrics.get("logsPerSecond"));
        
        // Assert that ingestion completes within reasonable time
        assertTrue((Long)directMetrics.get("ingestionTime") < 30000, 
                "Large batch direct ingestion should complete in less than 30 seconds");
        assertTrue((Long)bufferedMetrics.get("ingestionTime") < 5000, 
                "Large batch buffered ingestion should complete in less than 5 seconds");
    }

    /**
     * Test concurrent ingestion performance.
     */
    @Test
    void testConcurrentIngestionPerformance() throws InterruptedException {
        // Generate test logs for each thread
        int logsPerThread = MEDIUM_BATCH_SIZE / CONCURRENT_THREADS;
        List<List<LogEntry>> threadLogs = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            threadLogs.add(generateTestLogs(logsPerThread));
        }
        
        // Measure concurrent direct ingestion performance
        Map<String, Object> directMetrics = measureConcurrentDirectIngestionPerformance(threadLogs);
        
        // Measure concurrent buffered ingestion performance
        Map<String, Object> bufferedMetrics = measureConcurrentBufferedIngestionPerformance(threadLogs);
        
        // Log the results
        System.out.println("Concurrent Ingestion Performance (" + CONCURRENT_THREADS + " threads, " 
                + MEDIUM_BATCH_SIZE + " total logs):");
        System.out.println("  Direct Ingestion: " + directMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second: " + directMetrics.get("logsPerSecond"));
        System.out.println("  Buffered Ingestion: " + bufferedMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second (buffered): " + bufferedMetrics.get("logsPerSecond"));
        
        // Assert that ingestion completes within reasonable time
        assertTrue((Long)directMetrics.get("ingestionTime") < 15000, 
                "Concurrent direct ingestion should complete in less than 15 seconds");
        assertTrue((Long)bufferedMetrics.get("ingestionTime") < 3000, 
                "Concurrent buffered ingestion should complete in less than 3 seconds");
    }

    /**
     * Generate test logs.
     */
    private List<LogEntry> generateTestLogs(int count) {
        List<LogEntry> logs = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String logLevel = (i % 5 == 0) ? "ERROR" : (i % 3 == 0) ? "WARN" : "INFO";
            String source = "test-source-" + (i % 10);
            String message = "Test log message " + i + " with some random content " + UUID.randomUUID().toString();
            
            logs.add(createTestLogEntry(i, System.currentTimeMillis(), logLevel, message, source));
        }
        return logs;
    }

    /**
     * Measure direct ingestion performance (bypassing buffer).
     */
    private Map<String, Object> measureDirectIngestionPerformance(List<LogEntry> logs) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Measure ingestion time
            Instant start = Instant.now();
            int indexed = luceneService.indexLogEntries(logs);
            long ingestionTime = Duration.between(start, Instant.now()).toMillis();
            
            // Calculate throughput
            double logsPerSecond = (indexed / (double)ingestionTime) * 1000;
            
            // Store metrics
            metrics.put("ingestionTime", ingestionTime);
            metrics.put("logsPerSecond", logsPerSecond);
            metrics.put("indexed", indexed);
        } catch (IOException e) {
            System.err.println("Error during direct ingestion performance measurement: " + e.getMessage());
            e.printStackTrace();
            metrics.put("error", true);
            metrics.put("errorMessage", e.getMessage());
            metrics.put("ingestionTime", 0L);
            metrics.put("logsPerSecond", 0.0);
            metrics.put("indexed", 0);
        }
        
        return metrics;
    }

    /**
     * Measure buffered ingestion performance.
     */
    private Map<String, Object> measureBufferedIngestionPerformance(List<LogEntry> logs) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Measure ingestion time
        Instant start = Instant.now();
        logBufferService.addAllToBuffer(logs);
        long ingestionTime = Duration.between(start, Instant.now()).toMillis();
        
        // Calculate throughput
        double logsPerSecond = (logs.size() / (double)ingestionTime) * 1000;
        
        // Store metrics
        metrics.put("ingestionTime", ingestionTime);
        metrics.put("logsPerSecond", logsPerSecond);
        metrics.put("buffered", logs.size());
        
        return metrics;
    }

    /**
     * Measure concurrent direct ingestion performance.
     */
    private Map<String, Object> measureConcurrentDirectIngestionPerformance(List<List<LogEntry>> threadLogs) 
            throws InterruptedException {
        Map<String, Object> metrics = new HashMap<>();
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        
        // Track total logs and errors
        final int[] totalIndexed = {0};
        final boolean[] hasError = {false};
        
        // Measure ingestion time
        Instant start = Instant.now();
        
        // Submit tasks
        for (List<LogEntry> logs : threadLogs) {
            executor.submit(() -> {
                try {
                    int indexed = luceneService.indexLogEntries(logs);
                    synchronized (totalIndexed) {
                        totalIndexed[0] += indexed;
                    }
                } catch (IOException e) {
                    System.err.println("Error during concurrent direct ingestion: " + e.getMessage());
                    e.printStackTrace();
                    synchronized (hasError) {
                        hasError[0] = true;
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all tasks to complete
        latch.await(60, TimeUnit.SECONDS);
        long ingestionTime = Duration.between(start, Instant.now()).toMillis();
        
        // Shutdown executor
        executor.shutdown();
        
        // Calculate throughput
        double logsPerSecond = (totalIndexed[0] / (double)ingestionTime) * 1000;
        
        // Store metrics
        metrics.put("ingestionTime", ingestionTime);
        metrics.put("logsPerSecond", logsPerSecond);
        metrics.put("indexed", totalIndexed[0]);
        metrics.put("error", hasError[0]);
        
        return metrics;
    }

    /**
     * Measure concurrent buffered ingestion performance.
     */
    private Map<String, Object> measureConcurrentBufferedIngestionPerformance(List<List<LogEntry>> threadLogs) 
            throws InterruptedException {
        Map<String, Object> metrics = new HashMap<>();
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        
        // Track total logs
        final int[] totalBuffered = {0};
        
        // Measure ingestion time
        Instant start = Instant.now();
        
        // Submit tasks
        for (List<LogEntry> logs : threadLogs) {
            executor.submit(() -> {
                logBufferService.addAllToBuffer(logs);
                synchronized (totalBuffered) {
                    totalBuffered[0] += logs.size();
                }
                latch.countDown();
            });
        }
        
        // Wait for all tasks to complete
        latch.await(30, TimeUnit.SECONDS);
        long ingestionTime = Duration.between(start, Instant.now()).toMillis();
        
        // Shutdown executor
        executor.shutdown();
        
        // Calculate throughput
        double logsPerSecond = (totalBuffered[0] / (double)ingestionTime) * 1000;
        
        // Store metrics
        metrics.put("ingestionTime", ingestionTime);
        metrics.put("logsPerSecond", logsPerSecond);
        metrics.put("buffered", totalBuffered[0]);
        
        return metrics;
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
    
    /**
     * A simple mock implementation of RealTimeUpdateService that does nothing.
     * This is used to avoid the circular dependency between LuceneService and RealTimeUpdateService.
     */
    private static class MockRealTimeUpdateService extends RealTimeUpdateService {
        public MockRealTimeUpdateService() {
            super(); // Use the no-argument constructor
        }
        
        @Override
        public void broadcastLogUpdate(LogEntry logEntry) {
            // Do nothing - this is a mock implementation
        }
        
        @Override
        public void broadcastWidgetUpdate(String dashboardId, String widgetId, Object data) {
            // Do nothing - this is a mock implementation
        }
        
        @Override
        public Map<String, Object> getConnectionStats() {
            return new HashMap<>(); // Return empty stats
        }
    }
}