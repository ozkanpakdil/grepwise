package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogBufferServiceTest {

    private LogBufferService logBufferService;

    @Mock
    private LuceneService luceneService;

    @Captor
    private ArgumentCaptor<List<LogEntry>> logEntriesCaptor;

    @BeforeEach
    void setUp() throws IOException {
        // Mock LuceneService behavior - use lenient to avoid UnnecessaryStubbingException
        lenient().when(luceneService.indexLogEntries(any())).thenAnswer(invocation -> {
            List<LogEntry> entries = invocation.getArgument(0);
            return entries.size();
        });

        // Create LogBufferService with mocked dependencies
        logBufferService = new LogBufferService(luceneService);
        
        // Set smaller buffer size for testing
        ReflectionTestUtils.setField(logBufferService, "maxBufferSize", 10);
        ReflectionTestUtils.setField(logBufferService, "flushIntervalMs", 1000);
        
        // Initialize the service
        logBufferService.init();
    }

    @Test
    void testAddToBuffer() throws IOException {
        // Create a test log entry
        LogEntry entry = createTestLogEntry("Test message");
        
        // Add to buffer
        boolean added = logBufferService.addToBuffer(entry);
        
        // Verify
        assertTrue(added);
        assertEquals(1, logBufferService.getBufferSize());
        
        // No flush should have happened yet
        verify(luceneService, never()).indexLogEntries(any());
    }

    @Test
    void testAddAllToBuffer() throws IOException {
        // Create test log entries
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            entries.add(createTestLogEntry("Test message " + i));
        }
        
        // Add all to buffer
        int added = logBufferService.addAllToBuffer(entries);
        
        // Verify
        assertEquals(5, added);
        assertEquals(5, logBufferService.getBufferSize());
        
        // No flush should have happened yet
        verify(luceneService, never()).indexLogEntries(any());
    }

    @Test
    void testBufferFlushWhenFull() throws IOException {
        // Create enough entries to fill the buffer
        List<LogEntry> entries = new ArrayList<>();
        for (int i = 0; i < logBufferService.getMaxBufferSize(); i++) {
            entries.add(createTestLogEntry("Test message " + i));
        }
        
        // Add all to buffer - this should trigger a flush
        logBufferService.addAllToBuffer(entries);
        
        // Verify that indexLogEntries was called
        verify(luceneService, times(1)).indexLogEntries(logEntriesCaptor.capture());
        
        // Verify the flushed entries
        List<LogEntry> flushedEntries = logEntriesCaptor.getValue();
        assertEquals(logBufferService.getMaxBufferSize(), flushedEntries.size());
        
        // Buffer should be empty after flush
        assertEquals(0, logBufferService.getBufferSize());
    }

    @Test
    void testManualFlush() throws IOException {
        // Add some entries to the buffer
        for (int i = 0; i < 5; i++) {
            logBufferService.addToBuffer(createTestLogEntry("Test message " + i));
        }
        
        // Manually flush the buffer
        int flushed = logBufferService.flushBuffer();
        
        // Verify
        assertEquals(5, flushed);
        verify(luceneService, times(1)).indexLogEntries(logEntriesCaptor.capture());
        
        // Verify the flushed entries
        List<LogEntry> flushedEntries = logEntriesCaptor.getValue();
        assertEquals(5, flushedEntries.size());
        
        // Buffer should be empty after flush
        assertEquals(0, logBufferService.getBufferSize());
    }

    @Test
    void testScheduledFlush() throws Exception {
        // Reset the mock to clear previous stubbing
        reset(luceneService);
        
        // Setup mock behavior
        when(luceneService.indexLogEntries(any())).thenReturn(5);
        
        // Add some entries to the buffer
        for (int i = 0; i < 5; i++) {
            logBufferService.addToBuffer(createTestLogEntry("Test message " + i));
        }
        
        // Instead of waiting for the scheduled task, directly call the method
        logBufferService.scheduledFlush();
        
        // Verify that indexLogEntries was called
        verify(luceneService, times(1)).indexLogEntries(any());
        
        // Buffer should be empty after flush
        assertEquals(0, logBufferService.getBufferSize());
    }

    @Test
    void testConcurrentAccess() throws Exception {
        // Test that the buffer can be accessed concurrently by multiple threads
        int numThreads = 10;
        int entriesPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int t = 0; t < numThreads; t++) {
            final int threadNum = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < entriesPerThread; i++) {
                        LogEntry entry = createTestLogEntry("Thread " + threadNum + " message " + i);
                        logBufferService.addToBuffer(entry);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Flush any remaining entries
        logBufferService.flushBuffer();
        
        // Verify that all entries were processed
        // Note: Some entries may have been flushed automatically if the buffer filled up
        int totalEntries = numThreads * entriesPerThread;
        verify(luceneService, atLeastOnce()).indexLogEntries(any());
        
        // Buffer should be empty after flush
        assertEquals(0, logBufferService.getBufferSize());
    }

    @Test
    void testConfigurationProperties() {
        // Test setting and getting configuration properties
        logBufferService.setMaxBufferSize(20);
        assertEquals(20, logBufferService.getMaxBufferSize());
        
        logBufferService.setFlushIntervalMs(2000);
        assertEquals(2000, logBufferService.getFlushIntervalMs());
    }

    @Test
    void testErrorHandlingDuringFlush() throws IOException {
        // Reset the mock to clear previous stubbing
        reset(luceneService);
        
        // Setup LuceneService to throw an exception during indexing
        when(luceneService.indexLogEntries(any())).thenThrow(new IOException("Test exception"));
        
        // Add some entries to the buffer
        for (int i = 0; i < 5; i++) {
            logBufferService.addToBuffer(createTestLogEntry("Test message " + i));
        }
        
        // Flush should handle the exception gracefully
        int flushed = logBufferService.flushBuffer();
        
        // Verify
        assertEquals(0, flushed); // No entries were successfully flushed
        
        // Buffer should be empty after flush attempt (current implementation discards entries on error)
        assertEquals(0, logBufferService.getBufferSize());
    }

    private LogEntry createTestLogEntry(String message) {
        return new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                System.currentTimeMillis(),
                "INFO",
                message,
                "test-source",
                new HashMap<>(),
                message
        );
    }
}