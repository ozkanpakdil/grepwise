package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.config.TestConfig;
import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.PartitionConfiguration;
import io.github.ozkanpakdil.grepwise.repository.FieldConfigurationRepository;
import io.github.ozkanpakdil.grepwise.repository.PartitionConfigurationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.BufferedWriter;
import java.io.File;
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
 * Enhanced performance benchmarks for log ingestion functionality.
 * These tests measure ingestion throughput, CPU usage, and memory consumption for different scenarios.
 * Results are saved to a file for historical comparison.
 */
public class LogIngestionBenchmark {

    private LogBufferService logBufferService;
    private LuceneService luceneService;

    private Path testIndexPath;
    private static final int SMALL_BATCH_SIZE = 100;
    private static final int MEDIUM_BATCH_SIZE = 1000;
    private static final int LARGE_BATCH_SIZE = 5000;
    private static final int CONCURRENT_THREADS = 4;
    
    // File to save benchmark results
    private static final String BENCHMARK_RESULTS_FILE = "benchmark-results.csv";
    
    // JMX beans for system metrics
    private OperatingSystemMXBean osBean;
    private MemoryMXBean memoryBean;

    @BeforeEach
    void setUp() throws IOException {
        // Create a temporary directory for the test index
        testIndexPath = Files.createTempDirectory("test-lucene-index");
        
        // Initialize LuceneService
        luceneService = new LuceneService();
        
        // Configure the LuceneService to use the test index path
        try {
            // Use setter methods instead of reflection
            luceneService.setIndexPath(testIndexPath.toString());
            luceneService.setPartitioningEnabled(false);
            
            // Create and set mock services to avoid dependencies
            luceneService.setRealTimeUpdateService(new MockRealTimeUpdateService());
            luceneService.setFieldConfigurationService(new MockFieldConfigurationService());
            luceneService.setPartitionConfigurationRepository(new MockPartitionConfigurationRepository());
            luceneService.setArchiveService(new MockArchiveService());
            luceneService.setSearchCacheService(new MockSearchCacheService());
        } catch (Exception e) {
            System.err.println("Failed to configure LuceneService: " + e.getMessage());
        }
        
        // Initialize the services
        luceneService.init();
        
        // Initialize LogBufferService
        logBufferService = new LogBufferService(luceneService);
        
        // Configure the LogBufferService
        try {
            // Use setter methods instead of reflection
            logBufferService.setMaxBufferSize(1000);
            logBufferService.setFlushIntervalMs(5000);
        } catch (Exception e) {
            System.err.println("Failed to configure LogBufferService: " + e.getMessage());
        }
        
        // Initialize JMX beans for system metrics
        osBean = ManagementFactory.getOperatingSystemMXBean();
        memoryBean = ManagementFactory.getMemoryMXBean();
        
        // Initialize benchmark results file if it doesn't exist
        initBenchmarkResultsFile();
    }
    
    @AfterEach
    void tearDown() throws IOException {
        // Clean up temporary index directory
        if (testIndexPath != null) {
            Files.walk(testIndexPath)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
        }
    }

