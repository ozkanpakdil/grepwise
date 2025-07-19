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
public class ApacheLogParserTest {

    @InjectMocks
    private ApacheLogParser apacheLogParser;

    @BeforeEach
    void setUp() {
        // No additional setup needed as we're using @InjectMocks
    }

    @Test
    void testIsApacheLogFormat_CombinedFormat() {
        // Combined log format
        String combinedLog = "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 2326 \"http://www.example.com/start.html\" \"Mozilla/4.08 [en] (Win98; I ;Nav)\"";
        assertTrue(apacheLogParser.isApacheLogFormat(combinedLog), "Should recognize combined log format");
    }

    @Test
    void testIsApacheLogFormat_CommonFormat() {
        // Common log format
        String commonLog = "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 2326";
        assertTrue(apacheLogParser.isApacheLogFormat(commonLog), "Should recognize common log format");
    }

    @Test
    void testIsApacheLogFormat_ErrorFormat() {
        // Error log format
        String errorLog = "[Wed Oct 11 14:32:52 2000] [error] [pid 12345] [client 127.0.0.1] File does not exist: /path/to/file";
        assertTrue(apacheLogParser.isApacheLogFormat(errorLog), "Should recognize error log format");
    }

    @Test
    void testIsApacheLogFormat_NonApacheFormat() {
        // Non-Apache log format
        String nonApacheLog = "2023-10-10 13:55:36 INFO [main] Application started successfully";
        assertFalse(apacheLogParser.isApacheLogFormat(nonApacheLog), "Should not recognize non-Apache log format");
    }

