package io.github.ozkanpakdil.grepwise.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SystemResourceService class.
 * Tests the functionality for monitoring and evaluating system resource usage.
 */
@ExtendWith(MockitoExtension.class)
public class SystemResourceServiceTest {

    private SystemResourceService systemResourceService;

    @BeforeEach
    public void setUp() {
        // Use a simple meter registry for testing
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        systemResourceService = new SystemResourceService(meterRegistry);
    }

    @Test
    public void testCollectSystemMetrics() {
        // Test that system metrics can be collected
        Map<String, Object> metrics = systemResourceService.collectSystemMetrics();
        
        // Verify that basic metrics are present
        assertNotNull(metrics);
        assertFalse(metrics.isEmpty());
        
        // Check for essential metrics
        assertTrue(metrics.containsKey("systemLoadAverage"));
        assertTrue(metrics.containsKey("availableProcessors"));
        assertTrue(metrics.containsKey("heapMemoryUsage"));
        assertTrue(metrics.containsKey("heapMemoryMax"));
        assertTrue(metrics.containsKey("threadCount"));
        
        // Verify that metrics have reasonable values
        assertTrue((int) metrics.get("availableProcessors") > 0);
        assertTrue((long) metrics.get("heapMemoryMax") > 0);
        assertTrue((int) metrics.get("threadCount") > 0);
    }

    @Test
    public void testEvaluateCpuIntensiveLoad() {
        // Test with minimal duration to keep test execution time reasonable
        int durationSeconds = 2;
        int threadCount = 2;
        
        // Run CPU-intensive load test
        Map<String, Object> results = systemResourceService.evaluateCpuIntensiveLoad(durationSeconds, threadCount);
        
        // Verify results structure
        assertNotNull(results);
        assertTrue(results.containsKey("beforeTest"));
        assertTrue(results.containsKey("afterTest"));
        assertTrue(results.containsKey("metricSnapshots"));
        
        // Verify that metrics were collected during the test
        @SuppressWarnings("unchecked")
        Map<String, Object> beforeTest = (Map<String, Object>) results.get("beforeTest");
        @SuppressWarnings("unchecked")
        Map<String, Object> afterTest = (Map<String, Object>) results.get("afterTest");
        
        assertNotNull(beforeTest);
        assertNotNull(afterTest);
        
        // Analyze the results
        Map<String, Object> analysis = systemResourceService.analyzeResourceUsage(results);
        assertNotNull(analysis);
    }

    @Test
    public void testEvaluateMemoryIntensiveLoad() {
        // Test with minimal duration and memory to keep test execution time reasonable
        int durationSeconds = 2;
        int memoryMB = 10; // Just allocate a small amount for testing
        
        // Run memory-intensive load test
        Map<String, Object> results = systemResourceService.evaluateMemoryIntensiveLoad(durationSeconds, memoryMB);
        
        // Verify results structure
        assertNotNull(results);
        assertTrue(results.containsKey("beforeTest"));
        assertTrue(results.containsKey("afterTest"));
        assertTrue(results.containsKey("metricSnapshots"));
        
        // Verify that metrics were collected during the test
        @SuppressWarnings("unchecked")
        Map<String, Object> beforeTest = (Map<String, Object>) results.get("beforeTest");
        @SuppressWarnings("unchecked")
        Map<String, Object> afterTest = (Map<String, Object>) results.get("afterTest");
        
        assertNotNull(beforeTest);
        assertNotNull(afterTest);
        
        // Analyze the results
        Map<String, Object> analysis = systemResourceService.analyzeResourceUsage(results);
        assertNotNull(analysis);
    }

    @Test
    public void testEvaluateIoIntensiveLoad() {
        // Test with minimal duration to keep test execution time reasonable
        int durationSeconds = 2;
        int threadCount = 1;
        
        // Run I/O-intensive load test
        Map<String, Object> results = systemResourceService.evaluateIoIntensiveLoad(durationSeconds, threadCount);
        
        // Verify results structure
        assertNotNull(results);
        assertTrue(results.containsKey("beforeTest"));
        assertTrue(results.containsKey("afterTest"));
        assertTrue(results.containsKey("metricSnapshots"));
        
        // Verify that metrics were collected during the test
        @SuppressWarnings("unchecked")
        Map<String, Object> beforeTest = (Map<String, Object>) results.get("beforeTest");
        @SuppressWarnings("unchecked")
        Map<String, Object> afterTest = (Map<String, Object>) results.get("afterTest");
        
        assertNotNull(beforeTest);
        assertNotNull(afterTest);
        
        // Analyze the results
        Map<String, Object> analysis = systemResourceService.analyzeResourceUsage(results);
        assertNotNull(analysis);
    }

    @Test
    public void testAnalyzeResourceUsage() {
        // Create a simple test result map
        Map<String, Object> beforeTest = Map.of(
                "processCpuLoad", 0.1,
                "heapMemoryUsage", 100000000L,
                "threadCount", 10
        );
        
        Map<String, Object> afterTest = Map.of(
                "processCpuLoad", 0.2,
                "heapMemoryUsage", 120000000L,
                "threadCount", 12
        );
        
        Map<String, Object> snapshot1 = Map.of(
                "processCpuLoad", 0.3,
                "heapMemoryUsage", 110000000L,
                "threadCount", 11
        );
        
        Map<String, Object> snapshot2 = Map.of(
                "processCpuLoad", 0.4,
                "heapMemoryUsage", 115000000L,
                "threadCount", 12
        );
        
        Map<String, Object> testResults = Map.of(
                "beforeTest", beforeTest,
                "afterTest", afterTest,
                "metricSnapshots", java.util.List.of(beforeTest, snapshot1, snapshot2)
        );
        
        // Analyze the results
        Map<String, Object> analysis = systemResourceService.analyzeResourceUsage(testResults);
        
        // Verify analysis results
        assertNotNull(analysis);
        assertEquals(0.4, analysis.get("maxCpuLoad"));
        assertEquals(0.26666666666666666, analysis.get("avgCpuLoad"));
        assertEquals(115000000L, analysis.get("maxHeapUsage"));
        assertEquals(108333333L, analysis.get("avgHeapUsage"));
        assertEquals(20000000L, analysis.get("heapUsageDiff"));
        assertEquals(12, analysis.get("maxThreadCount"));
        assertEquals(11.0, analysis.get("avgThreadCount"));
    }
}