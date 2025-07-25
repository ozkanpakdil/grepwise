package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.AnomalyResult;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionServiceTest {

    @Mock
    private LuceneService luceneService;

    @InjectMocks
    private AnomalyDetectionService anomalyDetectionService;

    private List<LogEntry> normalLogs;
    private List<LogEntry> anomalousLogs;
    private List<LogEntry> combinedLogs;
    private long baseTimestamp;

    @BeforeEach
    void setUp() {
        // Configure the service
        ReflectionTestUtils.setField(anomalyDetectionService, "anomalyDetectionEnabled", true);
        ReflectionTestUtils.setField(anomalyDetectionService, "anomalyThreshold", 2.0);
        ReflectionTestUtils.setField(anomalyDetectionService, "minSampleSize", 5);
        ReflectionTestUtils.setField(anomalyDetectionService, "timeWindowMinutes", 60);

        // Create test data
        baseTimestamp = System.currentTimeMillis() - (60 * 60 * 1000); // 1 hour ago
        normalLogs = createNormalLogDistribution();
        anomalousLogs = createAnomalousLogDistribution();
        combinedLogs = new ArrayList<>();
        combinedLogs.addAll(normalLogs);
        combinedLogs.addAll(anomalousLogs);
    }

    @Test
    void testDetectFrequencyAnomalies_NormalDistribution() throws IOException {
        // Mock the LuceneService to return normal logs
        when(luceneService.search(anyString(), anyBoolean(), anyLong(), anyLong()))
                .thenReturn(normalLogs);

        // Test anomaly detection with normal distribution
        List<AnomalyResult> anomalies = anomalyDetectionService.detectFrequencyAnomalies(
                null, null, baseTimestamp, baseTimestamp + (60 * 60 * 1000), 5);

        // Verify no anomalies are detected in normal distribution
        assertTrue(anomalies.isEmpty(), "No anomalies should be detected in a normal distribution");
    }

    @Test
    void testDetectFrequencyAnomalies_AnomalousDistribution() throws IOException {
        // Mock the LuceneService to return combined logs with anomalies
        when(luceneService.search(anyString(), anyBoolean(), anyLong(), anyLong()))
                .thenReturn(combinedLogs);

        // Test anomaly detection with anomalous distribution
        List<AnomalyResult> anomalies = anomalyDetectionService.detectFrequencyAnomalies(
                null, null, baseTimestamp, baseTimestamp + (60 * 60 * 1000), 5);

        // Verify anomalies are detected
        assertFalse(anomalies.isEmpty(), "Anomalies should be detected in an anomalous distribution");
        assertTrue(anomalies.size() > 0, "At least one anomaly should be detected");
        
        // Verify anomaly details
        AnomalyResult anomaly = anomalies.get(0);
        assertTrue(anomaly.score() > anomalyDetectionService.getAnomalyThreshold(), 
                "Anomaly score should be above threshold");
        assertTrue(anomaly.description().contains("Anomalous log frequency detected"), 
                "Anomaly description should mention frequency");
    }

    @Test
    void testDetectFrequencyAnomalies_WithSourceFilter() throws IOException {
        // Mock the LuceneService to return combined logs with anomalies
        when(luceneService.search(anyString(), anyBoolean(), anyLong(), anyLong()))
                .thenReturn(combinedLogs);

        // Test anomaly detection with source filter
        List<AnomalyResult> anomalies = anomalyDetectionService.detectFrequencyAnomalies(
                "app.log", null, baseTimestamp, baseTimestamp + (60 * 60 * 1000), 5);

        // Since our test data has mixed sources, filtering should reduce the number of logs
        // This might affect whether anomalies are detected, but we're mainly testing that
        // the source filter is applied correctly
        assertNotNull(anomalies, "Result should not be null even with source filter");
    }

    @Test
    void testDetectFrequencyAnomalies_WithLevelFilter() throws IOException {
        // Mock the LuceneService to return combined logs with anomalies
        when(luceneService.search(anyString(), anyBoolean(), anyLong(), anyLong()))
                .thenReturn(combinedLogs);

        // Test anomaly detection with level filter
        List<AnomalyResult> anomalies = anomalyDetectionService.detectFrequencyAnomalies(
                null, "ERROR", baseTimestamp, baseTimestamp + (60 * 60 * 1000), 5);

        // Since our test data has mixed levels, filtering should reduce the number of logs
        assertNotNull(anomalies, "Result should not be null even with level filter");
    }

    @Test
    void testDetectFrequencyAnomalies_DisabledService() {
        // Disable anomaly detection
        ReflectionTestUtils.setField(anomalyDetectionService, "anomalyDetectionEnabled", false);

        // Test anomaly detection when disabled
        List<AnomalyResult> anomalies = anomalyDetectionService.detectFrequencyAnomalies(
                null, null, baseTimestamp, baseTimestamp + (60 * 60 * 1000), 5);

        // Verify no anomalies are returned when service is disabled
        assertTrue(anomalies.isEmpty(), "No anomalies should be returned when service is disabled");
    }

    @Test
    void testDetectPatternAnomalies() throws IOException {
        // Create pattern logs
        List<LogEntry> patternLogs = createLogsWithPattern();
        
        // Mock the LuceneService to return pattern logs for pattern query
        when(luceneService.search(eq("*error*"), anyBoolean(), anyLong(), anyLong()))
                .thenReturn(patternLogs.subList(0, 5)); // Only return error logs
                
        // Mock the LuceneService to return all logs for wildcard query
        when(luceneService.search(eq("*"), anyBoolean(), anyLong(), anyLong()))
                .thenReturn(patternLogs);

        // Test pattern anomaly detection
        List<AnomalyResult> anomalies = anomalyDetectionService.detectPatternAnomalies(
                null, baseTimestamp, baseTimestamp + (60 * 60 * 1000), "*error*");

        // Verify results
        assertNotNull(anomalies, "Result should not be null");
    }

    @Test
    void testConfigurationMethods() {
        // Test getter/setter methods
        anomalyDetectionService.setAnomalyThreshold(3.0);
        assertEquals(3.0, anomalyDetectionService.getAnomalyThreshold(), 
                "Anomaly threshold should be updated");
        
        anomalyDetectionService.setAnomalyDetectionEnabled(false);
        assertFalse(anomalyDetectionService.isAnomalyDetectionEnabled(), 
                "Anomaly detection should be disabled");
        
        anomalyDetectionService.setMinSampleSize(10);
        assertEquals(10, anomalyDetectionService.getMinSampleSize(), 
                "Min sample size should be updated");
        
        anomalyDetectionService.setTimeWindowMinutes(120);
        assertEquals(120, anomalyDetectionService.getTimeWindowMinutes(), 
                "Time window should be updated");
    }

    /**
     * Creates a set of logs with a normal distribution pattern.
     */
    private List<LogEntry> createNormalLogDistribution() {
        List<LogEntry> logs = new ArrayList<>();
        
        // Create logs with consistent frequency (10 logs per 5-minute interval)
        for (int i = 0; i < 12; i++) { // 12 intervals = 1 hour
            long intervalStart = baseTimestamp + (i * 5 * 60 * 1000);
            
            // Add 10 logs in each interval
            for (int j = 0; j < 10; j++) {
                long timestamp = intervalStart + (j * 30 * 1000); // Spread over the 5-minute interval
                logs.add(createLogEntry(timestamp, "INFO", "app.log"));
            }
        }
        
        return logs;
    }

    /**
     * Creates a set of logs with an anomalous spike in one interval.
     */
    private List<LogEntry> createAnomalousLogDistribution() {
        List<LogEntry> logs = new ArrayList<>();
        
        // Create an anomalous spike in the middle interval (30-35 minutes)
        long anomalyInterval = baseTimestamp + (6 * 5 * 60 * 1000);
        
        // Add 50 logs in the anomalous interval (5x normal)
        for (int j = 0; j < 50; j++) {
            long timestamp = anomalyInterval + (j * 6 * 1000); // Spread over the 5-minute interval
            logs.add(createLogEntry(timestamp, "ERROR", "system.log"));
        }
        
        return logs;
    }

    /**
     * Creates logs with varying patterns for pattern anomaly detection.
     */
    private List<LogEntry> createLogsWithPattern() {
        List<LogEntry> logs = new ArrayList<>();
        
        // Create 100 logs, with 20% containing "error"
        for (int i = 0; i < 100; i++) {
            long timestamp = baseTimestamp + (i * 36 * 1000); // Spread over the hour
            String level = (i % 5 == 0) ? "ERROR" : "INFO";
            String message = (i % 5 == 0) ? "An error occurred in processing" : "Normal processing completed";
            logs.add(new LogEntry(
                    UUID.randomUUID().toString(),
                    timestamp,
                    level,
                    message,
                    "pattern.log",
                    new HashMap<>(),
                    "Raw log content " + i
            ));
        }
        
        return logs;
    }

    /**
     * Helper method to create a log entry.
     */
    private LogEntry createLogEntry(long timestamp, String level, String source) {
        return new LogEntry(
                UUID.randomUUID().toString(),
                timestamp,
                level,
                "Log message " + UUID.randomUUID().toString().substring(0, 8),
                source,
                new HashMap<>(),
                "Raw log content"
        );
    }
}