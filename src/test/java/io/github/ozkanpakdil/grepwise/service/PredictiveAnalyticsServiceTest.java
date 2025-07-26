package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.PredictiveResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the PredictiveAnalyticsService.
 */
@ExtendWith(MockitoExtension.class)
class PredictiveAnalyticsServiceTest {

    @Mock
    private LuceneService luceneService;

    @InjectMocks
    private PredictiveAnalyticsService predictiveAnalyticsService;

    private List<LogEntry> testLogs;

    @BeforeEach
    void setUp() {
        // Configure service properties
        ReflectionTestUtils.setField(predictiveAnalyticsService, "predictiveAnalyticsEnabled", true);
        ReflectionTestUtils.setField(predictiveAnalyticsService, "minSampleSize", 5);
        ReflectionTestUtils.setField(predictiveAnalyticsService, "timeWindowMinutes", 60);
        ReflectionTestUtils.setField(predictiveAnalyticsService, "forecastHorizonMinutes", 120);

        // Create test logs
        testLogs = new ArrayList<>();
        long baseTime = System.currentTimeMillis() - (60 * 60 * 1000); // 1 hour ago
        
        // Create logs with increasing frequency
        for (int i = 0; i < 30; i++) {
            // Add more logs as time gets closer to present
            int logsInInterval = 5 + i / 2;
            for (int j = 0; j < logsInInterval; j++) {
                testLogs.add(createLogEntry(
                        baseTime + (i * 2 * 60 * 1000) + (j * 1000),
                        i % 3 == 0 ? "ERROR" : (i % 3 == 1 ? "WARN" : "INFO"),
                        "test-source"
                ));
            }
        }
    }

    @Test
    void predictLogVolume_ShouldReturnPredictions_WhenDataIsAvailable() throws Exception {
        // Arrange
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(testLogs);

        // Act
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                null, null, 
                System.currentTimeMillis() - (60 * 60 * 1000), 
                System.currentTimeMillis(),
                5, 3);

        // Assert
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
        assertEquals(3, predictions.size());
        