    @Test
    void testParseCombinedLogFormat() {
        // Combined log format
        String combinedLog = "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 2326 \"http://www.example.com/start.html\" \"Mozilla/4.08 [en] (Win98; I ;Nav)\"";
        String source = "apache-access.log";

        LogEntry logEntry = apacheLogParser.parseApacheLogLine(combinedLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("INFO", logEntry.level(), "Log level should be INFO for 200 status code");
        assertEquals(source, logEntry.source(), "Source should match the provided source");
        assertEquals(combinedLog, logEntry.rawContent(), "Raw content should match the original log line");

        Map<String, String> metadata = logEntry.metadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("127.0.0.1", metadata.get("ip_address"), "IP address should be extracted correctly");
        assertEquals("frank", metadata.get("user_id"), "User ID should be extracted correctly");
        assertEquals("GET", metadata.get("method"), "HTTP method should be extracted correctly");
        assertEquals("/apache_pb.gif", metadata.get("path"), "Path should be extracted correctly");
        assertEquals("HTTP/1.0", metadata.get("protocol"), "Protocol should be extracted correctly");
        assertEquals("200", metadata.get("status_code"), "Status code should be extracted correctly");
        assertEquals("2326", metadata.get("bytes"), "Bytes should be extracted correctly");
        assertEquals("http://www.example.com/start.html", metadata.get("referer"), "Referer should be extracted correctly");
        assertEquals("Mozilla/4.08 [en] (Win98; I ;Nav)", metadata.get("user_agent"), "User agent should be extracted correctly");
        assertEquals("apache_combined", metadata.get("log_format"), "Log format should be identified as apache_combined");
    }

    @Test
    void testParseCommonLogFormat() {
        // Common log format
        String commonLog = "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 2326";
        String source = "apache-access.log";

        LogEntry logEntry = apacheLogParser.parseApacheLogLine(commonLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("INFO", logEntry.level(), "Log level should be INFO for 200 status code");
        assertEquals(source, logEntry.source(), "Source should match the provided source");
        assertEquals(commonLog, logEntry.rawContent(), "Raw content should match the original log line");

        Map<String, String> metadata = logEntry.metadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("127.0.0.1", metadata.get("ip_address"), "IP address should be extracted correctly");
        assertEquals("frank", metadata.get("user_id"), "User ID should be extracted correctly");
        assertEquals("GET", metadata.get("method"), "HTTP method should be extracted correctly");
        assertEquals("/apache_pb.gif", metadata.get("path"), "Path should be extracted correctly");
        assertEquals("HTTP/1.0", metadata.get("protocol"), "Protocol should be extracted correctly");
        assertEquals("200", metadata.get("status_code"), "Status code should be extracted correctly");
        assertEquals("2326", metadata.get("bytes"), "Bytes should be extracted correctly");
        assertEquals("apache_common", metadata.get("log_format"), "Log format should be identified as apache_common");
    }

    @Test
    void testParseErrorLogFormat() {
        // Error log format
        String errorLog = "[Wed Oct 11 14:32:52 2000] [error] [pid 12345] [client 127.0.0.1] File does not exist: /path/to/file";
        String source = "apache-error.log";

        LogEntry logEntry = apacheLogParser.parseApacheLogLine(errorLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("ERROR", logEntry.level(), "Log level should be ERROR for error log");
        assertEquals(source, logEntry.source(), "Source should match the provided source");
        assertEquals(errorLog, logEntry.rawContent(), "Raw content should match the original log line");

        Map<String, String> metadata = logEntry.metadata();
        assertNotNull(metadata, "Metadata should not be null");
        assertEquals("Wed Oct 11 14:32:52 2000", metadata.get("timestamp"), "Timestamp should be extracted correctly");
        assertEquals("error", metadata.get("log_level"), "Log level should be extracted correctly");
        assertEquals("12345", metadata.get("process_id"), "Process ID should be extracted correctly");
        assertEquals("File does not exist: /path/to/file", metadata.get("error_message"), "Error message should be extracted correctly");
        assertEquals("127.0.0.1", metadata.get("client_ip"), "Client IP should be extracted correctly");
        assertEquals("apache_error", metadata.get("log_format"), "Log format should be identified as apache_error");
    }

    @Test
    void testParseApacheLogLine_NonApacheFormat() {
        // Non-Apache log format
        String nonApacheLog = "2023-10-10 13:55:36 INFO [main] Application started successfully";
        String source = "application.log";

        LogEntry logEntry = apacheLogParser.parseApacheLogLine(nonApacheLog, source);

        assertNull(logEntry, "LogEntry should be null for non-Apache log format");
    }

    @Test
    void testParseApacheLogLine_ErrorStatusCode() {
        // Combined log format with error status code
        String errorLog = "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 500 1234 \"http://www.example.com/start.html\" \"Mozilla/4.08 [en] (Win98; I ;Nav)\"";
        String source = "apache-access.log";

        LogEntry logEntry = apacheLogParser.parseApacheLogLine(errorLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("ERROR", logEntry.level(), "Log level should be ERROR for 500 status code");
        assertEquals("500", logEntry.metadata().get("status_code"), "Status code should be extracted correctly");
    }

    @Test
    void testParseApacheLogLine_WarningStatusCode() {
        // Combined log format with warning status code
        String warningLog = "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 404 1234 \"http://www.example.com/start.html\" \"Mozilla/4.08 [en] (Win98; I ;Nav)\"";
        String source = "apache-access.log";

        LogEntry logEntry = apacheLogParser.parseApacheLogLine(warningLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("WARN", logEntry.level(), "Log level should be WARN for 404 status code");
        assertEquals("404", logEntry.metadata().get("status_code"), "Status code should be extracted correctly");
    }

    @Test
    void testParseCommonLogFormat_WithDashForBytes() {
        // Common log format with dash for bytes
        String commonLog = "127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] \"GET /apache_pb.gif HTTP/1.0\" 200 -";
        String source = "apache-access.log";

        LogEntry logEntry = apacheLogParser.parseApacheLogLine(commonLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("0", logEntry.metadata().get("bytes"), "Bytes should be '0' when '-' is in the log");
    }

    @Test
    void testParseErrorLogFormat_WithoutPidAndClient() {
        // Error log format without pid and client
        String errorLog = "[Wed Oct 11 14:32:52 2000] [error] File does not exist: /path/to/file";
        String source = "apache-error.log";

        LogEntry logEntry = apacheLogParser.parseApacheLogLine(errorLog, source);

        assertNotNull(logEntry, "LogEntry should not be null");
        assertEquals("ERROR", logEntry.level(), "Log level should be ERROR for error log");
        assertEquals("File does not exist: /path/to/file", logEntry.metadata().get("error_message"), "Error message should be extracted correctly");
        assertNull(logEntry.metadata().get("process_id"), "Process ID should be null");
        assertNull(logEntry.metadata().get("client_ip"), "Client IP should be null");
    }
}