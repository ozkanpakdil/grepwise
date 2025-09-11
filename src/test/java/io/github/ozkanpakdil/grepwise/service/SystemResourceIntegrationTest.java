package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.config.MetricsConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the system resource monitoring functionality.
 * Tests the integration between SystemResourceService and MetricsConfig using a real MeterRegistry.
 *
 * Note: This test avoids bootstrapping the full Spring context to keep it lightweight and reliable.
 */
public class SystemResourceIntegrationTest {

    private static SystemResourceService systemResourceService;
    private static MeterRegistry meterRegistry;

    @BeforeAll
    public static void setUp() {
        // Use a SimpleMeterRegistry for testing
        meterRegistry = new SimpleMeterRegistry();

        // Register application-level metrics from MetricsConfig
        MetricsConfig metricsConfig = new MetricsConfig();
        metricsConfig.applicationMetrics().bindTo(meterRegistry);

        // Create the service under test
        systemResourceService = new SystemResourceService(meterRegistry);
    }

    @Test
    public void testSystemResourceMetricsRegistration() {
        // Verify that the MeterRegistry is initialized
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