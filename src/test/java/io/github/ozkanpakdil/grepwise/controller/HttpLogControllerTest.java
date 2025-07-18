package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import io.github.ozkanpakdil.grepwise.service.LogBufferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the HttpLogController.
 */
public class HttpLogControllerTest {

    private LogBufferService logBufferService;
    private HttpLogController controller;
    private LogSourceConfig httpSource;

    @BeforeEach
    public void setup() {
        // Create a mock LogBufferService
        logBufferService = Mockito.mock(LogBufferService.class);
        
        // Create the controller with the mock service
        controller = new HttpLogController(logBufferService);
        
        // Create a test HTTP source
        httpSource = LogSourceConfig.createHttpSource(
                "test-source",
                "Test HTTP Source",
                "/api/logs/test-source",
                "test-token",
                true,
                true
        );
        
        // Register the source with the controller
        controller.registerHttpSource(httpSource);
    }

    @Test
    public void testRegisterHttpSource() {
        // Create a new HTTP source
        LogSourceConfig newSource = LogSourceConfig.createHttpSource(
                "new-source",
                "New HTTP Source",
                "/api/logs/new-source",
                "new-token",
                true,
                true
        );
        
        // Register the source
        boolean result = controller.registerHttpSource(newSource);
        
        // Verify the result
        assertTrue(result);
        assertEquals(2, controller.getRegisteredSourceCount());
    }
    
    @Test
    public void testRegisterNonHttpSource() {
        // Create a non-HTTP source
        LogSourceConfig fileSource = LogSourceConfig.createFileSource(
                "file-source",
                "File Source",
                "/var/log",
                "*.log",
                60,
                true
        );
        
        // Attempt to register the source
        boolean result = controller.registerHttpSource(fileSource);
        
        // Verify the result
        assertFalse(result);
        assertEquals(1, controller.getRegisteredSourceCount());
    }
    
    @Test
    public void testUnregisterHttpSource() {
        // Unregister the source
        boolean result = controller.unregisterHttpSource("test-source");
        
        // Verify the result
        assertTrue(result);
        assertEquals(0, controller.getRegisteredSourceCount());
    }
    
    @Test
    public void testUnregisterNonExistentSource() {
        // Attempt to unregister a non-existent source
        boolean result = controller.unregisterHttpSource("non-existent");
        
        // Verify the result
        assertFalse(result);
        assertEquals(1, controller.getRegisteredSourceCount());
    }
    
    @Test
    public void testReceiveLogWithValidSource() {
        // Configure the mock to return true when addToBuffer is called
        when(logBufferService.addToBuffer(any(LogEntry.class))).thenReturn(true);
        
        // Create a log request
        HttpLogController.LogRequest logRequest = new HttpLogController.LogRequest();
        logRequest.setMessage("Test log message");
        logRequest.setLevel("INFO");
        logRequest.setTimestamp(System.currentTimeMillis());
        
        Map<String, String> metadata = new HashMap<>();
        metadata.put("key1", "value1");
        metadata.put("key2", "value2");
        logRequest.setMetadata(metadata);
        
        // Send the log request
        ResponseEntity<Map<String, Object>> response = controller.receiveLog(
                "test-source",
                "test-token",
                logRequest
        );
        
        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        
        // Verify that addToBuffer was called with the correct LogEntry
        ArgumentCaptor<LogEntry> logEntryCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logBufferService).addToBuffer(logEntryCaptor.capture());
        
        LogEntry capturedEntry = logEntryCaptor.getValue();
        assertEquals("INFO", capturedEntry.level());
        assertEquals("Test log message", capturedEntry.message());
        assertEquals("http:test-source", capturedEntry.source());
        assertEquals(logRequest.getTimestamp(), capturedEntry.recordTime());
        
        // Verify metadata
        Map<String, String> capturedMetadata = capturedEntry.metadata();
        assertEquals("value1", capturedMetadata.get("key1"));
        assertEquals("value2", capturedMetadata.get("key2"));
        assertEquals("http", capturedMetadata.get("source_type"));
        assertEquals("test-source", capturedMetadata.get("source_id"));
    }
    
    @Test
    public void testReceiveLogWithInvalidSource() {
        // Send a log request with an invalid source
        ResponseEntity<Map<String, Object>> response = controller.receiveLog(
                "invalid-source",
                "test-token",
                new HttpLogController.LogRequest()
        );
        
        // Verify the response
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        
        // Verify that addToBuffer was not called
        verify(logBufferService, never()).addToBuffer(any(LogEntry.class));
    }
    
    @Test
    public void testReceiveLogWithInvalidToken() {
        // Send a log request with an invalid token
        ResponseEntity<Map<String, Object>> response = controller.receiveLog(
                "test-source",
                "invalid-token",
                new HttpLogController.LogRequest()
        );
        
        // Verify the response
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        
        // Verify that addToBuffer was not called
        verify(logBufferService, never()).addToBuffer(any(LogEntry.class));
    }
    
    @Test
    public void testReceiveLogWithDisabledSource() {
        // Disable the source
        httpSource.setEnabled(false);
        
        // Send a log request
        ResponseEntity<Map<String, Object>> response = controller.receiveLog(
                "test-source",
                "test-token",
                new HttpLogController.LogRequest()
        );
        
        // Verify the response
        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertFalse((Boolean) response.getBody().get("success"));
        
        // Verify that addToBuffer was not called
        verify(logBufferService, never()).addToBuffer(any(LogEntry.class));
    }
    
    @Test
    public void testReceiveLogBatchWithValidSource() {
        // Configure the mock to return the number of entries when addAllToBuffer is called
        when(logBufferService.addAllToBuffer(anyList())).thenReturn(2);
        
        // Create log requests
        List<HttpLogController.LogRequest> logRequests = new ArrayList<>();
        
        HttpLogController.LogRequest request1 = new HttpLogController.LogRequest();
        request1.setMessage("Test log message 1");
        request1.setLevel("INFO");
        logRequests.add(request1);
        
        HttpLogController.LogRequest request2 = new HttpLogController.LogRequest();
        request2.setMessage("Test log message 2");
        request2.setLevel("ERROR");
        logRequests.add(request2);
        
        // Send the log requests
        ResponseEntity<Map<String, Object>> response = controller.receiveLogBatch(
                "test-source",
                "test-token",
                logRequests
        );
        
        // Verify the response
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue((Boolean) response.getBody().get("success"));
        assertEquals(2, response.getBody().get("count"));
        
        // Verify that addAllToBuffer was called with the correct list of LogEntry objects
        ArgumentCaptor<List<LogEntry>> logEntriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(logBufferService).addAllToBuffer(logEntriesCaptor.capture());
        
        List<LogEntry> capturedEntries = logEntriesCaptor.getValue();
        assertEquals(2, capturedEntries.size());
        
        // Verify the first entry
        LogEntry entry1 = capturedEntries.get(0);
        assertEquals("INFO", entry1.level());
        assertEquals("Test log message 1", entry1.message());
        assertEquals("http:test-source", entry1.source());
        
        // Verify the second entry
        LogEntry entry2 = capturedEntries.get(1);
        assertEquals("ERROR", entry2.level());
        assertEquals("Test log message 2", entry2.message());
        assertEquals("http:test-source", entry2.source());
    }
}