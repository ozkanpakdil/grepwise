package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
public class NginxLogParserTest {

    @InjectMocks
    private NginxLogParser nginxLogParser;

    @BeforeEach
    void setUp() {
        // No additional setup needed as we're using @InjectMocks
    }

    @Test
    void testIsNginxLogFormat_CombinedFormat() {
        // Combined log format
        String combinedLog = "192.168.1.1 - john [10/Oct/2023:13:55:36 +0000] \"GET /api/users HTTP/1.1\" 200 2326 \"http://example.com/page\" \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\"";
        assertTrue(nginxLogParser.isNginxLogFormat(combinedLog), "Should recognize combined log format");
    }

    @Test
    void testIsNginxLogFormat_CommonFormat() {
        // Common log format
        String commonLog = "192.168.1.1 - john [10/Oct/2023:13:55:36 +0000] \"GET /api/users HTTP/1.1\" 200 2326";
        assertTrue(nginxLogParser.isNginxLogFormat(commonLog), "Should recognize common log format");
    }

    @Test
    void testIsNginxLogFormat_ErrorFormat() {
        // Error log format
        String errorLog = "2023/10/10 13:55:36 [error] 12345#0: *67890 open() failed: No such file or directory, client: 192.168.1.1, server: example.com, request: \"GET /missing.html HTTP/1.1\"";
        assertTrue(nginxLogParser.isNginxLogFormat(errorLog), "Should recognize error log format");
    }

    @Test
    void testIsNginxLogFormat_NonNginxFormat() {
        // Non-Nginx log format
        String nonNginxLog = "2023-10-10 13:55:36 INFO [main] Application started successfully";
        assertFalse(nginxLogParser.isNginxLogFormat(nonNginxLog), "Should not recognize non-Nginx log format");
    }

