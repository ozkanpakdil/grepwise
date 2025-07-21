package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.config.MetricsConfig;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the system resource monitoring functionality.
 * Tests the integration between SystemResourceService, MetricsConfig, and Spring's metrics infrastructure.
 */
@SpringBootTest
@ActiveProfiles("test")
public class SystemResourceIntegrationTest {

    @Autowired
    private SystemResourceService systemResourceService;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    public void testSystemResourceMetricsRegistration() {
        // Verify that the SystemResourceService is properly autowired
        assertNotNull(systemResourceService);
        
        // Verify that the MeterRegistry is properly autowired
        assertNotNull(meterRegistry);
        
        // Verify that custom metrics are registered
        assertTrue(meterRegistry.getMeters().stream()
                .anyMatch(meter -> meter.getId().getName().startsWith("grepwise.")));
    }

    @Test
    public void testSystemResourceMetricsCollection() {
        // Collect system metrics
        Map<String, Object> metrics = systemResourceService.collectSystemMetrics();
        
        // Verify that metrics are collected
        assertNotNull(metrics);
        assertFalse(metrics.isEmpty());
        
        // Verify essential metrics
        assertTrue(metrics.containsKey("systemLoadAverage"));
        assertTrue(metrics.containsKey("availableProcessors"));
        assertTrue(metrics.containsKey("heapMemoryUsage"));
        
        // Verify that metrics have reasonable values
        assertTrue((int) metrics.get("availableProcessors") > 0);
        assertTrue((long) metrics.get("heapMemoryUsage") > 0);
    }

    @Test
    public void testResourceEvaluationIntegration() {
        // Run a very short CPU test to verify integration
        Map<String, Object> results = systemResourceService.evaluateCpuIntensiveLoad(1, 1);
        
        // Verify results
        assertNotNull(results);
        assertTrue(results.containsKey("beforeTest"));
        assertTrue(results.containsKey("afterTest"));
        
        // Analyze results
        Map<String, Object> analysis = systemResourceService.analyzeResourceUsage(results);
        assertNotNull(analysis);
        
        // Verify that analysis contains expected metrics
        // Note: We don't assert specific values as they will vary by environment
        if (analysis.containsKey("maxCpuLoad")) {
            assertTrue(analysis.get("maxCpuLoad") instanceof Double);
        }
        
        if (analysis.containsKey("maxHeapUsage")) {
            assertTrue(analysis.get("maxHeapUsage") instanceof Long);
        }
        
        if (analysis.containsKey("maxThreadCount")) {
            assertTrue(analysis.get("maxThreadCount") instanceof Integer);
        }
    }
}