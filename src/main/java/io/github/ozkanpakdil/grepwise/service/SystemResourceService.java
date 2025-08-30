package io.github.ozkanpakdil.grepwise.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Service for monitoring and evaluating system resource usage under various conditions.
 * This service provides methods to:
 * 1. Collect detailed system metrics (CPU, memory, disk, threads)
 * 2. Simulate different load conditions
 * 3. Evaluate and report resource usage under these conditions
 */
@Service
public class SystemResourceService {
    private static final Logger logger = LoggerFactory.getLogger(SystemResourceService.class);

    private final MeterRegistry meterRegistry;
    private final OperatingSystemMXBean osBean;
    private final MemoryMXBean memoryBean;
    private final ThreadMXBean threadBean;

    @Autowired
    public SystemResourceService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.osBean = ManagementFactory.getOperatingSystemMXBean();
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.threadBean = ManagementFactory.getThreadMXBean();
    }

    /**
     * Collects current system resource metrics
     *
     * @return Map containing various system metrics
     */
    public Map<String, Object> collectSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // CPU metrics
        metrics.put("systemLoadAverage", osBean.getSystemLoadAverage());
        metrics.put("availableProcessors", osBean.getAvailableProcessors());

        // Memory metrics
        metrics.put("heapMemoryUsage", memoryBean.getHeapMemoryUsage().getUsed());
        metrics.put("heapMemoryMax", memoryBean.getHeapMemoryUsage().getMax());
        metrics.put("nonHeapMemoryUsage", memoryBean.getNonHeapMemoryUsage().getUsed());

        // Thread metrics
        metrics.put("threadCount", threadBean.getThreadCount());
        metrics.put("peakThreadCount", threadBean.getPeakThreadCount());
        metrics.put("daemonThreadCount", threadBean.getDaemonThreadCount());

        // If running on a JVM that supports these operations
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOsBean) {
            metrics.put("processCpuLoad", sunOsBean.getProcessCpuLoad());
            metrics.put("systemCpuLoad", sunOsBean.getCpuLoad());
            metrics.put("processCpuTime", sunOsBean.getProcessCpuTime());
            metrics.put("freePhysicalMemory", sunOsBean.getFreeMemorySize());
            metrics.put("totalPhysicalMemory", sunOsBean.getTotalMemorySize());
            metrics.put("committedVirtualMemory", sunOsBean.getCommittedVirtualMemorySize());
        }

        return metrics;
    }

    /**
     * Evaluates system resource usage under CPU-intensive load
     *
     * @param durationSeconds how long to run the test
     * @param threadCount     number of threads to use
     * @return Map containing resource usage metrics before, during, and after the test
     */
    public Map<String, Object> evaluateCpuIntensiveLoad(int durationSeconds, int threadCount) {
        logger.info("Starting CPU-intensive load test with {} threads for {} seconds", threadCount, durationSeconds);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> metricSnapshots = new ArrayList<>();

        // Collect metrics before the test
        Map<String, Object> beforeMetrics = collectSystemMetrics();
        result.put("beforeTest", beforeMetrics);
        metricSnapshots.add(beforeMetrics);

        // Create and start worker threads
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicBoolean running = new AtomicBoolean(true);

        Instant startTime = Instant.now();

        // Start CPU-intensive tasks
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                while (running.get()) {
                    // CPU-intensive computation (calculating prime numbers)
                    for (int j = 2; j < 100000; j++) {
                        boolean isPrime = true;
                        for (int k = 2; k <= Math.sqrt(j); k++) {
                            if (j % k == 0) {
                                isPrime = false;
                                break;
                            }
                        }
                    }
                }
            });
        }

        // Collect metrics during the test at regular intervals
        CompletableFuture.runAsync(() -> {
            try {
                while (Duration.between(startTime, Instant.now()).getSeconds() < durationSeconds) {
                    Map<String, Object> duringMetrics = collectSystemMetrics();
                    metricSnapshots.add(duringMetrics);
                    Thread.sleep(1000); // Collect metrics every second
                }
                running.set(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wait for the test to complete
        try {
            executor.shutdown();
            executor.awaitTermination(durationSeconds + 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("CPU load test was interrupted", e);
        }

        // Collect metrics after the test
        Map<String, Object> afterMetrics = collectSystemMetrics();
        result.put("afterTest", afterMetrics);
        result.put("metricSnapshots", metricSnapshots);

        logger.info("Completed CPU-intensive load test");
        return result;
    }

    /**
     * Evaluates system resource usage under memory-intensive load
     *
     * @param durationSeconds how long to run the test
     * @param memoryMB        approximate amount of memory to allocate in MB
     * @return Map containing resource usage metrics before, during, and after the test
     */
    public Map<String, Object> evaluateMemoryIntensiveLoad(int durationSeconds, int memoryMB) {
        logger.info("Starting memory-intensive load test with {}MB for {} seconds", memoryMB, durationSeconds);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> metricSnapshots = new ArrayList<>();

        // Collect metrics before the test
        Map<String, Object> beforeMetrics = collectSystemMetrics();
        result.put("beforeTest", beforeMetrics);
        metricSnapshots.add(beforeMetrics);

        // Calculate number of objects to create (each ~1MB)
        int objectCount = memoryMB;
        List<byte[]> memoryConsumers = new ArrayList<>();

        Instant startTime = Instant.now();

        // Start memory allocation in a separate thread
        CompletableFuture<Void> allocationFuture = CompletableFuture.runAsync(() -> {
            try {
                // Gradually allocate memory
                for (int i = 0; i < objectCount && Duration.between(startTime, Instant.now()).getSeconds() < durationSeconds; i++) {
                    memoryConsumers.add(new byte[1024 * 1024]); // Allocate 1MB
                    Thread.sleep(100); // Slow down allocation to avoid OutOfMemoryError
                }

                // Hold the memory for the remaining duration
                long remainingSeconds = durationSeconds - Duration.between(startTime, Instant.now()).getSeconds();
                if (remainingSeconds > 0) {
                    Thread.sleep(remainingSeconds * 1000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Collect metrics during the test at regular intervals
        CompletableFuture.runAsync(() -> {
            try {
                while (Duration.between(startTime, Instant.now()).getSeconds() < durationSeconds) {
                    Map<String, Object> duringMetrics = collectSystemMetrics();
                    metricSnapshots.add(duringMetrics);
                    Thread.sleep(1000); // Collect metrics every second
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wait for the test to complete
        try {
            allocationFuture.get(durationSeconds + 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.error("Memory load test encountered an error", e);
        }

        // Clear memory
        memoryConsumers.clear();
        System.gc();

        // Collect metrics after the test
        Map<String, Object> afterMetrics = collectSystemMetrics();
        result.put("afterTest", afterMetrics);
        result.put("metricSnapshots", metricSnapshots);

        logger.info("Completed memory-intensive load test");
        return result;
    }

    /**
     * Evaluates system resource usage under I/O-intensive load
     *
     * @param durationSeconds how long to run the test
     * @param threadCount     number of threads to use
     * @return Map containing resource usage metrics before, during, and after the test
     */
    public Map<String, Object> evaluateIoIntensiveLoad(int durationSeconds, int threadCount) {
        logger.info("Starting I/O-intensive load test with {} threads for {} seconds", threadCount, durationSeconds);

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> metricSnapshots = new ArrayList<>();

        // Collect metrics before the test
        Map<String, Object> beforeMetrics = collectSystemMetrics();
        result.put("beforeTest", beforeMetrics);
        metricSnapshots.add(beforeMetrics);

        // Create and start worker threads
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicBoolean running = new AtomicBoolean(true);

        Instant startTime = Instant.now();

        // Start I/O-intensive tasks
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    java.nio.file.Path tempFile = java.nio.file.Files.createTempFile("grepwise-io-test", ".tmp");
                    while (running.get()) {
                        // Write data to file
                        java.nio.file.Files.write(tempFile, new byte[1024 * 1024]); // Write 1MB

                        // Read data from file
                        java.nio.file.Files.readAllBytes(tempFile);
                    }
                    // Clean up
                    java.nio.file.Files.deleteIfExists(tempFile);
                } catch (Exception e) {
                    logger.error("Error in I/O test thread", e);
                }
            });
        }

        // Collect metrics during the test at regular intervals
        CompletableFuture.runAsync(() -> {
            try {
                while (Duration.between(startTime, Instant.now()).getSeconds() < durationSeconds) {
                    Map<String, Object> duringMetrics = collectSystemMetrics();
                    metricSnapshots.add(duringMetrics);
                    Thread.sleep(1000); // Collect metrics every second
                }
                running.set(false);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Wait for the test to complete
        try {
            executor.shutdown();
            executor.awaitTermination(durationSeconds + 5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("I/O load test was interrupted", e);
        }

        // Collect metrics after the test
        Map<String, Object> afterMetrics = collectSystemMetrics();
        result.put("afterTest", afterMetrics);
        result.put("metricSnapshots", metricSnapshots);

        logger.info("Completed I/O-intensive load test");
        return result;
    }

    /**
     * Analyzes the results of a resource evaluation test
     *
     * @param testResults the results from one of the evaluation methods
     * @return Map containing analysis of the resource usage
     */
    public Map<String, Object> analyzeResourceUsage(Map<String, Object> testResults) {
        Map<String, Object> analysis = new HashMap<>();

        @SuppressWarnings("unchecked")
        Map<String, Object> beforeTest = (Map<String, Object>) testResults.get("beforeTest");

        @SuppressWarnings("unchecked")
        Map<String, Object> afterTest = (Map<String, Object>) testResults.get("afterTest");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> snapshots = (List<Map<String, Object>>) testResults.get("metricSnapshots");

        // Calculate CPU usage statistics
        if (beforeTest.containsKey("processCpuLoad") && snapshots != null && !snapshots.isEmpty()) {
            double maxCpuLoad = snapshots.stream()
                    .mapToDouble(snapshot -> (double) snapshot.getOrDefault("processCpuLoad", 0.0))
                    .max()
                    .orElse(0.0);

            double avgCpuLoad = snapshots.stream()
                    .mapToDouble(snapshot -> (double) snapshot.getOrDefault("processCpuLoad", 0.0))
                    .average()
                    .orElse(0.0);

            analysis.put("maxCpuLoad", maxCpuLoad);
            analysis.put("avgCpuLoad", avgCpuLoad);
        }

        // Calculate memory usage statistics
        if (beforeTest.containsKey("heapMemoryUsage") && snapshots != null && !snapshots.isEmpty()) {
            long maxHeapUsage = snapshots.stream()
                    .mapToLong(snapshot -> (long) snapshot.getOrDefault("heapMemoryUsage", 0L))
                    .max()
                    .orElse(0L);

            long avgHeapUsage = (long) snapshots.stream()
                    .mapToLong(snapshot -> (long) snapshot.getOrDefault("heapMemoryUsage", 0L))
                    .average()
                    .orElse(0.0);

            long heapUsageDiff = (long) afterTest.getOrDefault("heapMemoryUsage", 0L) -
                    (long) beforeTest.getOrDefault("heapMemoryUsage", 0L);

            analysis.put("maxHeapUsage", maxHeapUsage);
            analysis.put("avgHeapUsage", avgHeapUsage);
            analysis.put("heapUsageDiff", heapUsageDiff);
        }

        // Calculate thread usage statistics
        if (beforeTest.containsKey("threadCount") && snapshots != null && !snapshots.isEmpty()) {
            int maxThreadCount = snapshots.stream()
                    .mapToInt(snapshot -> (int) snapshot.getOrDefault("threadCount", 0))
                    .max()
                    .orElse(0);

            double avgThreadCount = snapshots.stream()
                    .mapToInt(snapshot -> (int) snapshot.getOrDefault("threadCount", 0))
                    .average()
                    .orElse(0.0);

            analysis.put("maxThreadCount", maxThreadCount);
            analysis.put("avgThreadCount", avgThreadCount);
        }

        return analysis;
    }
}