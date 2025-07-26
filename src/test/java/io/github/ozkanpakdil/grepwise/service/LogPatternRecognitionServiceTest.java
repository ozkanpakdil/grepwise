package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LogPatternRecognitionServiceTest {

    @Mock
    private LuceneService luceneService;

    @InjectMocks
    private LogPatternRecognitionService patternRecognitionService;

    private List<LogEntry> testLogEntries;

    @BeforeEach
    void setUp() {
        testLogEntries = createTestLogEntries();
    }

    @Test
    void testRecognizePattern_WithUUID() {
        String message = "User with ID 123e4567-e89b-12d3-a456-426614174000 logged in successfully";
        LogPatternRecognitionService.LogPattern pattern = patternRecognitionService.recognizePattern(message);
        
        assertEquals("User with ID {{UUID}} logged in successfully", pattern.template());
        assertTrue(pattern.variables().containsKey("UUID"));
        assertEquals("123e4567-e89b-12d3-a456-426614174000", pattern.variables().get("UUID").get(0));
    }

    @Test
    void testRecognizePattern_WithIPAddress() {
        String message = "Connection from 192.168.1.1 was rejected";
        LogPatternRecognitionService.LogPattern pattern = patternRecognitionService.recognizePattern(message);
        
        assertEquals("Connection from {{IP_ADDRESS}} was rejected", pattern.template());
        assertTrue(pattern.variables().containsKey("IP_ADDRESS"));
        assertEquals("192.168.1.1", pattern.variables().get("IP_ADDRESS").get(0));
    }

    @Test
    void testRecognizePattern_WithEmail() {
        String message = "Email sent to user@example.com successfully";
        LogPatternRecognitionService.LogPattern pattern = patternRecognitionService.recognizePattern(message);
        
        assertEquals("Email sent to {{EMAIL}} successfully", pattern.template());
        assertTrue(pattern.variables().containsKey("EMAIL"));
        assertEquals("user@example.com", pattern.variables().get("EMAIL").get(0));
    }

    @Test
    void testRecognizePattern_WithURL() {
        String message = "Resource downloaded from https://example.com/resource.zip";
        LogPatternRecognitionService.LogPattern pattern = patternRecognitionService.recognizePattern(message);
        
        assertEquals("Resource downloaded from {{URL}}", pattern.template());
        assertTrue(pattern.variables().containsKey("URL"));
        assertEquals("https://example.com/resource.zip", pattern.variables().get("URL").get(0));
    }

    @Test
    void testRecognizePattern_WithTimestamp() {
        String message = "Scheduled task started at 2023-07-26T14:30:45.123Z";
        LogPatternRecognitionService.LogPattern pattern = patternRecognitionService.recognizePattern(message);
        
        assertEquals("Scheduled task started at {{TIMESTAMP}}", pattern.template());
        assertTrue(pattern.variables().containsKey("TIMESTAMP"));
        assertEquals("2023-07-26T14:30:45.123Z", pattern.variables().get("TIMESTAMP").get(0));
    }

    @Test
    void testRecognizePattern_WithNumber() {
        String message = "Process completed with exit code 0";
        LogPatternRecognitionService.LogPattern pattern = patternRecognitionService.recognizePattern(message);
        
        assertEquals("Process completed with exit code {{NUMBER}}", pattern.template());
        assertTrue(pattern.variables().containsKey("NUMBER"));
        assertEquals("0", pattern.variables().get("NUMBER").get(0));
    }

    @Test
    void testRecognizePattern_WithMultipleVariables() {
        String message = "User 12345 from 192.168.1.1 accessed resource at 2023-07-26T14:30:45Z";
        LogPatternRecognitionService.LogPattern pattern = patternRecognitionService.recognizePattern(message);
        
        assertEquals("User {{NUMBER}} from {{IP_ADDRESS}} accessed resource at {{TIMESTAMP}}", pattern.template());
        assertTrue(pattern.variables().containsKey("NUMBER"));
        assertTrue(pattern.variables().containsKey("IP_ADDRESS"));
        assertTrue(pattern.variables().containsKey("TIMESTAMP"));
        assertEquals("12345", pattern.variables().get("NUMBER").get(0));
        assertEquals("192.168.1.1", pattern.variables().get("IP_ADDRESS").get(0));
        assertEquals("2023-07-26T14:30:45Z", pattern.variables().get("TIMESTAMP").get(0));
    }

    @Test
    void testAnalyzeLogPatterns() {
        Map<String, Integer> patternCounts = patternRecognitionService.analyzeLogPatterns(testLogEntries);
        
        assertEquals(3, patternCounts.size());
        assertEquals(2, patternCounts.get("User {{NUMBER}} logged in from {{IP_ADDRESS}}"));
        assertEquals(1, patternCounts.get("Failed login attempt from {{IP_ADDRESS}} for user {{NUMBER}}"));
        assertEquals(2, patternCounts.get("Database query took {{NUMBER}} ms"));
    }

    @Test
    void testFindLogsByPattern() throws IOException {
        String patternTemplate = "User {{NUMBER}} logged in from {{IP_ADDRESS}}";
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(testLogEntries);
        
        List<LogEntry> matchingLogs = patternRecognitionService.findLogsByPattern(patternTemplate, null, null);
        
        assertEquals(2, matchingLogs.size());
        assertEquals("User 123 logged in from 192.168.1.1", matchingLogs.get(0).message());
        assertEquals("User 456 logged in from 10.0.0.1", matchingLogs.get(1).message());
    }

    @Test
    void testGetMostCommonPatterns() throws IOException {
        when(luceneService.search(eq("*"), eq(false), any(), any()))
                .thenReturn(testLogEntries);
        
        Map<String, Integer> commonPatterns = patternRecognitionService.getMostCommonPatterns(null, null, 2);
        
        assertEquals(2, commonPatterns.size());
        assertTrue(commonPatterns.containsKey("User {{NUMBER}} logged in from {{IP_ADDRESS}}"));
        assertTrue(commonPatterns.containsKey("Database query took {{NUMBER}} ms"));
        assertEquals(2, commonPatterns.get("User {{NUMBER}} logged in from {{IP_ADDRESS}}"));
        assertEquals(2, commonPatterns.get("Database query took {{NUMBER}} ms"));
    }

    @Test
    void testPatternCache() {
        String message = "User 123 logged in from 192.168.1.1";
        
        // First call should add to cache
        patternRecognitionService.recognizePattern(message);
        
        // Get cache size
        int cacheSize = patternRecognitionService.getPatternCacheSize();
        assertEquals(1, cacheSize);
        
        // Second call should use cache
        LogPatternRecognitionService.LogPattern pattern = patternRecognitionService.recognizePattern(message);
        assertEquals("User {{NUMBER}} logged in from {{IP_ADDRESS}}", pattern.template());
        
        // Clear cache
        patternRecognitionService.clearPatternCache();
        assertEquals(0, patternRecognitionService.getPatternCacheSize());
    }

    @Test
    void testHandleIOException_FindLogsByPattern() throws IOException {
        when(luceneService.search(any(), anyBoolean(), any(), any()))
                .thenThrow(new IOException("Test exception"));
        
        List<LogEntry> result = patternRecognitionService.findLogsByPattern("test pattern", null, null);
        
        assertTrue(result.isEmpty());
    }

    @Test
    void testHandleIOException_GetMostCommonPatterns() throws IOException {
        when(luceneService.search(any(), anyBoolean(), any(), any()))
                .thenThrow(new IOException("Test exception"));
        
        Map<String, Integer> result = patternRecognitionService.getMostCommonPatterns(null, null, 10);
        
        assertTrue(result.isEmpty());
    }

    private List<LogEntry> createTestLogEntries() {
        List<LogEntry> entries = new ArrayList<>();
        
        // Create log entries with different patterns
        entries.add(createLogEntry("User 123 logged in from 192.168.1.1", "INFO", "auth.log"));
        entries.add(createLogEntry("User 456 logged in from 10.0.0.1", "INFO", "auth.log"));
        entries.add(createLogEntry("Failed login attempt from 10.1.1.1 for user 789", "WARN", "auth.log"));
        entries.add(createLogEntry("Database query took 150 ms", "INFO", "app.log"));
        entries.add(createLogEntry("Database query took 200 ms", "INFO", "app.log"));
        
        return entries;
    }

    private LogEntry createLogEntry(String message, String level, String source) {
        return new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                level,
                message,
                source,
                new HashMap<>(),
                message
        );
    }
}