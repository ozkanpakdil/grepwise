package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogDirectoryConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Execution(ExecutionMode.SAME_THREAD)
public class ApacheLogIntegrationTest {

    @Mock(lenient = true)
    private LogDirectoryConfigRepository configRepository;

    @Mock(lenient = true)
    private LuceneService luceneService;

    @Mock(lenient = true)
    private LogBufferService logBufferService;

    private NginxLogParser nginxLogParser;
    private ApacheLogParser apacheLogParser;
    private LogScannerService logScannerService;
    private Path tempDir;
    private File apacheAccessLogFile;
    private File apacheErrorLogFile;

    @Captor
    private ArgumentCaptor<LogEntry> logEntryCaptor;

    @BeforeEach
    void setUp() throws IOException {
        // Create real parsers
        nginxLogParser = new NginxLogParser();
        apacheLogParser = new ApacheLogParser();

        // Create LogScannerService with mocked dependencies
        logScannerService = new LogScannerService(configRepository, luceneService, logBufferService, nginxLogParser, apacheLogParser,
                new LogPatternRecognitionService(), new RealTimeUpdateService());

        // Create temporary directory for test log files
        tempDir = Files.createTempDirectory("apache-test-logs");
        
        // Create test apache access log file
        apacheAccessLogFile = new File(tempDir.toFile(), "apache-access.log");
        try (FileWriter writer = new FileWriter(apacheAccessLogFile)) {
            // Write sample apache access logs in combined format
            writer.write("127.0.0.1 - john [10/Oct/2023:13:55:36 -0700] \"GET /api/users HTTP/1.1\" 200 2326 \"http://example.com/page\" \"Mozilla/5.0\"\n");
            writer.write("127.0.0.1 - jane [10/Oct/2023:13:56:12 -0700] \"POST /api/login HTTP/1.1\" 401 1234 \"http://example.com/login\" \"Mozilla/5.0\"\n");
            writer.write("127.0.0.1 - bob [10/Oct/2023:13:57:45 -0700] \"GET /api/products HTTP/1.1\" 404 567 \"http://example.com/shop\" \"Mozilla/5.0\"\n");
        }

        // Create test apache error log file
        apacheErrorLogFile = new File(tempDir.toFile(), "apache-error.log");
        try (FileWriter writer = new FileWriter(apacheErrorLogFile)) {
            // Write sample apache error logs
            writer.write("[Wed Oct 11 14:32:52 2023] [error] [pid 12345] [client 127.0.0.1] File does not exist: /path/to/file\n");
            writer.write("[Wed Oct 11 14:33:42 2023] [warn] [pid 12345] [client 127.0.0.1] Rewrite or internal redirection cycle detected\n");
        }

        // Configure mock repository to return our test directory
        LogDirectoryConfig config = new LogDirectoryConfig(
                UUID.randomUUID().toString(),
                tempDir.toString(),
                true,
                "*.log",
                System.currentTimeMillis()
        );
        when(configRepository.findAll()).thenReturn(List.of(config));
        when(configRepository.findById(any())).thenReturn(config);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up temporary files
        Files.deleteIfExists(apacheAccessLogFile.toPath());
        Files.deleteIfExists(apacheErrorLogFile.toPath());
        Files.deleteIfExists(tempDir);
    }

    @Test
    void testApacheAccessLogParsing() throws IOException {
        // Configure LogScannerService to use direct indexing (no buffer)
        // This makes testing easier as we can capture the log entries directly
        ReflectionTestUtils.setField(logScannerService, "useBuffer", false);

        // When indexing is called, just return the number of entries
        when(luceneService.indexLogEntries(any())).thenReturn(3);

        // Scan the directory
        int processed = logScannerService.scanDirectory(configRepository.findById("any-id"));

        // Verify that logs were processed
        assertEquals(6, processed, "Should process 6 log entries (3 from access log and 3 from error log)");

        // Capture the log entries that were passed to the luceneService
        verify(luceneService, times(2)).indexLogEntries(any());
    }

    @Test
    void testApacheErrorLogParsing() throws IOException {
        // Configure LogScannerService to use buffering
        ReflectionTestUtils.setField(logScannerService, "useBuffer", true);

        // Scan the directory
        int processed = logScannerService.scanDirectory(configRepository.findById("any-id"));

        // Verify that logs were processed
        assertTrue(processed > 0, "Should process log entries");

        // Verify that log entries were added to the buffer
        verify(logBufferService, atLeastOnce()).addToBuffer(logEntryCaptor.capture());

        // Get all captured log entries
        List<LogEntry> capturedEntries = logEntryCaptor.getAllValues();

        // Verify that at least one entry has apache_error format
        boolean hasApacheErrorFormat = capturedEntries.stream()
                .anyMatch(entry -> entry.metadata().containsKey("log_format") && 
                        entry.metadata().get("log_format").equals("apache_error"));

        assertTrue(hasApacheErrorFormat, "Should have at least one apache_error format log entry");
    }

    @Test
    void testManualScanAllDirectories() throws IOException {
        // Configure LogScannerService to use direct indexing (no buffer)
        ReflectionTestUtils.setField(logScannerService, "useBuffer", false);

        // When indexing is called, just return the number of entries
        when(luceneService.indexLogEntries(any())).thenReturn(5);
        when(configRepository.count()).thenReturn(1);

        // Manually trigger scan
        int scanned = logScannerService.manualScanAllDirectories();

        // Verify that directories were scanned
        assertEquals(1, scanned, "Should scan 1 directory");

        // Verify that logs were processed
        verify(luceneService, atLeastOnce()).indexLogEntries(any());
    }
}