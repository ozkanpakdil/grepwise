package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.PredictiveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for PredictiveAnalyticsService.
 * Tests the integration of PredictiveAnalyticsService with LuceneService and other components.
 */
public class PredictiveAnalyticsIntegrationTest {

    private PredictiveAnalyticsService predictiveAnalyticsService;
    private LuceneService luceneService;

    @BeforeEach
    void setUp() throws Exception {
        // Fresh LuceneService with a temp single-index directory
        luceneService = new LuceneService();
        Path tempIndex = Files.createTempDirectory("test-lucene-index");
        luceneService.setIndexPath(tempIndex.toString());
        luceneService.setPartitioningEnabled(false);
        luceneService.setRealTimeUpdateService(null);
        // Provide minimal dependencies for LuceneService
        luceneService.setFieldConfigurationService(new FieldConfigurationService(new io.github.ozkanpakdil.grepwise.repository.FieldConfigurationRepository()));
        // Provide a simple in-memory search cache service
        luceneService.setSearchCacheService(new SearchCacheService());
        luceneService.init();

        // Create PredictiveAnalyticsService and inject luceneService
        predictiveAnalyticsService = new PredictiveAnalyticsService();
        ReflectionTestUtils.setField(predictiveAnalyticsService, "luceneService", luceneService);
        ReflectionTestUtils.setField(predictiveAnalyticsService, "predictiveAnalyticsEnabled", true);
        ReflectionTestUtils.setField(predictiveAnalyticsService, "minSampleSize", 5);

        // Create test logs with increasing frequency pattern
        List<LogEntry> testLogs = new ArrayList<>();
        long baseTime = System.currentTimeMillis() - (24 * 60 * 60 * 1000); // 24 hours ago

        // Create logs with a clear increasing trend over 24 hours
        for (int hour = 0; hour < 24; hour++) {
            // More logs as we get closer to present time
            int logsPerHour = 5 + hour;

            for (int i = 0; i < logsPerHour; i++) {
                testLogs.add(createLogEntry(
                        baseTime + (hour * 60 * 60 * 1000) + (i * 60 * 1000),
                        hour % 3 == 0 ? "ERROR" : (hour % 3 == 1 ? "WARN" : "INFO"),
                        "test-source"
                ));
            }
        }

        // Index test logs
        luceneService.indexLogEntries(testLogs);
    }

    @Test
    void testPredictLogVolume() throws Exception {
        // Act
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                null, null, 
                System.currentTimeMillis() - (24 * 60 * 60 * 1000), 
                System.currentTimeMillis(),
                60, 6); // Predict 6 hours ahead in 1-hour intervals
        
        // Assert
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
        assertEquals(6, predictions.size());
        
        // Verify prediction structure
        for (PredictiveResult prediction : predictions) {
            assertEquals("VOLUME", prediction.predictionType());
            assertTrue(prediction.predictionTimestamp() > System.currentTimeMillis());
            assertTrue(prediction.predictedValue() > 0);
            assertTrue(prediction.confidenceLevel() > 0 && prediction.confidenceLevel() <= 1.0);
            assertNotNull(prediction.description());
        }
    }

    @Test
    void testPredictLogTrend() throws Exception {
        // Act
        PredictiveResult prediction = predictiveAnalyticsService.predictLogTrend(
                null, null, 
                System.currentTimeMillis() - (24 * 60 * 60 * 1000), 
                System.currentTimeMillis(),
                60); // 1-hour intervals
        
        // Assert
        assertNotNull(prediction);
        assertEquals("TREND", prediction.predictionType());
        
        // Since we created logs with an increasing pattern, trend should be increasing
        Map<String, Object> metadata = prediction.metadata();
        assertNotNull(metadata);
        assertEquals("INCREASING", metadata.get("trendDirection"));
        
        // Slope should be positive
        assertTrue((Double) metadata.get("slope") > 0);
        
        // R-squared should be high (good fit)
        assertTrue((Double) metadata.get("rSquared") > 0.5);
    }

    @Test
    void testPredictLogLevelDistribution() throws Exception {
        // Act
        PredictiveResult prediction = predictiveAnalyticsService.predictLogLevelDistribution(
                null, 
                System.currentTimeMillis() - (24 * 60 * 60 * 1000), 
                System.currentTimeMillis());
        
        // Assert
        assertNotNull(prediction);
        assertEquals("LEVEL_DISTRIBUTION", prediction.predictionType());
        
        // Verify distribution data
        Map<String, Object> metadata = prediction.metadata();
        assertNotNull(metadata);
        
        @SuppressWarnings("unchecked")
        Map<String, Double> distribution = (Map<String, Double>) metadata.get("distribution");
        assertNotNull(distribution);
        assertFalse(distribution.isEmpty());
        
        // Should have entries for our log levels
        assertTrue(distribution.containsKey("INFO"));
        assertTrue(distribution.containsKey("WARN"));
        assertTrue(distribution.containsKey("ERROR"));
        
        // Sum of percentages should be close to 100%
        double sum = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        assertEquals(100.0, sum, 0.1);
    }

    @Test
    void testPredictLogVolumeWithSourceFilter() throws Exception {
        // Act
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                "test-source", null, 
                System.currentTimeMillis() - (24 * 60 * 60 * 1000), 
                System.currentTimeMillis(),
                60, 3);
        
        // Assert
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
    }

    @Test
    void testPredictLogVolumeWithLevelFilter() throws Exception {
        // Act
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                null, "ERROR", 
                System.currentTimeMillis() - (24 * 60 * 60 * 1000), 
                System.currentTimeMillis(),
                60, 3);
        
        // Assert
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
    }

    @Test
    void testPerformanceWithLargeDataVolume() throws Exception {
        // Add more logs to test performance with larger volume
        long baseTime = System.currentTimeMillis() - (12 * 60 * 60 * 1000); // 12 hours ago
        
        List<LogEntry> performanceLogs = new ArrayList<>();
        for (int minute = 0; minute < 720; minute++) { // 12 hours in minutes
            performanceLogs.add(createLogEntry(
                    baseTime + (minute * 60 * 1000),
                    "INFO",
                    "performance-test-source"
            ));
        }
        
        // Index performance test logs
        luceneService.indexLogEntries(performanceLogs);
        
        // Measure time for prediction
        long startTime = System.currentTimeMillis();
        
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                "performance-test-source", null, 
                baseTime, 
                System.currentTimeMillis(),
                30, 6);
        
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;
        
        // Assert
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
        
        // Performance should be reasonable (under 5 seconds)
        assertTrue(executionTime < 5000, "Prediction took too long: " + executionTime + "ms");
        
        System.out.println("Performance test completed in " + executionTime + "ms");
    }

    /**
     * Helper method to create a test log entry.
     */
    private LogEntry createLogEntry(long timestamp, String level, String source) {
        return new LogEntry(
                UUID.randomUUID().toString(),
                timestamp,
                null,
                level,
                "Test log message " + timestamp,
                source,
                new HashMap<>(),
                "Raw content " + timestamp
        );
    }
}