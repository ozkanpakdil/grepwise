package io.github.ozkanpakdil.grepwise.benchmark;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.service.LogBufferService;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.github.ozkanpakdil.grepwise.service.RealTimeUpdateService;

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

/**
 * A standalone benchmark tool for measuring log ingestion performance.
 * This tool can be run as a command-line application to benchmark log ingestion
 * with different batch sizes and modes (direct vs. buffered).
 * 
 * Results are saved to a CSV file for historical comparison.
 */
public class LogIngestionBenchmarkTool {

    private static final int SMALL_BATCH_SIZE = 100;
    private static final int MEDIUM_BATCH_SIZE = 1000;
    private static final int LARGE_BATCH_SIZE = 5000;
    private static final int CONCURRENT_THREADS = 4;
    
    // File to save benchmark results
    private static final String BENCHMARK_RESULTS_FILE = "benchmark-results.csv";
    
    // JMX beans for system metrics
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    
    // Services
    private final LuceneService luceneService;
    private final LogBufferService logBufferService;
    
    // Temporary index path
    private Path testIndexPath;

    /**
     * Main method to run the benchmark tool.
     */
    public static void main(String[] args) {
        try {
            LogIngestionBenchmarkTool tool = new LogIngestionBenchmarkTool();
            tool.runAllBenchmarks();
            tool.cleanup();
            System.out.println("Benchmarks completed successfully. Results saved to " + BENCHMARK_RESULTS_FILE);
        } catch (Exception e) {
            System.err.println("Error running benchmarks: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Constructor to initialize the benchmark tool.
     */
    public LogIngestionBenchmarkTool() throws IOException {
        // Initialize JMX beans for system metrics
        osBean = ManagementFactory.getOperatingSystemMXBean();
        memoryBean = ManagementFactory.getMemoryMXBean();
        
        // Create a temporary directory for the test index
        testIndexPath = Files.createTempDirectory("test-lucene-index");
        
        // Initialize services
        luceneService = new LuceneService();
        luceneService.setIndexPath(testIndexPath.toString());
        luceneService.setPartitioningEnabled(false);
        
        // Create a mock RealTimeUpdateService
        luceneService.setRealTimeUpdateService(new MockRealTimeUpdateService());
        
        try {
            luceneService.init();
        } catch (Exception e) {
            System.err.println("Error initializing LuceneService: " + e.getMessage());
            e.printStackTrace();
        }
        
        logBufferService = new LogBufferService(luceneService);
        logBufferService.setMaxBufferSize(1000);
        logBufferService.setFlushIntervalMs(5000);
        
        // Initialize benchmark results file if it doesn't exist
        initBenchmarkResultsFile();
    }
    
    /**
     * Run all benchmarks.
     */
    public void runAllBenchmarks() throws IOException, InterruptedException {
        // Run small batch benchmarks
        System.out.println("Running small batch benchmarks...");
        List<LogEntry> smallBatchLogs = generateTestLogs(SMALL_BATCH_SIZE);
        Map<String, Object> smallBatchDirectMetrics = measureDirectIngestionPerformance(smallBatchLogs);
        Map<String, Object> smallBatchBufferedMetrics = measureBufferedIngestionPerformance(smallBatchLogs);
        printResults("Small Batch", SMALL_BATCH_SIZE, smallBatchDirectMetrics, smallBatchBufferedMetrics);
        saveBenchmarkResults("SmallBatch", SMALL_BATCH_SIZE, "Direct", smallBatchDirectMetrics);
        saveBenchmarkResults("SmallBatch", SMALL_BATCH_SIZE, "Buffered", smallBatchBufferedMetrics);
        
        // Run medium batch benchmarks
        System.out.println("\nRunning medium batch benchmarks...");
        List<LogEntry> mediumBatchLogs = generateTestLogs(MEDIUM_BATCH_SIZE);
        Map<String, Object> mediumBatchDirectMetrics = measureDirectIngestionPerformance(mediumBatchLogs);
        Map<String, Object> mediumBatchBufferedMetrics = measureBufferedIngestionPerformance(mediumBatchLogs);
        printResults("Medium Batch", MEDIUM_BATCH_SIZE, mediumBatchDirectMetrics, mediumBatchBufferedMetrics);
        saveBenchmarkResults("MediumBatch", MEDIUM_BATCH_SIZE, "Direct", mediumBatchDirectMetrics);
        saveBenchmarkResults("MediumBatch", MEDIUM_BATCH_SIZE, "Buffered", mediumBatchBufferedMetrics);
        
        // Run large batch benchmarks
        System.out.println("\nRunning large batch benchmarks...");
        List<LogEntry> largeBatchLogs = generateTestLogs(LARGE_BATCH_SIZE);
        Map<String, Object> largeBatchDirectMetrics = measureDirectIngestionPerformance(largeBatchLogs);
        Map<String, Object> largeBatchBufferedMetrics = measureBufferedIngestionPerformance(largeBatchLogs);
        printResults("Large Batch", LARGE_BATCH_SIZE, largeBatchDirectMetrics, largeBatchBufferedMetrics);
        saveBenchmarkResults("LargeBatch", LARGE_BATCH_SIZE, "Direct", largeBatchDirectMetrics);
        saveBenchmarkResults("LargeBatch", LARGE_BATCH_SIZE, "Buffered", largeBatchBufferedMetrics);
        
        // Run concurrent benchmarks
        System.out.println("\nRunning concurrent benchmarks...");
        int logsPerThread = MEDIUM_BATCH_SIZE / CONCURRENT_THREADS;
        List<List<LogEntry>> threadLogs = new ArrayList<>();
        for (int i = 0; i < CONCURRENT_THREADS; i++) {
            threadLogs.add(generateTestLogs(logsPerThread));
        }
        Map<String, Object> concurrentDirectMetrics = measureConcurrentDirectIngestionPerformance(threadLogs);
        Map<String, Object> concurrentBufferedMetrics = measureConcurrentBufferedIngestionPerformance(threadLogs);
        printResults("Concurrent", MEDIUM_BATCH_SIZE, concurrentDirectMetrics, concurrentBufferedMetrics);
        saveBenchmarkResults("ConcurrentIngestion", MEDIUM_BATCH_SIZE, "Direct", concurrentDirectMetrics);
        saveBenchmarkResults("ConcurrentIngestion", MEDIUM_BATCH_SIZE, "Buffered", concurrentBufferedMetrics);
    }
    
    /**
     * Print benchmark results to the console.
     */
    private void printResults(String testName, int batchSize, Map<String, Object> directMetrics, Map<String, Object> bufferedMetrics) {
        System.out.println("Ingestion Performance (" + testName + " - " + batchSize + " logs):");
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
    }
    
    /**
     * Clean up resources.
     */
    public void cleanup() throws IOException {
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
}