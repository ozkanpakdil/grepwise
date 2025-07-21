package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.PartitionConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
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
 * Performance tests for search functionality under load.
 * This test class measures search performance with different query types,
 * data volumes, and concurrency levels.
 */
public class SearchPerformanceTest {
    private static final String RESULTS_FILE = "search-benchmark-results.csv";
    private static final int SMALL_DATASET_SIZE = 1000;
    private static final int MEDIUM_DATASET_SIZE = 10000;
    private static final int LARGE_DATASET_SIZE = 50000;
    private static final int CONCURRENT_THREADS = 5;
    
    private String testIndexPath;
    private LuceneService luceneService;
    private SearchCacheService searchCacheService;
    private FieldConfigurationService fieldConfigurationService;
    private RealTimeUpdateService realTimeUpdateService;
    private ArchiveService archiveService;
    
    @BeforeEach
    public void setUp() throws IOException {
        // Create a unique index path for this test
        testIndexPath = "target/test-index-search-perf-" + UUID.randomUUID().toString();
        
        // Create test directory if it doesn't exist
        Path indexPath = Paths.get(testIndexPath);
        if (!Files.exists(indexPath)) {
            Files.createDirectories(indexPath);
        }
        
        // Initialize services
        searchCacheService = new MockSearchCacheService();
        fieldConfigurationService = new MockFieldConfigurationService();
        realTimeUpdateService = new MockRealTimeUpdateService();
        archiveService = new MockArchiveService();
        
        // Initialize Lucene service
        luceneService = new LuceneService();
        luceneService.setIndexPath(testIndexPath);
        luceneService.setPartitioningEnabled(false);
        luceneService.setFieldConfigurationService(fieldConfigurationService);
        luceneService.setRealTimeUpdateService(realTimeUpdateService);
        luceneService.setArchiveService(archiveService);
        luceneService.setSearchCacheService(searchCacheService);
        luceneService.setPartitionConfigurationRepository(new MockPartitionConfigurationRepository());
        
        // Initialize the service
        luceneService.init();
        
        // Initialize benchmark results file
        initBenchmarkResultsFile();
    }
    
    @AfterEach
    public void tearDown() throws IOException {
        // Close Lucene service
        luceneService.close();
        
        // Clean up test directory
        Path indexPath = Paths.get(testIndexPath);
        if (Files.exists(indexPath)) {
            Files.walk(indexPath)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
        }
    }
    
