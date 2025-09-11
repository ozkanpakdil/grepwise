package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.repository.LogDirectoryConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class LogScannerBufferIntegrationTest {

    private LogScannerService logScannerService;
    private LogBufferService logBufferService;

    @Mock
    private LogDirectoryConfigRepository configRepository;

    @Mock
    private LuceneService luceneService;
    
    @Mock
    private NginxLogParser nginxLogParser;
    
    @Mock
    private ApacheLogParser apacheLogParser;
    
    @Mock
    private LogPatternRecognitionService patternRecognitionService;
    
    @Mock
    private RealTimeUpdateService realTimeUpdateService;

    @Captor
    private ArgumentCaptor<List<Object>> logEntriesCaptor;

    @TempDir
    Path tempDir;

    private File logFile;
    private LogDirectoryConfig config;

    @BeforeEach
    void setUp() throws IOException {
        // Initialize LogBufferService with LuceneService
        logBufferService = spy(new LogBufferService(luceneService));
        
        // Initialize LogScannerService with all dependencies
        logScannerService = new LogScannerService(
            configRepository,
            luceneService,
            logBufferService,
            nginxLogParser,
            apacheLogParser,
            patternRecognitionService,
            realTimeUpdateService
        );
        
        // Create a temporary log file
        logFile = new File(tempDir.toFile(), "test.log");
        try (FileWriter writer = new FileWriter(logFile)) {
            // Write 100 log lines
            for (int i = 0; i < 100; i++) {
                writer.write("2023-07-18 21:28:00 INFO Test log message " + i + "\n");
            }
        }

        // Create a directory config pointing to the temp directory
        config = new LogDirectoryConfig();
        config.setId(UUID.randomUUID().toString());
        config.setDirectoryPath(tempDir.toString());
        config.setEnabled(true);

        // Mock the repository to return our config
        when(configRepository.findAll()).thenReturn(List.of(config));
        when(configRepository.findById(config.getId())).thenReturn(config);
        when(configRepository.count()).thenReturn(1);
        
        // Mock LuceneService to return the number of entries indexed
        when(luceneService.indexLogEntries(any())).thenAnswer(invocation -> {
            List<?> entries = invocation.getArgument(0);
            return entries.size();
        });
        
        // Set a smaller buffer size for testing
        ReflectionTestUtils.setField(logBufferService, "maxBufferSize", 20);
        
        // Ensure buffering is enabled
        ReflectionTestUtils.setField(logScannerService, "useBuffer", true);
    }

    @AfterEach
    void tearDown() {
        // Clean up
        if (logFile != null && logFile.exists()) {
            logFile.delete();
        }
    }

    @Test
    void testLogScannerUsesBuffer() throws IOException {
        // Scan the directory
        int processed = logScannerService.scanDirectory(config);
        
        // Verify that all log entries were processed
        assertEquals(100, processed);
        
        // Verify that the buffer was used
        verify(logBufferService, atLeast(100)).addToBuffer(any());
        
        // Verify that the buffer was flushed at least once
        // (with our buffer size of 20, it should be flushed 5 times for 100 entries)
        verify(logBufferService, atLeast(5)).flushBuffer();
        
        // Verify that LuceneService was called to index the entries
        verify(luceneService, atLeast(1)).indexLogEntries(any());
    }

    @Test
    void testDirectIndexingWhenBufferingDisabled() throws IOException {
        // Disable buffering
        ReflectionTestUtils.setField(logScannerService, "useBuffer", false);
        
        // Scan the directory
        int processed = logScannerService.scanDirectory(config);
        
        // Verify that all log entries were processed
        assertEquals(100, processed);
        
        // Verify that the buffer was NOT used
        verify(logBufferService, never()).addToBuffer(any());
        
        // Verify that LuceneService was called directly to index the entries
        verify(luceneService, times(1)).indexLogEntries(any());
    }

}