        // Verify prediction structure
        PredictiveResult firstPrediction = predictions.get(0);
        assertEquals("VOLUME", firstPrediction.predictionType());
        assertTrue(firstPrediction.predictionTimestamp() > System.currentTimeMillis());
        assertTrue(firstPrediction.predictedValue() > 0);
        assertTrue(firstPrediction.confidenceLevel() > 0 && firstPrediction.confidenceLevel() <= 1.0);
        assertNotNull(firstPrediction.description());
    }

    @Test
    void predictLogVolume_ShouldFilterBySource() throws Exception {
        // Arrange
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(testLogs);

        // Act
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                "test-source", null, 
                System.currentTimeMillis() - (60 * 60 * 1000), 
                System.currentTimeMillis(),
                5, 3);

        // Assert
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
    }

    @Test
    void predictLogVolume_ShouldFilterByLevel() throws Exception {
        // Arrange
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(testLogs);

        // Act
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                null, "ERROR", 
                System.currentTimeMillis() - (60 * 60 * 1000), 
                System.currentTimeMillis(),
                5, 3);

        // Assert
        assertNotNull(predictions);
        assertFalse(predictions.isEmpty());
    }

    @Test
    void predictLogVolume_ShouldReturnEmptyList_WhenNoData() throws Exception {
        // Arrange
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                null, null, 
                System.currentTimeMillis() - (60 * 60 * 1000), 
                System.currentTimeMillis(),
                5, 3);

        // Assert
        assertNotNull(predictions);
        assertTrue(predictions.isEmpty());
    }

    @Test
    void predictLogVolume_ShouldReturnEmptyList_WhenPredictiveAnalyticsDisabled() throws Exception {
        // Arrange
        ReflectionTestUtils.setField(predictiveAnalyticsService, "predictiveAnalyticsEnabled", false);

        // Act
        List<PredictiveResult> predictions = predictiveAnalyticsService.predictLogVolume(
                null, null, null, null, 5, 3);

        // Assert
        assertNotNull(predictions);
        assertTrue(predictions.isEmpty());
    }

    @Test
    void predictLogTrend_ShouldReturnTrendPrediction_WhenDataIsAvailable() throws Exception {
        // Arrange
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(testLogs);

        // Act
        PredictiveResult prediction = predictiveAnalyticsService.predictLogTrend(
                null, null, 
                System.currentTimeMillis() - (60 * 60 * 1000), 
                System.currentTimeMillis(),
                5);

        // Assert
        assertNotNull(prediction);
        assertEquals("TREND", prediction.predictionType());
        assertTrue(prediction.predictionTimestamp() > System.currentTimeMillis());
        assertNotNull(prediction.description());
        
        // Verify metadata contains trend information
        assertNotNull(prediction.metadata());
        assertTrue(prediction.metadata().containsKey("trendDirection"));
        assertTrue(prediction.metadata().containsKey("slope"));
        assertTrue(prediction.metadata().containsKey("rSquared"));
    }

    @Test
    void predictLogTrend_ShouldReturnNull_WhenNoData() throws Exception {
        // Arrange
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        PredictiveResult prediction = predictiveAnalyticsService.predictLogTrend(
                null, null, 
                System.currentTimeMillis() - (60 * 60 * 1000), 
                System.currentTimeMillis(),
                5);

        // Assert
        assertNull(prediction);
    }

    @Test
    void predictLogLevelDistribution_ShouldReturnDistributionPrediction_WhenDataIsAvailable() throws Exception {
        // Arrange
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(testLogs);

        // Act
        PredictiveResult prediction = predictiveAnalyticsService.predictLogLevelDistribution(
                null, 
                System.currentTimeMillis() - (60 * 60 * 1000), 
                System.currentTimeMillis());

        // Assert
        assertNotNull(prediction);
        assertEquals("LEVEL_DISTRIBUTION", prediction.predictionType());
        assertTrue(prediction.predictionTimestamp() > System.currentTimeMillis());
        
        // Verify metadata contains distribution information
        assertNotNull(prediction.metadata());
        assertTrue(prediction.metadata().containsKey("distribution"));
        
        @SuppressWarnings("unchecked")
        Map<String, Double> distribution = (Map<String, Double>) prediction.metadata().get("distribution");
        assertNotNull(distribution);
        assertFalse(distribution.isEmpty());
        
        // Should have entries for our log levels
        assertTrue(distribution.containsKey("INFO") || distribution.containsKey("WARN") || distribution.containsKey("ERROR"));
    }

    @Test
    void predictLogLevelDistribution_ShouldReturnNull_WhenNoData() throws Exception {
        // Arrange
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        PredictiveResult prediction = predictiveAnalyticsService.predictLogLevelDistribution(
                null, 
                System.currentTimeMillis() - (60 * 60 * 1000), 
                System.currentTimeMillis());

        // Assert
        assertNull(prediction);
    }

    @Test
    void testConfigurationMethods() {
        // Test setters and getters
        predictiveAnalyticsService.setPredictiveAnalyticsEnabled(false);
        assertFalse(predictiveAnalyticsService.isPredictiveAnalyticsEnabled(),
                "Predictive analytics should be disabled");

        predictiveAnalyticsService.setMinSampleSize(10);
        assertEquals(10, predictiveAnalyticsService.getMinSampleSize(),
                "Min sample size should be updated");

        predictiveAnalyticsService.setTimeWindowMinutes(120);
        assertEquals(120, predictiveAnalyticsService.getTimeWindowMinutes(),
                "Time window should be updated");

        predictiveAnalyticsService.setForecastHorizonMinutes(240);
        assertEquals(240, predictiveAnalyticsService.getForecastHorizonMinutes(),
                "Forecast horizon should be updated");
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