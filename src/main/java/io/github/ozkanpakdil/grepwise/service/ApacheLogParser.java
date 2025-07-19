package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing Apache HTTP Server log formats.
 * Supports common Apache log formats including:
 * - Common Log Format (CLF)
 * - Combined Log Format
 * - Error Log Format
 */
@Service
public class ApacheLogParser {
    private static final Logger logger = LoggerFactory.getLogger(ApacheLogParser.class);

    // Regex pattern for Apache common log format
    // Example: 127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326
    private static final Pattern COMMON_LOG_PATTERN = Pattern.compile(
            "^(\\S+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"([^\"]+)\" (\\d+) (\\d+|-)$"
    );

    // Regex pattern for Apache combined log format
    // Example: 127.0.0.1 - frank [10/Oct/2000:13:55:36 -0700] "GET /apache_pb.gif HTTP/1.0" 200 2326 "http://www.example.com/start.html" "Mozilla/4.08 [en] (Win98; I ;Nav)"
    private static final Pattern COMBINED_LOG_PATTERN = Pattern.compile(
            "^(\\S+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"([^\"]+)\" (\\d+) (\\d+|-) \"([^\"]*)\" \"([^\"]*)\"$"
    );

    // Regex pattern for Apache error log format
    // Example: [Wed Oct 11 14:32:52 2000] [error] [pid 12345] [client 127.0.0.1] File does not exist: /path/to/file
    private static final Pattern ERROR_LOG_PATTERN = Pattern.compile(
            "^\\[([^\\]]+)\\] \\[([^\\]]+)\\](?: \\[pid (\\d+)\\])?(?: \\[client ([^\\]]+)\\])? (.+)$"
    );

    /**
     * Checks if a log line is in a recognized Apache format.
     *
     * @param logLine The log line to check
     * @return true if the log line is in a recognized Apache format, false otherwise
     */
    public boolean isApacheLogFormat(String logLine) {
        return COMMON_LOG_PATTERN.matcher(logLine).matches() ||
               COMBINED_LOG_PATTERN.matcher(logLine).matches() ||
               ERROR_LOG_PATTERN.matcher(logLine).matches();
    }

    /**
     * Parses a log line in Apache format into a LogEntry.
     *
     * @param logLine The log line to parse
     * @param source The source of the log (filename)
     * @return A LogEntry with extracted fields, or null if the log line is not in a recognized Apache format
     */
    public LogEntry parseApacheLogLine(String logLine, String source) {
        // Try combined log format first
        Matcher combinedMatcher = COMBINED_LOG_PATTERN.matcher(logLine);
        if (combinedMatcher.matches()) {
            return parseCombinedLogFormat(combinedMatcher, logLine, source);
        }

        // Try common log format
        Matcher commonMatcher = COMMON_LOG_PATTERN.matcher(logLine);
        if (commonMatcher.matches()) {
            return parseCommonLogFormat(commonMatcher, logLine, source);
        }

        // Try error log format
        Matcher errorMatcher = ERROR_LOG_PATTERN.matcher(logLine);
        if (errorMatcher.matches()) {
            return parseErrorLogFormat(errorMatcher, logLine, source);
        }

        // Not a recognized Apache log format
        logger.debug("Log line is not in a recognized Apache format: {}", logLine);
        return null;
    }

    /**
     * Parses a log line in Apache combined log format.
     *
     * @param matcher The matcher with captured groups
     * @param logLine The original log line
     * @param source The source of the log (filename)
     * @return A LogEntry with extracted fields
     */
    private LogEntry parseCombinedLogFormat(Matcher matcher, String logLine, String source) {
        String ipAddress = matcher.group(1);
        String identd = matcher.group(2);
        String userId = matcher.group(3);
        String timestamp = matcher.group(4);
        String request = matcher.group(5);
        String statusCode = matcher.group(6);
        String bytes = matcher.group(7);
        String referer = matcher.group(8);
        String userAgent = matcher.group(9);

        // Extract HTTP method, path, and protocol from request
        String[] requestParts = request.split(" ", 3);
        String method = requestParts.length > 0 ? requestParts[0] : "";
        String path = requestParts.length > 1 ? requestParts[1] : "";
        String protocol = requestParts.length > 2 ? requestParts[2] : "";

        // Create metadata with extracted fields
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ip_address", ipAddress);
        metadata.put("identd", identd);
        metadata.put("user_id", userId);
        metadata.put("request", request);
        metadata.put("method", method);
        metadata.put("path", path);
        metadata.put("protocol", protocol);
        metadata.put("status_code", statusCode);
        metadata.put("bytes", bytes.equals("-") ? "0" : bytes);
        metadata.put("referer", referer);
        metadata.put("user_agent", userAgent);
        metadata.put("log_format", "apache_combined");

        // Determine log level based on status code
        String level = determineLogLevelFromStatusCode(statusCode);

        // Extract timestamp
        Long recordTime = parseApacheTimestamp(timestamp);

        // Create a message that summarizes the log entry
        String message = String.format("%s %s %s %s", ipAddress, method, path, statusCode);

        return new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                recordTime,
                level,
                message,
                source,
                metadata,
                logLine
        );
    }

    /**
     * Parses a log line in Apache common log format.
     *
     * @param matcher The matcher with captured groups
     * @param logLine The original log line
     * @param source The source of the log (filename)
     * @return A LogEntry with extracted fields
     */
    private LogEntry parseCommonLogFormat(Matcher matcher, String logLine, String source) {
        String ipAddress = matcher.group(1);
        String identd = matcher.group(2);
        String userId = matcher.group(3);
        String timestamp = matcher.group(4);
        String request = matcher.group(5);
        String statusCode = matcher.group(6);
        String bytes = matcher.group(7);

        // Extract HTTP method, path, and protocol from request
        String[] requestParts = request.split(" ", 3);
        String method = requestParts.length > 0 ? requestParts[0] : "";
        String path = requestParts.length > 1 ? requestParts[1] : "";
        String protocol = requestParts.length > 2 ? requestParts[2] : "";

        // Create metadata with extracted fields
        Map<String, String> metadata = new HashMap<>();
        metadata.put("ip_address", ipAddress);
        metadata.put("identd", identd);
        metadata.put("user_id", userId);
        metadata.put("request", request);
        metadata.put("method", method);
        metadata.put("path", path);
        metadata.put("protocol", protocol);
        metadata.put("status_code", statusCode);
        metadata.put("bytes", bytes.equals("-") ? "0" : bytes);
        metadata.put("log_format", "apache_common");

        // Determine log level based on status code
        String level = determineLogLevelFromStatusCode(statusCode);

        // Extract timestamp
        Long recordTime = parseApacheTimestamp(timestamp);

        // Create a message that summarizes the log entry
        String message = String.format("%s %s %s %s", ipAddress, method, path, statusCode);

        return new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                recordTime,
                level,
                message,
                source,
                metadata,
                logLine
        );
    }

    /**
     * Parses a log line in Apache error log format.
     *
     * @param matcher The matcher with captured groups
     * @param logLine The original log line
     * @param source The source of the log (filename)
     * @return A LogEntry with extracted fields
     */
    private LogEntry parseErrorLogFormat(Matcher matcher, String logLine, String source) {
        String timestamp = matcher.group(1);
        String logLevel = matcher.group(2);
        String processId = matcher.group(3); // May be null
        String clientIp = matcher.group(4);  // May be null
        String errorMessage = matcher.group(5);

        // Create metadata with extracted fields
        Map<String, String> metadata = new HashMap<>();
        metadata.put("timestamp", timestamp);
        metadata.put("log_level", logLevel);
        if (processId != null) metadata.put("process_id", processId);
        if (clientIp != null) metadata.put("client_ip", clientIp);
        metadata.put("error_message", errorMessage);
        metadata.put("log_format", "apache_error");

        // Map Apache log level to system log level
        String level = mapApacheLogLevelToSystemLevel(logLevel);

        // Extract timestamp
        Long recordTime = DateTimeRegexPatterns.extractDateTimeToTimestamp(timestamp);

        // Create a message that summarizes the log entry
        String message = String.format("[%s] %s", logLevel, errorMessage);
        if (clientIp != null) {
            message = String.format("[%s] [client %s] %s", logLevel, clientIp, errorMessage);
        }

        return new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                recordTime,
                level,
                message,
                source,
                metadata,
                logLine
        );
    }

    /**
     * Parses a timestamp in Apache format (e.g., "10/Oct/2000:13:55:36 -0700").
     *
     * @param timestamp The timestamp string to parse
     * @return The timestamp in milliseconds since epoch, or null if parsing fails
     */
    private Long parseApacheTimestamp(String timestamp) {
        try {
            // Convert Apache timestamp format to a format that DateTimeRegexPatterns can handle
            // Example: "10/Oct/2000:13:55:36 -0700" -> "10 Oct 2000 13:55:36"
            String[] parts = timestamp.split(":");
            if (parts.length >= 3) {
                String datePart = parts[0]; // "10/Oct/2000"
                String timePart = parts[1] + ":" + parts[2].split(" ")[0]; // "13:55:36"
                
                datePart = datePart.replace("/", " "); // "10 Oct 2000"
                
                String reformattedTimestamp = datePart + " " + timePart; // "10 Oct 2000 13:55:36"
                
                long time = DateTimeRegexPatterns.extractDateTimeToTimestamp(reformattedTimestamp);
                if (time != -1) {
                    return time;
                }
            }
            
            // If the above parsing fails, try using the DateTimeRegexPatterns directly
            return DateTimeRegexPatterns.extractDateTimeToTimestamp(timestamp);
        } catch (Exception e) {
            logger.warn("Failed to parse Apache timestamp: {}", timestamp, e);
            return null;
        }
    }

    /**
     * Determines the log level based on the HTTP status code.
     *
     * @param statusCode The HTTP status code
     * @return The corresponding log level
     */
    private String determineLogLevelFromStatusCode(String statusCode) {
        int code = Integer.parseInt(statusCode);
        if (code >= 500) {
            return "ERROR";
        } else if (code >= 400) {
            return "WARN";
        } else {
            return "INFO";
        }
    }

    /**
     * Maps Apache log levels to system log levels.
     *
     * @param apacheLogLevel The Apache log level
     * @return The corresponding system log level
     */
    private String mapApacheLogLevelToSystemLevel(String apacheLogLevel) {
        return switch (apacheLogLevel.toLowerCase()) {
            case "emerg", "alert", "crit", "error", "fatal" -> "ERROR";
            case "warn", "warning", "notice" -> "WARN";
            case "info" -> "INFO";
            case "debug", "trace" -> "DEBUG";
            default -> "UNKNOWN";
        };
    }
}