    /**
     * Initialize the benchmark results file with headers if it doesn't exist.
     */
    private void initBenchmarkResultsFile() throws IOException {
        File file = new File(BENCHMARK_RESULTS_FILE);
        if (!file.exists()) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("Timestamp,Test,BatchSize,Mode,IngestionTime(ms),LogsPerSecond,CPUUsage(%),HeapMemoryUsed(MB),NonHeapMemoryUsed(MB)\n");
            }
        }
    }

    /**
     * Test ingestion performance with a small batch of logs.
     */
    @Test
    void testIngestionPerformanceSmallBatch() throws IOException {
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
        System.out.println("  CPU Usage: " + directMetrics.get("cpuUsage") + "%");
        System.out.println("  Heap Memory Used: " + directMetrics.get("heapMemoryUsed") + "MB");
        System.out.println("  Non-Heap Memory Used: " + directMetrics.get("nonHeapMemoryUsed") + "MB");
        
        System.out.println("  Buffered Ingestion: " + bufferedMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second (buffered): " + bufferedMetrics.get("logsPerSecond"));
        System.out.println("  CPU Usage (buffered): " + bufferedMetrics.get("cpuUsage") + "%");
        System.out.println("  Heap Memory Used (buffered): " + bufferedMetrics.get("heapMemoryUsed") + "MB");
        System.out.println("  Non-Heap Memory Used (buffered): " + bufferedMetrics.get("nonHeapMemoryUsed") + "MB");
        
        // Save results to file
        saveBenchmarkResults("SmallBatch", SMALL_BATCH_SIZE, "Direct", directMetrics);
        saveBenchmarkResults("SmallBatch", SMALL_BATCH_SIZE, "Buffered", bufferedMetrics);
        
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
    void testIngestionPerformanceMediumBatch() throws IOException {
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
        System.out.println("  CPU Usage: " + directMetrics.get("cpuUsage") + "%");
        System.out.println("  Heap Memory Used: " + directMetrics.get("heapMemoryUsed") + "MB");
        System.out.println("  Non-Heap Memory Used: " + directMetrics.get("nonHeapMemoryUsed") + "MB");
        
        System.out.println("  Buffered Ingestion: " + bufferedMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second (buffered): " + bufferedMetrics.get("logsPerSecond"));
        System.out.println("  CPU Usage (buffered): " + bufferedMetrics.get("cpuUsage") + "%");
        System.out.println("  Heap Memory Used (buffered): " + bufferedMetrics.get("heapMemoryUsed") + "MB");
        System.out.println("  Non-Heap Memory Used (buffered): " + bufferedMetrics.get("nonHeapMemoryUsed") + "MB");
        
        // Save results to file
        saveBenchmarkResults("MediumBatch", MEDIUM_BATCH_SIZE, "Direct", directMetrics);
        saveBenchmarkResults("MediumBatch", MEDIUM_BATCH_SIZE, "Buffered", bufferedMetrics);
        
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
    void testIngestionPerformanceLargeBatch() throws IOException {
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
        System.out.println("  CPU Usage: " + directMetrics.get("cpuUsage") + "%");
        System.out.println("  Heap Memory Used: " + directMetrics.get("heapMemoryUsed") + "MB");
        System.out.println("  Non-Heap Memory Used: " + directMetrics.get("nonHeapMemoryUsed") + "MB");
        
        System.out.println("  Buffered Ingestion: " + bufferedMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second (buffered): " + bufferedMetrics.get("logsPerSecond"));
        System.out.println("  CPU Usage (buffered): " + bufferedMetrics.get("cpuUsage") + "%");
        System.out.println("  Heap Memory Used (buffered): " + bufferedMetrics.get("heapMemoryUsed") + "MB");
        System.out.println("  Non-Heap Memory Used (buffered): " + bufferedMetrics.get("nonHeapMemoryUsed") + "MB");
        
        // Save results to file
        saveBenchmarkResults("LargeBatch", LARGE_BATCH_SIZE, "Direct", directMetrics);
        saveBenchmarkResults("LargeBatch", LARGE_BATCH_SIZE, "Buffered", bufferedMetrics);
        
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
    void testConcurrentIngestionPerformance() throws InterruptedException, IOException {
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
        System.out.println("  CPU Usage: " + directMetrics.get("cpuUsage") + "%");
        System.out.println("  Heap Memory Used: " + directMetrics.get("heapMemoryUsed") + "MB");
        System.out.println("  Non-Heap Memory Used: " + directMetrics.get("nonHeapMemoryUsed") + "MB");
        
        System.out.println("  Buffered Ingestion: " + bufferedMetrics.get("ingestionTime") + "ms");
        System.out.println("  Logs/second (buffered): " + bufferedMetrics.get("logsPerSecond"));
        System.out.println("  CPU Usage (buffered): " + bufferedMetrics.get("cpuUsage") + "%");
        System.out.println("  Heap Memory Used (buffered): " + bufferedMetrics.get("heapMemoryUsed") + "MB");
        System.out.println("  Non-Heap Memory Used (buffered): " + bufferedMetrics.get("nonHeapMemoryUsed") + "MB");
        
        // Save results to file
        saveBenchmarkResults("ConcurrentIngestion", MEDIUM_BATCH_SIZE, "Direct", directMetrics);
        saveBenchmarkResults("ConcurrentIngestion", MEDIUM_BATCH_SIZE, "Buffered", bufferedMetrics);
        
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
     * Measure direct ingestion performance (bypassing buffer) with CPU and memory metrics.
     */
    private Map<String, Object> measureDirectIngestionPerformance(List<LogEntry> logs) {
        Map<String, Object> metrics = new HashMap<>();
        
        try {
            // Get initial memory usage
            long initialHeapMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long initialNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed();
            
            // Measure ingestion time and CPU usage
            Instant start = Instant.now();
            double startCpuTime = getProcessCpuTime();
            
            int indexed = luceneService.indexLogEntries(logs);
            
            long ingestionTime = Duration.between(start, Instant.now()).toMillis();
            double endCpuTime = getProcessCpuTime();
            
            // Get final memory usage
            long finalHeapMemory = memoryBean.getHeapMemoryUsage().getUsed();
            long finalNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed();
            
            // Calculate metrics
            double cpuUsage = calculateCpuUsage(startCpuTime, endCpuTime, ingestionTime);
            double heapMemoryUsed = (finalHeapMemory - initialHeapMemory) / (1024.0 * 1024.0); // Convert to MB
            double nonHeapMemoryUsed = (finalNonHeapMemory - initialNonHeapMemory) / (1024.0 * 1024.0); // Convert to MB
            double logsPerSecond = (indexed / (double)ingestionTime) * 1000;
            
            // Store metrics
            metrics.put("ingestionTime", ingestionTime);
            metrics.put("logsPerSecond", logsPerSecond);
            metrics.put("indexed", indexed);
            metrics.put("cpuUsage", cpuUsage);
            metrics.put("heapMemoryUsed", heapMemoryUsed);
            metrics.put("nonHeapMemoryUsed", nonHeapMemoryUsed);
        } catch (IOException e) {
            System.err.println("Error during direct ingestion performance measurement: " + e.getMessage());
            e.printStackTrace();
            metrics.put("error", true);
            metrics.put("errorMessage", e.getMessage());
            metrics.put("ingestionTime", 0L);
            metrics.put("logsPerSecond", 0.0);
            metrics.put("indexed", 0);
            metrics.put("cpuUsage", 0.0);
            metrics.put("heapMemoryUsed", 0.0);
            metrics.put("nonHeapMemoryUsed", 0.0);
        }
        
        return metrics;
    }

    /**
     * Measure buffered ingestion performance with CPU and memory metrics.
     */
    private Map<String, Object> measureBufferedIngestionPerformance(List<LogEntry> logs) {
        Map<String, Object> metrics = new HashMap<>();
        
        // Get initial memory usage
        long initialHeapMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long initialNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        // Measure ingestion time and CPU usage
        Instant start = Instant.now();
        double startCpuTime = getProcessCpuTime();
        
        logBufferService.addAllToBuffer(logs);
        
        long ingestionTime = Duration.between(start, Instant.now()).toMillis();
        double endCpuTime = getProcessCpuTime();
        
        // Get final memory usage
        long finalHeapMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long finalNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        // Calculate metrics
        double cpuUsage = calculateCpuUsage(startCpuTime, endCpuTime, ingestionTime);
        double heapMemoryUsed = (finalHeapMemory - initialHeapMemory) / (1024.0 * 1024.0); // Convert to MB
        double nonHeapMemoryUsed = (finalNonHeapMemory - initialNonHeapMemory) / (1024.0 * 1024.0); // Convert to MB
        double logsPerSecond = (logs.size() / (double)ingestionTime) * 1000;
        
        // Store metrics
        metrics.put("ingestionTime", ingestionTime);
        metrics.put("logsPerSecond", logsPerSecond);
        metrics.put("buffered", logs.size());
        metrics.put("cpuUsage", cpuUsage);
        metrics.put("heapMemoryUsed", heapMemoryUsed);
        metrics.put("nonHeapMemoryUsed", nonHeapMemoryUsed);
        
        return metrics;
    }

    /**
     * Measure concurrent direct ingestion performance with CPU and memory metrics.
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
        
        // Get initial memory usage
        long initialHeapMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long initialNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        // Measure ingestion time and CPU usage
        Instant start = Instant.now();
        double startCpuTime = getProcessCpuTime();
        
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
        double endCpuTime = getProcessCpuTime();
        
        // Get final memory usage
        long finalHeapMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long finalNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        // Shutdown executor
        executor.shutdown();
        
        // Calculate metrics
        double cpuUsage = calculateCpuUsage(startCpuTime, endCpuTime, ingestionTime);
        double heapMemoryUsed = (finalHeapMemory - initialHeapMemory) / (1024.0 * 1024.0); // Convert to MB
        double nonHeapMemoryUsed = (finalNonHeapMemory - initialNonHeapMemory) / (1024.0 * 1024.0); // Convert to MB
        double logsPerSecond = (totalIndexed[0] / (double)ingestionTime) * 1000;
        
        // Store metrics
        metrics.put("ingestionTime", ingestionTime);
        metrics.put("logsPerSecond", logsPerSecond);
        metrics.put("indexed", totalIndexed[0]);
        metrics.put("error", hasError[0]);
        metrics.put("cpuUsage", cpuUsage);
        metrics.put("heapMemoryUsed", heapMemoryUsed);
        metrics.put("nonHeapMemoryUsed", nonHeapMemoryUsed);
        
        return metrics;
    }

    /**
     * Measure concurrent buffered ingestion performance with CPU and memory metrics.
     */
    private Map<String, Object> measureConcurrentBufferedIngestionPerformance(List<List<LogEntry>> threadLogs) 
            throws InterruptedException {
        Map<String, Object> metrics = new HashMap<>();
        
        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
        
        // Track total logs
        final int[] totalBuffered = {0};
        
        // Get initial memory usage
        long initialHeapMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long initialNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        // Measure ingestion time and CPU usage
        Instant start = Instant.now();
        double startCpuTime = getProcessCpuTime();
        
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
        double endCpuTime = getProcessCpuTime();
        
        // Get final memory usage
        long finalHeapMemory = memoryBean.getHeapMemoryUsage().getUsed();
        long finalNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed();
        
        // Shutdown executor
        executor.shutdown();
        
        // Calculate metrics
        double cpuUsage = calculateCpuUsage(startCpuTime, endCpuTime, ingestionTime);
        double heapMemoryUsed = (finalHeapMemory - initialHeapMemory) / (1024.0 * 1024.0); // Convert to MB
        double nonHeapMemoryUsed = (finalNonHeapMemory - initialNonHeapMemory) / (1024.0 * 1024.0); // Convert to MB
        double logsPerSecond = (totalBuffered[0] / (double)ingestionTime) * 1000;
        
        // Store metrics
        metrics.put("ingestionTime", ingestionTime);
        metrics.put("logsPerSecond", logsPerSecond);
        metrics.put("buffered", totalBuffered[0]);
        metrics.put("cpuUsage", cpuUsage);
        metrics.put("heapMemoryUsed", heapMemoryUsed);
        metrics.put("nonHeapMemoryUsed", nonHeapMemoryUsed);
        
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
     * Get the current process CPU time.
     */
    private double getProcessCpuTime() {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            return ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuTime();
        }
        return 0.0;
    }
    
    /**
     * Calculate CPU usage as a percentage.
     */
    private double calculateCpuUsage(double startCpuTime, double endCpuTime, long elapsedTimeMs) {
        if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
            double cpuTime = endCpuTime - startCpuTime;
            int availableProcessors = osBean.getAvailableProcessors();
            
            // Convert from nanoseconds to milliseconds and account for multiple processors
            double cpuUsage = (cpuTime / 1000000.0) / (elapsedTimeMs * availableProcessors) * 100.0;
            return Math.min(100.0, cpuUsage); // Cap at 100%
        }
        return 0.0;
    }
    
    /**
     * Save benchmark results to a CSV file.
     */
    private void saveBenchmarkResults(String testName, int batchSize, String mode, Map<String, Object> metrics) 
            throws IOException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = dateFormat.format(new Date());
        
        String line = String.format("%s,%s,%d,%s,%d,%.2f,%.2f,%.2f,%.2f\n",
                timestamp,
                testName,
                batchSize,
                mode,
                metrics.get("ingestionTime"),
                metrics.get("logsPerSecond"),
                metrics.get("cpuUsage"),
                metrics.get("heapMemoryUsed"),
                metrics.get("nonHeapMemoryUsed"));
        
        Files.write(Paths.get(BENCHMARK_RESULTS_FILE), line.getBytes(), 
                StandardOpenOption.APPEND);
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
    
    /**
     * A simple mock implementation of FieldConfigurationService that returns empty results.
     * This is used to avoid dependencies on the database during testing.
     */
    private static class MockFieldConfigurationService extends FieldConfigurationService {
        public MockFieldConfigurationService() {
            super(null); // Pass null for the repository since we don't use it
        }
        
        @Override
        public List<FieldConfiguration> getAllEnabledFieldConfigurations() {
            return new ArrayList<>(); // Return empty list
        }
        
        @Override
        public String extractFieldValue(FieldConfiguration fieldConfiguration, String sourceValue) {
            return null; // Return null for simplicity
        }
    }
    
    /**
     * A simple mock implementation of PartitionConfigurationRepository that returns a default configuration.
     * This is used to avoid dependencies on the database during testing.
     */
    private static class MockPartitionConfigurationRepository extends PartitionConfigurationRepository {
        @Override
        public PartitionConfiguration getDefaultConfiguration() {
            PartitionConfiguration config = new PartitionConfiguration();
            config.setPartitioningEnabled(false); // Disable partitioning for tests
            return config;
        }
    }
    
    /**
     * A simple mock implementation of ArchiveService that does nothing.
     * This is used to avoid dependencies on the database during testing.
     */
    private static class MockArchiveService extends ArchiveService {
        public MockArchiveService() {
            super(null, null); // Pass null for the repositories since we don't use them
        }
        
        @Override
        public boolean archiveLogsBeforeDeletion(List<LogEntry> logs) {
            return true; // Pretend archiving was successful
        }
    }
    
    /**
     * A simple mock implementation of SearchCacheService that does nothing.
     * This is used to avoid dependencies on the cache during testing.
     */
    private static class MockSearchCacheService extends SearchCacheService {
        @Override
        public List<LogEntry> getFromCache(String queryStr, boolean isRegex, Long startTime, Long endTime) {
            return null; // Return null to indicate cache miss
        }
        
        @Override
        public void addToCache(String queryStr, boolean isRegex, Long startTime, Long endTime, List<LogEntry> results) {
            // Do nothing - this is a mock implementation
        }
    }
}