    @Test
    void testParseCombinedLogFormat() {
        // Combined log format
        String combinedLog = "192.168.1.1 - john [10/Oct/2023:13:55:36 +0000] \"GET /api/users HTTP/1.1\" 200 2326 \"http://example.com/page\" \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\"";
        String source = "nginx-access.log";

        LogEntry logEntry = nginxLogParser.parseNginxLogLine(combinedLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("INFO", logEntry.level(), "Log level should be INFO for 200 status code");
        assertEquals(source, logEntry.source(), "Source should match the provided source");
        assertEquals(combinedLog, logEntry.rawContent(), "Raw content should match the original log line");

        Map<String, String> metadata = logEntry.metadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("192.168.1.1", metadata.get("ip_address"), "IP address should be extracted correctly");
        assertEquals("john", metadata.get("user_id"), "User ID should be extracted correctly");
        assertEquals("GET", metadata.get("method"), "HTTP method should be extracted correctly");
        assertEquals("/api/users", metadata.get("path"), "Path should be extracted correctly");
        assertEquals("HTTP/1.1", metadata.get("protocol"), "Protocol should be extracted correctly");
        assertEquals("200", metadata.get("status_code"), "Status code should be extracted correctly");
        assertEquals("2326", metadata.get("bytes"), "Bytes should be extracted correctly");
        assertEquals("http://example.com/page", metadata.get("referer"), "Referer should be extracted correctly");
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36", metadata.get("user_agent"), "User agent should be extracted correctly");
        assertEquals("nginx_combined", metadata.get("log_format"), "Log format should be identified as nginx_combined");
    }

    @Test
    void testParseCommonLogFormat() {
        // Common log format
        String commonLog = "192.168.1.1 - john [10/Oct/2023:13:55:36 +0000] \"GET /api/users HTTP/1.1\" 200 2326";
        String source = "nginx-access.log";

        LogEntry logEntry = nginxLogParser.parseNginxLogLine(commonLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("INFO", logEntry.level(), "Log level should be INFO for 200 status code");
        assertEquals(source, logEntry.source(), "Source should match the provided source");
        assertEquals(commonLog, logEntry.rawContent(), "Raw content should match the original log line");

        Map<String, String> metadata = logEntry.metadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("192.168.1.1", metadata.get("ip_address"), "IP address should be extracted correctly");
        assertEquals("john", metadata.get("user_id"), "User ID should be extracted correctly");
        assertEquals("GET", metadata.get("method"), "HTTP method should be extracted correctly");
        assertEquals("/api/users", metadata.get("path"), "Path should be extracted correctly");
        assertEquals("HTTP/1.1", metadata.get("protocol"), "Protocol should be extracted correctly");
        assertEquals("200", metadata.get("status_code"), "Status code should be extracted correctly");
        assertEquals("2326", metadata.get("bytes"), "Bytes should be extracted correctly");
        assertEquals("nginx_common", metadata.get("log_format"), "Log format should be identified as nginx_common");
    }

    @Test
    void testParseErrorLogFormat() {
        // Error log format
        String errorLog = "2023/10/10 13:55:36 [error] 12345#0: *67890 open() failed: No such file or directory, client: 192.168.1.1, server: example.com, request: \"GET /missing.html HTTP/1.1\"";
        String source = "nginx-error.log";

        LogEntry logEntry = nginxLogParser.parseNginxLogLine(errorLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("ERROR", logEntry.level(), "Log level should be ERROR for error log");
        assertEquals(source, logEntry.source(), "Source should match the provided source");
        assertEquals(errorLog, logEntry.rawContent(), "Raw content should match the original log line");

        Map<String, String> metadata = logEntry.metadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("2023/10/10 13:55:36", metadata.get("timestamp"), "Timestamp should be extracted correctly");
        assertEquals("error", metadata.get("log_level"), "Log level should be extracted correctly");
        assertEquals("12345", metadata.get("process_id"), "Process ID should be extracted correctly");
        assertEquals("open() failed: No such file or directory", metadata.get("error_message"), "Error message should be extracted correctly");
        assertEquals("192.168.1.1", metadata.get("client_ip"), "Client IP should be extracted correctly");
        assertEquals("example.com", metadata.get("server"), "Server should be extracted correctly");
        assertEquals("GET", metadata.get("method"), "HTTP method should be extracted correctly");
        assertEquals("/missing.html", metadata.get("path"), "Path should be extracted correctly");
        assertEquals("HTTP/1.1", metadata.get("protocol"), "Protocol should be extracted correctly");
        assertEquals("nginx_error", metadata.get("log_format"), "Log format should be identified as nginx_error");
    }

    @Test
    void testParseNginxLogLine_NonNginxFormat() {
        // Non-Nginx log format
        String nonNginxLog = "2023-10-10 13:55:36 INFO [main] Application started successfully";
        String source = "application.log";

        LogEntry logEntry = nginxLogParser.parseNginxLogLine(nonNginxLog, source);

        assertNull(logEntry, "LogEntry should be null for non-Nginx log format");
    }

    @Test
    void testParseNginxLogLine_ErrorStatusCode() {
        // Combined log format with error status code
        String errorLog = "192.168.1.1 - john [10/Oct/2023:13:55:36 +0000] \"GET /api/users HTTP/1.1\" 500 1234 \"http://example.com/page\" \"Mozilla/5.0\"";
        String source = "nginx-access.log";

        LogEntry logEntry = nginxLogParser.parseNginxLogLine(errorLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("ERROR", logEntry.level(), "Log level should be ERROR for 500 status code");
        assertEquals("500", logEntry.metadata().get("status_code"), "Status code should be extracted correctly");
    }

    @Test
    void testParseNginxLogLine_WarningStatusCode() {
        // Combined log format with warning status code
        String warningLog = "192.168.1.1 - john [10/Oct/2023:13:55:36 +0000] \"GET /api/users HTTP/1.1\" 404 1234 \"http://example.com/page\" \"Mozilla/5.0\"";
        String source = "nginx-access.log";

        LogEntry logEntry = nginxLogParser.parseNginxLogLine(warningLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("WARN", logEntry.level(), "Log level should be WARN for 404 status code");
        assertEquals("404", logEntry.metadata().get("status_code"), "Status code should be extracted correctly");
    }
}