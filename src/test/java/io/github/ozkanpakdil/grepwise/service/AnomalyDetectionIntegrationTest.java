package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.AnomalyResult;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for AnomalyDetectionService.
 * Tests the integration of AnomalyDetectionService with LuceneService and other components.
 */
@SpringBootTest
@ActiveProfiles("test")
class AnomalyDetectionIntegrationTest {

    @Autowired
    private AnomalyDetectionService anomalyDetectionService;
    
    @Autowired
    private LuceneService luceneService;
    
    private long baseTimestamp;
    private List<LogEntry> testLogs;

    @BeforeEach
    void setUp() throws IOException {
        // Clear any existing logs
        luceneService.close();
        
        // Create test data
        baseTimestamp = System.currentTimeMillis() - (60 * 60 * 1000); // 1 hour ago
        testLogs = createTestLogs();
        
        // Index test logs
        luceneService.indexLogEntries(testLogs);
    }

    @Test
    void testFrequencyAnomalyDetection() {
        // Test anomaly detection with real indexed logs
        List<AnomalyResult> anomalies = anomalyDetectionService.detectFrequencyAnomalies(
                null, null, baseTimestamp, baseTimestamp + (60 * 60 * 1000), 5);
        
        // Verify results
        assertNotNull(anomalies, "Anomaly detection result should not be null");
    }
    
    @Test
    void testPatternAnomalyDetection() {
        // Test pattern anomaly detection with real indexed logs
        List<AnomalyResult> anomalies = anomalyDetectionService.detectPatternAnomalies(
                null, baseTimestamp, baseTimestamp + (60 * 60 * 1000), "error");
        
        // Verify results
        assertNotNull(anomalies, "Pattern anomaly detection result should not be null");
    }
    
    @Test
    void testAnomalyDetectionWithSourceFilter() {
        // Test anomaly detection with source filter
        List<AnomalyResult> anomalies = anomalyDetectionService.detectFrequencyAnomalies(
                "test.log", null, baseTimestamp, baseTimestamp + (60 * 60 * 1000), 5);
        
        // Verify results
        assertNotNull(anomalies, "Anomaly detection with source filter should not return null");
    }
    
    @Test
    void testAnomalyDetectionWithLevelFilter() {
        // Test anomaly detection with level filter
        List<AnomalyResult> anomalies = anomalyDetectionService.detectFrequencyAnomalies(
                null, "ERROR", baseTimestamp, baseTimestamp + (60 * 60 * 1000), 5);
        
        // Verify results
        assertNotNull(anomalies, "Anomaly detection with level filter should not return null");
    }

    /**
     * Creates test logs with varying patterns for testing.
     */
    private List<LogEntry> createTestLogs() {
        List<LogEntry> logs = new ArrayList<>();
        
        // Create normal distribution logs
        for (int i = 0; i < 12; i++) { // 12 intervals = 1 hour
            long intervalStart = baseTimestamp + (i * 5 * 60 * 1000);
            
            // Add 10 logs in each interval
            for (int j = 0; j < 10; j++) {
                long timestamp = intervalStart + (j * 30 * 1000);
                logs.add(createLogEntry(timestamp, "INFO", "test.log"));
            }
        }
        
        // Create an anomalous spike in one interval
        long anomalyInterval = baseTimestamp + (6 * 5 * 60 * 1000);
        for (int j = 0; j < 50; j++) {
            long timestamp = anomalyInterval + (j * 6 * 1000);
            logs.add(createLogEntry(timestamp, "ERROR", "system.log"));
        }
        
        return logs;
    }

    /**
     * Helper method to create a log entry.
     */
    private LogEntry createLogEntry(long timestamp, String level, String source) {
        String message = level.equals("ERROR") ? 
                "An error occurred in processing: " + UUID.randomUUID() : 
                "Normal processing completed: " + UUID.randomUUID();
                
        return new LogEntry(
                UUID.randomUUID().toString(),
                timestamp,
                level,
                message,
                source,
                new HashMap<>(),
                "Raw log content"
        );
    }
}