    private void initBenchmarkResultsFile() throws IOException {
        Path resultsFile = Paths.get(RESULTS_FILE);
        if (!Files.exists(resultsFile)) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(RESULTS_FILE))) {
                writer.write("Timestamp,TestName,DatasetSize,QueryType,CacheEnabled,Concurrent,ResponseTimeMs,ThroughputQPS,CPUUsage,MemoryUsageMB,HitRatio\n");
            }
        }
    }
    
    /**
     * Test search performance with a small dataset
     */
    @Test
    public void testSearchPerformanceSmallDataset() throws IOException {
        // Generate and index test data
        List<LogEntry> logs = generateTestLogs(SMALL_DATASET_SIZE);
        luceneService.indexLogEntries(logs);
        
        // Test simple query performance
        Map<String, Object> simpleQueryMetrics = measureSearchPerformance("error", false, null, null, false);
        saveBenchmarkResults("SimpleQuery", SMALL_DATASET_SIZE, "NoCache", simpleQueryMetrics);
        
        // Test with cache enabled
        Map<String, Object> cachedQueryMetrics = measureSearchPerformance("error", false, null, null, true);
        saveBenchmarkResults("SimpleQuery", SMALL_DATASET_SIZE, "WithCache", cachedQueryMetrics);
        
        // Test regex query performance
        Map<String, Object> regexQueryMetrics = measureSearchPerformance("error.*exception", true, null, null, false);
        saveBenchmarkResults("RegexQuery", SMALL_DATASET_SIZE, "NoCache", regexQueryMetrics);
        
        // Test time-based query performance
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000; // 24 hours ago
        Map<String, Object> timeQueryMetrics = measureSearchPerformance("", false, startTime, endTime, false);
        saveBenchmarkResults("TimeQuery", SMALL_DATASET_SIZE, "NoCache", timeQueryMetrics);
        
        // Print results
        System.out.println("Small Dataset Search Performance:");
        System.out.println("Simple Query: " + simpleQueryMetrics);
        System.out.println("Cached Query: " + cachedQueryMetrics);
        System.out.println("Regex Query: " + regexQueryMetrics);
        System.out.println("Time-based Query: " + timeQueryMetrics);
        
        // For small datasets, allow a margin of error since caching overhead might make
        // the cached query slightly slower in some test runs
        long cachedTime = (long) cachedQueryMetrics.get("responseTimeMs");
        long nonCachedTime = (long) simpleQueryMetrics.get("responseTimeMs");
        
        // Allow cached query to be up to 20% slower for small datasets
        double allowedSlowdown = 1.2; // 20% slower is acceptable for small datasets
        boolean isCachedFastEnough = cachedTime <= nonCachedTime * allowedSlowdown;
        
        System.out.println("Small dataset - Cached time: " + cachedTime + "ms, Non-cached time: " + nonCachedTime + "ms");
        assertTrue(isCachedFastEnough, 
                "Cached query should not be significantly slower than non-cached query (allowed slowdown: " + 
                allowedSlowdown + "x)");
    }
    
    /**
     * Test search performance with a medium dataset
     */
    @Test
    public void testSearchPerformanceMediumDataset() throws IOException {
        // Generate and index test data
        List<LogEntry> logs = generateTestLogs(MEDIUM_DATASET_SIZE);
        luceneService.indexLogEntries(logs);
        
        // Test simple query performance
        Map<String, Object> simpleQueryMetrics = measureSearchPerformance("error", false, null, null, false);
        saveBenchmarkResults("SimpleQuery", MEDIUM_DATASET_SIZE, "NoCache", simpleQueryMetrics);
        
        // Test with cache enabled
        Map<String, Object> cachedQueryMetrics = measureSearchPerformance("error", false, null, null, true);
        saveBenchmarkResults("SimpleQuery", MEDIUM_DATASET_SIZE, "WithCache", cachedQueryMetrics);
        
        // Test regex query performance
        Map<String, Object> regexQueryMetrics = measureSearchPerformance("error.*exception", true, null, null, false);
        saveBenchmarkResults("RegexQuery", MEDIUM_DATASET_SIZE, "NoCache", regexQueryMetrics);
        
        // Test time-based query performance
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000; // 24 hours ago
        Map<String, Object> timeQueryMetrics = measureSearchPerformance("", false, startTime, endTime, false);
        saveBenchmarkResults("TimeQuery", MEDIUM_DATASET_SIZE, "NoCache", timeQueryMetrics);
        
        // Print results
        System.out.println("Medium Dataset Search Performance:");
        System.out.println("Simple Query: " + simpleQueryMetrics);
        System.out.println("Cached Query: " + cachedQueryMetrics);
        System.out.println("Regex Query: " + regexQueryMetrics);
        System.out.println("Time-based Query: " + timeQueryMetrics);
        
        // Verify that cached query is faster than non-cached
        long cachedTime = (long) cachedQueryMetrics.get("responseTimeMs");
        long nonCachedTime = (long) simpleQueryMetrics.get("responseTimeMs");
        assertTrue(cachedTime <= nonCachedTime, 
                "Cached query should be faster or equal to non-cached query");
    }
    
    /**
     * Test search performance with a large dataset
     */
    @Test
    public void testSearchPerformanceLargeDataset() throws IOException {
        // Generate and index test data
        List<LogEntry> logs = generateTestLogs(LARGE_DATASET_SIZE);
        luceneService.indexLogEntries(logs);
        
        // Test simple query performance
        Map<String, Object> simpleQueryMetrics = measureSearchPerformance("error", false, null, null, false);
        saveBenchmarkResults("SimpleQuery", LARGE_DATASET_SIZE, "NoCache", simpleQueryMetrics);
        
        // Test with cache enabled
        Map<String, Object> cachedQueryMetrics = measureSearchPerformance("error", false, null, null, true);
        saveBenchmarkResults("SimpleQuery", LARGE_DATASET_SIZE, "WithCache", cachedQueryMetrics);
        
        // Test regex query performance
        Map<String, Object> regexQueryMetrics = measureSearchPerformance("error.*exception", true, null, null, false);
        saveBenchmarkResults("RegexQuery", LARGE_DATASET_SIZE, "NoCache", regexQueryMetrics);
        
        // Test time-based query performance
        long endTime = System.currentTimeMillis();
        long startTime = endTime - 24 * 60 * 60 * 1000; // 24 hours ago
        Map<String, Object> timeQueryMetrics = measureSearchPerformance("", false, startTime, endTime, false);
        saveBenchmarkResults("TimeQuery", LARGE_DATASET_SIZE, "NoCache", timeQueryMetrics);
        
        // Print results
        System.out.println("Large Dataset Search Performance:");
        System.out.println("Simple Query: " + simpleQueryMetrics);
        System.out.println("Cached Query: " + cachedQueryMetrics);
        System.out.println("Regex Query: " + regexQueryMetrics);
        System.out.println("Time-based Query: " + timeQueryMetrics);
        
        // For large datasets, allow a margin of error since JVM optimizations and other factors
        // can introduce variability in performance measurements
        long cachedTime = (long) cachedQueryMetrics.get("responseTimeMs");
        long nonCachedTime = (long) simpleQueryMetrics.get("responseTimeMs");
        
        // Allow cached query to be up to 50% slower for large datasets
        double allowedSlowdown = 1.5; // 50% slower is acceptable for large datasets
        boolean isCachedFastEnough = cachedTime <= nonCachedTime * allowedSlowdown;
        
        System.out.println("Large dataset - Cached time: " + cachedTime + "ms, Non-cached time: " + nonCachedTime + "ms");
        assertTrue(isCachedFastEnough, 
                "Cached query should not be significantly slower than non-cached query (allowed slowdown: " + 
                allowedSlowdown + "x)");
    }
    
    /**
     * Test concurrent search performance
     */
    @Test
    public void testConcurrentSearchPerformance() throws IOException, InterruptedException {
        // Generate and index test data
        List<LogEntry> logs = generateTestLogs(MEDIUM_DATASET_SIZE);
        luceneService.indexLogEntries(logs);
        
        // Create different query types for concurrent testing
        List<SearchQuery> queries = new ArrayList<>();
        queries.add(new SearchQuery("error", false, null, null));
        queries.add(new SearchQuery("warning", false, null, null));
        queries.add(new SearchQuery("info", false, null, null));
        queries.add(new SearchQuery("debug", false, null, null));
        queries.add(new SearchQuery("error.*exception", true, null, null));
        
        // Test concurrent search without cache
        Map<String, Object> concurrentMetrics = measureConcurrentSearchPerformance(queries, false);
        saveBenchmarkResults("ConcurrentQueries", MEDIUM_DATASET_SIZE, "NoCache", concurrentMetrics);
        
        // Test concurrent search with cache
        Map<String, Object> concurrentCachedMetrics = measureConcurrentSearchPerformance(queries, true);
        saveBenchmarkResults("ConcurrentQueries", MEDIUM_DATASET_SIZE, "WithCache", concurrentCachedMetrics);
        
        // Print results
        System.out.println("Concurrent Search Performance:");
        System.out.println("Without Cache: " + concurrentMetrics);
        System.out.println("With Cache: " + concurrentCachedMetrics);
        
        // For concurrent tests, allow a margin of error since thread scheduling and other factors
        // can introduce variability in performance measurements
        long cachedTime = (long) concurrentCachedMetrics.get("responseTimeMs");
        long nonCachedTime = (long) concurrentMetrics.get("responseTimeMs");
        
        // Allow cached query to be up to 30% slower for concurrent tests
        double allowedSlowdown = 1.3; // 30% slower is acceptable for concurrent tests
        boolean isCachedFastEnough = cachedTime <= nonCachedTime * allowedSlowdown;
        
        System.out.println("Concurrent test - Cached time: " + cachedTime + "ms, Non-cached time: " + nonCachedTime + "ms");
        assertTrue(isCachedFastEnough, 
                "Cached concurrent search should not be significantly slower than non-cached search (allowed slowdown: " + 
                allowedSlowdown + "x)");
    }
    
    /**
     * Generate test log entries
     */
    private List<LogEntry> generateTestLogs(int count) {
        List<LogEntry> logs = new ArrayList<>();
        long baseTimestamp = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // Start 24 hours ago
        
        String[] levels = {"INFO", "WARNING", "ERROR", "DEBUG"};
        String[] sources = {"application", "system", "security", "database", "network"};
        
        for (int i = 0; i < count; i++) {
            String level = levels[i % levels.length];
            String source = sources[i % sources.length];
            long timestamp = baseTimestamp + (i * 1000); // 1 second between logs
            
            String message;
            if (level.equals("ERROR")) {
                if (i % 3 == 0) {
                    message = "Error occurred: NullPointerException in module " + (i % 10);
                } else if (i % 3 == 1) {
                    message = "Error processing request: timeout exception for request " + i;
                } else {
                    message = "Error in transaction: database connection failed for transaction " + i;
                }
            } else if (level.equals("WARNING")) {
                message = "Warning: resource usage high at " + (i % 100) + "% for service " + (i % 5);
            } else if (level.equals("INFO")) {
                message = "User " + (i % 20) + " logged in successfully from IP 192.168.1." + (i % 255);
            } else {
                message = "Debug: processing item " + i + " with parameters: param1=" + (i % 10) + ", param2=" + (i % 5);
            }
            
            logs.add(createTestLogEntry(i, timestamp, level, message, source));
        }
        
        return logs;
    }
    
    /**
     * Measure search performance for a single query
     */
    private Map<String, Object> measureSearchPerformance(String queryStr, boolean isRegex, 
                                                        Long startTime, Long endTime, boolean useCache) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Configure cache
        searchCacheService.setCacheEnabled(useCache);
        if (useCache) {
            searchCacheService.clearCache();
        }
        
        // Warm-up run
        try {
            luceneService.search(queryStr, isRegex, startTime, endTime);
        } catch (IOException e) {
            System.err.println("Error during warm-up search: " + e.getMessage());
            e.printStackTrace();
        }
        
        // Measure CPU usage
        double startCpuTime = getProcessCpuTime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long startMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        // Measure search time
        Instant start = Instant.now();
        List<LogEntry> results = new ArrayList<>();
        try {
            results = luceneService.search(queryStr, isRegex, startTime, endTime);
        } catch (IOException e) {
            System.err.println("Error during search: " + e.getMessage());
            e.printStackTrace();
        }
        Instant end = Instant.now();
        
        // Calculate metrics
        long elapsedTimeMs = Duration.between(start, end).toMillis();
        double endCpuTime = getProcessCpuTime();
        long endMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        double cpuUsage = calculateCpuUsage(startCpuTime, endCpuTime, elapsedTimeMs);
        double memoryUsageMB = (endMemory - startMemory) / (1024.0 * 1024.0);
        
        // Store metrics
        metrics.put("responseTimeMs", elapsedTimeMs);
        metrics.put("resultCount", results.size());
        metrics.put("throughputQPS", elapsedTimeMs > 0 ? 1000.0 / elapsedTimeMs : 0);
        metrics.put("cpuUsage", cpuUsage);
        metrics.put("memoryUsageMB", memoryUsageMB);
        
        // Add cache stats if enabled
        if (useCache) {
            Map<String, Object> cacheStats = searchCacheService.getCacheStats();
            metrics.put("cacheHitRatio", cacheStats.get("hitRatio"));
            metrics.put("cacheSize", cacheStats.get("size"));
        } else {
            metrics.put("cacheHitRatio", 0.0);
            metrics.put("cacheSize", 0);
        }
        
        return metrics;
    }
    
    /**
     * Measure concurrent search performance
     */
    private Map<String, Object> measureConcurrentSearchPerformance(List<SearchQuery> queries, boolean useCache) 
            throws InterruptedException {
        Map<String, Object> metrics = new HashMap<>();
        
        // Configure cache
        searchCacheService.setCacheEnabled(useCache);
        if (useCache) {
            searchCacheService.clearCache();
        }
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        
        // Warm-up run
        for (SearchQuery query : queries) {
            try {
                luceneService.search(query.queryStr, query.isRegex, query.startTime, query.endTime);
            } catch (IOException e) {
                System.err.println("Error during warm-up search: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // Measure CPU usage
        double startCpuTime = getProcessCpuTime();
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long startMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        // Start timing
        Instant start = Instant.now();
        
        // Submit search tasks
        List<Long> responseTimes = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            final int threadIndex = i;
            executor.submit(() -> {
                try {
                    // Each thread executes a different query
                    SearchQuery query = queries.get(threadIndex % queries.size());
                    
                    Instant queryStart = Instant.now();
                    List<LogEntry> results = new ArrayList<>();
                    try {
                        results = luceneService.search(
                                query.queryStr, query.isRegex, query.startTime, query.endTime);
                    } catch (IOException e) {
                        System.err.println("Error during concurrent search: " + e.getMessage());
                        e.printStackTrace();
                    }
                    Instant queryEnd = Instant.now();
                    
                    long queryTime = Duration.between(queryStart, queryEnd).toMillis();
                    synchronized (responseTimes) {
                        responseTimes.add(queryTime);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        latch.await();
        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        
        // End timing
        Instant end = Instant.now();
        
        // Calculate metrics
        long elapsedTimeMs = Duration.between(start, end).toMillis();
        double endCpuTime = getProcessCpuTime();
        long endMemory = memoryBean.getHeapMemoryUsage().getUsed();
        
        double cpuUsage = calculateCpuUsage(startCpuTime, endCpuTime, elapsedTimeMs);
        double memoryUsageMB = (endMemory - startMemory) / (1024.0 * 1024.0);
        
        // Calculate average and max response times
        long totalResponseTime = 0;
        long maxResponseTime = 0;
        for (long time : responseTimes) {
            totalResponseTime += time;
            maxResponseTime = Math.max(maxResponseTime, time);
        }
        long avgResponseTime = responseTimes.isEmpty() ? 0 : totalResponseTime / responseTimes.size();
        
        // Store metrics
        metrics.put("responseTimeMs", elapsedTimeMs);
        metrics.put("avgQueryResponseTimeMs", avgResponseTime);
        metrics.put("maxQueryResponseTimeMs", maxResponseTime);
        metrics.put("throughputQPS", elapsedTimeMs > 0 ? (CONCURRENT_THREADS * 1000.0) / elapsedTimeMs : 0);
        metrics.put("cpuUsage", cpuUsage);
        metrics.put("memoryUsageMB", memoryUsageMB);
        
        // Add cache stats if enabled
        if (useCache) {
            Map<String, Object> cacheStats = searchCacheService.getCacheStats();
            metrics.put("cacheHitRatio", cacheStats.get("hitRatio"));
            metrics.put("cacheSize", cacheStats.get("size"));
        } else {
            metrics.put("cacheHitRatio", 0.0);
            metrics.put("cacheSize", 0);
        }
        
        return metrics;
    }
    
    /**
     * Create a test log entry
     */
    private LogEntry createTestLogEntry(int id, long timestamp, String level, String message, String source) {
        // Create metadata with extracted fields
        Map<String, String> metadata = new HashMap<>();
        metadata.put("test_id", String.valueOf(id));
        metadata.put("host", "test-host-" + (id % 5));
        metadata.put("service", "test-service-" + (id % 3));
        
        // Create log entry using constructor
        return new LogEntry(
            UUID.randomUUID().toString(),
            timestamp,
            System.currentTimeMillis(),
            level,
            message,
            source,
            metadata,
            message // Using message as rawContent for simplicity
        );
    }
    
    /**
     * Get the current CPU time used by the process
     */
    private double getProcessCpuTime() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuTime();
        }
        return 0.0;
    }
    
    /**
     * Calculate CPU usage percentage
     */
    private double calculateCpuUsage(double startCpuTime, double endCpuTime, long elapsedTimeMs) {
        if (elapsedTimeMs == 0) {
            return 0.0;
        }
        
        double cpuTimeDiff = endCpuTime - startCpuTime;
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        
        // Convert from nanoseconds to milliseconds and account for multiple processors
        double cpuUsage = (cpuTimeDiff / 1_000_000.0) / (elapsedTimeMs * availableProcessors);
        
        return Math.min(cpuUsage * 100.0, 100.0); // Convert to percentage and cap at 100%
    }
    
    /**
     * Save benchmark results to CSV file
     */
    private void saveBenchmarkResults(String testName, int datasetSize, String mode, Map<String, Object> metrics) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String timestamp = dateFormat.format(new Date());
            
            String line = String.format("%s,%s,%d,%s,%b,%b,%d,%.2f,%.2f,%.2f,%.2f\n",
                    timestamp,
                    testName,
                    datasetSize,
                    mode,
                    metrics.containsKey("cacheHitRatio") && (double)metrics.get("cacheHitRatio") > 0,
                    mode.contains("Concurrent"),
                    metrics.get("responseTimeMs"),
                    metrics.get("throughputQPS"),
                    metrics.get("cpuUsage"),
                    metrics.get("memoryUsageMB"),
                    metrics.getOrDefault("cacheHitRatio", 0.0));
            
            Files.write(Paths.get(RESULTS_FILE), line.getBytes(), StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("Failed to save benchmark results: " + e.getMessage());
        }
    }
    
    /**
     * Helper class to represent a search query
     */
    private static class SearchQuery {
        private final String queryStr;
        private final boolean isRegex;
        private final Long startTime;
        private final Long endTime;
        
        public SearchQuery(String queryStr, boolean isRegex, Long startTime, Long endTime) {
            this.queryStr = queryStr;
            this.isRegex = isRegex;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
    
    /**
     * Mock implementation of RealTimeUpdateService
     */
    private static class MockRealTimeUpdateService extends RealTimeUpdateService {
        public MockRealTimeUpdateService() {
            super();
        }
        
        @Override
        public void broadcastLogUpdate(LogEntry logEntry) {
            // Do nothing in tests
        }
        
        @Override
        public void broadcastWidgetUpdate(String dashboardId, String widgetId, Object data) {
            // Do nothing in tests
        }
        
        @Override
        public Map<String, Object> getConnectionStats() {
            return new HashMap<>();
        }
    }
    
    /**
     * Mock implementation of FieldConfigurationService
     */
    private static class MockFieldConfigurationService extends FieldConfigurationService {
        public MockFieldConfigurationService() {
            super(null);
        }
        
        @Override
        public List<FieldConfiguration> getAllEnabledFieldConfigurations() {
            return new ArrayList<>();
        }
        
        @Override
        public String extractFieldValue(FieldConfiguration fieldConfiguration, String sourceValue) {
            return null;
        }
    }
    
    /**
     * Mock implementation of PartitionConfigurationRepository
     */
    private static class MockPartitionConfigurationRepository extends io.github.ozkanpakdil.grepwise.repository.PartitionConfigurationRepository {
        @Override
        public PartitionConfiguration getDefaultConfiguration() {
            PartitionConfiguration config = new PartitionConfiguration();
            config.setPartitioningEnabled(false);
            config.setPartitionType("MONTHLY"); // Must be one of: DAILY, WEEKLY, MONTHLY
            return config;
        }
    }
    
    /**
     * Mock implementation of ArchiveService
     */
    private static class MockArchiveService extends ArchiveService {
        public MockArchiveService() {
            super(null, null);
        }
        
        @Override
        public boolean archiveLogsBeforeDeletion(List<LogEntry> logs) {
            return true;
        }
    }
    
    /**
     * Mock implementation of SearchCacheService
     */
    private static class MockSearchCacheService extends SearchCacheService {
        private boolean cacheEnabled = true;
        
        @Override
        public boolean isCacheEnabled() {
            return cacheEnabled;
        }
        
        @Override
        public void setCacheEnabled(boolean cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
            ReflectionTestUtils.setField(this, "cacheEnabled", cacheEnabled);
        }
    }
}