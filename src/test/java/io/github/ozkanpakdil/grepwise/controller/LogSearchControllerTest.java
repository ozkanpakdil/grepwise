package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogRepository;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.github.ozkanpakdil.grepwise.service.SearchCacheService;
import io.github.ozkanpakdil.grepwise.service.SplQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the LogSearchController.
 */
public class LogSearchControllerTest {

    private LogRepository logRepository;
    private LuceneService luceneService;
    private SplQueryService splQueryService;
    private SearchCacheService searchCacheService;
    private LogSearchController controller;

    @BeforeEach
    public void setup() {
        // Create mock dependencies
        logRepository = Mockito.mock(LogRepository.class);
        luceneService = Mockito.mock(LuceneService.class);
        splQueryService = Mockito.mock(SplQueryService.class);
        searchCacheService = Mockito.mock(SearchCacheService.class);
        
        // Create the controller with the mock dependencies
        controller = new LogSearchController(logRepository, luceneService, splQueryService, searchCacheService);
    }

    @Test
    public void testExportLogsAsCsv() throws IOException {
        // Create test log entries
        List<LogEntry> testLogs = createTestLogs();
        
        // Configure the mock to return the test logs
        when(luceneService.search(anyString(), anyBoolean(), anyLong(), anyLong()))
            .thenReturn(testLogs);
        
        // Call the export method
        ResponseEntity<String> response = controller.exportLogsAsCsv(
            "test query", true, "24h", null, null);
        
        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Verify the content type
        HttpHeaders headers = response.getHeaders();
        assertEquals(MediaType.parseMediaType("text/csv"), headers.getContentType());
        
        // Verify that the Content-Disposition header is set for file download
        assertTrue(headers.getContentDisposition().isAttachment());
        assertTrue(headers.getContentDisposition().getFilename().startsWith("logs_export_"));
        assertTrue(headers.getContentDisposition().getFilename().endsWith(".csv"));
        
        // Verify the CSV content
        String csvContent = response.getBody();
        assertNotNull(csvContent);
        
        // Check for CSV header
        assertTrue(csvContent.startsWith("ID,Timestamp,DateTime,Level,Source,Message,RawContent"));
        
        // Check for log entries
        assertTrue(csvContent.contains("test-id-1"));
        assertTrue(csvContent.contains("INFO"));
        assertTrue(csvContent.contains("Test message 1"));
        assertTrue(csvContent.contains("test-source-1"));
        
        assertTrue(csvContent.contains("test-id-2"));
        assertTrue(csvContent.contains("ERROR"));
        assertTrue(csvContent.contains("Test message 2"));
        assertTrue(csvContent.contains("test-source-2"));
        
        // Verify that the search method was called
        verify(luceneService).search(eq("test query"), eq(true), anyLong(), anyLong());
    }

    @Test
    public void testExportLogsAsJson() throws IOException {
        // Create test log entries
        List<LogEntry> testLogs = createTestLogs();
        
        // Configure the mock to return the test logs
        when(luceneService.search(anyString(), anyBoolean(), anyLong(), anyLong()))
            .thenReturn(testLogs);
        
        // Call the export method
        ResponseEntity<List<LogEntry>> response = controller.exportLogsAsJson(
            "test query", true, "24h", null, null);
        
        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        
        // Verify the content type
        HttpHeaders headers = response.getHeaders();
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        
        // Verify that the Content-Disposition header is set for file download
        assertTrue(headers.getContentDisposition().isAttachment());
        assertTrue(headers.getContentDisposition().getFilename().startsWith("logs_export_"));
        assertTrue(headers.getContentDisposition().getFilename().endsWith(".json"));
        
        // Verify the JSON content
        List<LogEntry> jsonContent = response.getBody();
        assertNotNull(jsonContent);
        assertEquals(2, jsonContent.size());
        
        // Check the first log entry
        LogEntry log1 = jsonContent.get(0);
        assertEquals("test-id-1", log1.id());
        assertEquals("INFO", log1.level());
        assertEquals("Test message 1", log1.message());
        assertEquals("test-source-1", log1.source());
        
        // Check the second log entry
        LogEntry log2 = jsonContent.get(1);
        assertEquals("test-id-2", log2.id());
        assertEquals("ERROR", log2.level());
        assertEquals("Test message 2", log2.message());
        assertEquals("test-source-2", log2.source());
        
        // Verify that the search method was called
        verify(luceneService).search(eq("test query"), eq(true), anyLong(), anyLong());
    }

    @Test
    public void testExportLogsAsCsvWithError() throws IOException {
        // Configure the mock to throw an exception
        when(luceneService.search(anyString(), anyBoolean(), anyLong(), anyLong()))
            .thenThrow(new IOException("Test exception"));
        
        // Call the export method
        ResponseEntity<String> response = controller.exportLogsAsCsv(
            "test query", true, "24h", null, null);
        
        // Verify the response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    @Test
    public void testExportLogsAsJsonWithError() throws IOException {
        // Configure the mock to throw an exception
        when(luceneService.search(anyString(), anyBoolean(), anyLong(), anyLong()))
            .thenThrow(new IOException("Test exception"));
        
        // Call the export method
        ResponseEntity<List<LogEntry>> response = controller.exportLogsAsJson(
            "test query", true, "24h", null, null);
        
        // Verify the response
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNull(response.getBody());
    }

    /**
     * Helper method to create test log entries.
     */
    private List<LogEntry> createTestLogs() {
        List<LogEntry> logs = new ArrayList<>();
        
        // Create the first log entry
        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("key1", "value1");
        logs.add(new LogEntry(
            "test-id-1",
            System.currentTimeMillis(),
            null,
            "INFO",
            "Test message 1",
            "test-source-1",
            metadata1,
            "Raw content 1"
        ));
        
        // Create the second log entry
        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put("key2", "value2");
        logs.add(new LogEntry(
            "test-id-2",
            System.currentTimeMillis(),
            null,
            "ERROR",
            "Test message 2",
            "test-source-2",
            metadata2,
            "Raw content 2"
        ));
        
        return logs;
    }
}