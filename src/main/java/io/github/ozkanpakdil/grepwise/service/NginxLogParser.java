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
 * Service for parsing Nginx log formats.
 * Supports common Nginx log formats including:
 * - Combined Log Format
 * - Common Log Format
 * - Error Log Format
 */
@Service
public class NginxLogParser {
    private static final Logger logger = LoggerFactory.getLogger(NginxLogParser.class);

    // Regex pattern for Nginx combined log format
    // Example: 192.168.1.1 - user [10/Oct/2023:13:55:36 +0000] "GET /api/users HTTP/1.1" 200 2326 "http://example.com/page" "Mozilla/5.0"
    private static final Pattern COMBINED_LOG_PATTERN = Pattern.compile(
            "^(\\S+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"([^\"]+)\" (\\d+) (\\d+) \"([^\"]*)\" \"([^\"]*)\"$"
    );

    // Regex pattern for Nginx common log format
    // Example: 192.168.1.1 - user [10/Oct/2023:13:55:36 +0000] "GET /api/users HTTP/1.1" 200 2326
    private static final Pattern COMMON_LOG_PATTERN = Pattern.compile(
            "^(\\S+) (\\S+) (\\S+) \\[([^\\]]+)\\] \"([^\"]+)\" (\\d+) (\\d+)$"
    );

    // Regex pattern for Nginx error log format
    // Example: 2023/10/10 13:55:36 [error] 12345#0: *67890 error message, client: 192.168.1.1, server: example.com, request: "GET /api/users HTTP/1.1"
    private static final Pattern ERROR_LOG_PATTERN = Pattern.compile(
            "^(\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}) \\[(\\w+)\\] (\\d+)#\\d+: \\*\\d+ (.+?), client: ([^,]+)(?:, server: ([^,]+))?(?:, request: \"([^\"]+)\")?.*$"
    );

    /**
     * Checks if a log line is in a recognized Nginx format.
     *
     * @param logLine The log line to check
     * @return true if the log line is in a recognized Nginx format, false otherwise
     */
    public boolean isNginxLogFormat(String logLine) {
        return COMBINED_LOG_PATTERN.matcher(logLine).matches() ||
               COMMON_LOG_PATTERN.matcher(logLine).matches() ||
               ERROR_LOG_PATTERN.matcher(logLine).matches();
    }

    /**
     * Parses a log line in Nginx format into a LogEntry.
     *
     * @param logLine The log line to parse
     * @param source The source of the log (filename)
     * @return A LogEntry with extracted fields, or null if the log line is not in a recognized Nginx format
     */
    public LogEntry parseNginxLogLine(String logLine, String source) {
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

        // Not a recognized Nginx log format
        logger.debug("Log line is not in a recognized Nginx format: {}", logLine);
        return null;
    }

    /**
     * Parses a log line in Nginx combined log format.
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
        metadata.put("bytes", bytes);
        metadata.put("referer", referer);
        metadata.put("user_agent", userAgent);
        metadata.put("log_format", "nginx_combined");

        // Determine log level based on status code
        String level = determineLogLevelFromStatusCode(statusCode);

        // Extract timestamp
        Long recordTime = parseNginxTimestamp(timestamp);

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
     * Parses a log line in Nginx common log format.
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
        metadata.put("bytes", bytes);
        metadata.put("log_format", "nginx_common");

        // Determine log level based on status code
        String level = determineLogLevelFromStatusCode(statusCode);

        // Extract timestamp
        Long recordTime = parseNginxTimestamp(timestamp);

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
     * Parses a log line in Nginx error log format.
     *
     * @param matcher The matcher with captured groups
     * @param logLine The original log line
     * @param source The source of the log (filename)
     * @return A LogEntry with extracted fields
     */
    private LogEntry parseErrorLogFormat(Matcher matcher, String logLine, String source) {
        String timestamp = matcher.group(1);
        String logLevel = matcher.group(2);
        String processId = matcher.group(3);
        String errorMessage = matcher.group(4);
        String clientIp = matcher.group(5);
        String server = matcher.group(6);
        String request = matcher.group(7);

        // Extract HTTP method, path, and protocol from request if available
        String method = "";
        String path = "";
        String protocol = "";
        if (request != null) {
            String[] requestParts = request.split(" ", 3);
            method = requestParts.length > 0 ? requestParts[0] : "";
            path = requestParts.length > 1 ? requestParts[1] : "";
            protocol = requestParts.length > 2 ? requestParts[2] : "";
        }

        // Create metadata with extracted fields
        Map<String, String> metadata = new HashMap<>();
        metadata.put("timestamp", timestamp);
        metadata.put("log_level", logLevel);
        metadata.put("process_id", processId);
        metadata.put("error_message", errorMessage);
        metadata.put("client_ip", clientIp);
        if (server != null) metadata.put("server", server);
        if (request != null) {
            metadata.put("request", request);
            metadata.put("method", method);
            metadata.put("path", path);
            metadata.put("protocol", protocol);
        }
        metadata.put("log_format", "nginx_error");

        // Map Nginx log level to system log level
        String level = mapNginxLogLevelToSystemLevel(logLevel);

        // Extract timestamp
        Long recordTime = DateTimeRegexPatterns.extractDateTimeToTimestamp(timestamp);

        // Create a message that summarizes the log entry
        String message = String.format("[%s] %s - %s", logLevel, clientIp, errorMessage);

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
     * Parses a timestamp in Nginx format (e.g., "10/Oct/2023:13:55:36 +0000").
     *
     * @param timestamp The timestamp string to parse
     * @return The timestamp in milliseconds since epoch, or null if parsing fails
     */
    private Long parseNginxTimestamp(String timestamp) {
        try {
            // Convert Nginx timestamp format to a format that DateTimeRegexPatterns can handle
            // Example: "10/Oct/2023:13:55:36 +0000" -> "10 Oct 2023 13:55:36"
            String[] parts = timestamp.split(":");
            if (parts.length >= 3) {
                String datePart = parts[0]; // "10/Oct/2023"
                String timePart = parts[1] + ":" + parts[2].split(" ")[0]; // "13:55:36"
                
                datePart = datePart.replace("/", " "); // "10 Oct 2023"
                
                String reformattedTimestamp = datePart + " " + timePart; // "10 Oct 2023 13:55:36"
                
                long time = DateTimeRegexPatterns.extractDateTimeToTimestamp(reformattedTimestamp);
                if (time != -1) {
                    return time;
                }
            }
            
            // If the above parsing fails, try using the DateTimeRegexPatterns directly
            return DateTimeRegexPatterns.extractDateTimeToTimestamp(timestamp);
        } catch (Exception e) {
            logger.warn("Failed to parse Nginx timestamp: {}", timestamp, e);
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
     * Maps Nginx log levels to system log levels.
     *
     * @param nginxLogLevel The Nginx log level
     * @return The corresponding system log level
     */
    private String mapNginxLogLevelToSystemLevel(String nginxLogLevel) {
        return switch (nginxLogLevel.toLowerCase()) {
            case "emerg", "alert", "crit", "error" -> "ERROR";
            case "warn", "notice" -> "WARN";
            case "info" -> "INFO";
            case "debug" -> "DEBUG";
            default -> "UNKNOWN";
        };
    }
}