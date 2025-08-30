package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import io.github.ozkanpakdil.grepwise.service.LogBufferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller for receiving logs via HTTP.
 * Provides endpoints for receiving single log entries or batches of log entries.
 */
@RestController
@RequestMapping("/api/logs")
public class HttpLogController {
    private static final Logger logger = LoggerFactory.getLogger(HttpLogController.class);

    private final LogBufferService logBufferService;
    private final Map<String, LogSourceConfig> httpSources = new ConcurrentHashMap<>();

    public HttpLogController(LogBufferService logBufferService) {
        this.logBufferService = logBufferService;
        logger.info("HttpLogController initialized");
    }

    /**
     * Register an HTTP log source configuration.
     * This is used to validate incoming requests against configured sources.
     *
     * @param config The HTTP log source configuration
     * @return true if the source was registered successfully, false otherwise
     */
    public boolean registerHttpSource(LogSourceConfig config) {
        if (config.getSourceType() != LogSourceConfig.SourceType.HTTP) {
            logger.error("Cannot register non-HTTP source type: {}", config.getSourceType());
            return false;
        }

        httpSources.put(config.getId(), config);
        logger.info("Registered HTTP log source: {}", config.getId());
        return true;
    }

    /**
     * Unregister an HTTP log source configuration.
     *
     * @param configId The ID of the HTTP log source configuration
     * @return true if the source was unregistered successfully, false otherwise
     */
    public boolean unregisterHttpSource(String configId) {
        LogSourceConfig config = httpSources.remove(configId);
        if (config != null) {
            logger.info("Unregistered HTTP log source: {}", configId);
            return true;
        } else {
            logger.warn("No HTTP log source found with ID: {}", configId);
            return false;
        }
    }

    /**
     * Get the number of registered HTTP log sources.
     *
     * @return The number of registered sources
     */
    public int getRegisteredSourceCount() {
        return httpSources.size();
    }

    /**
     * Receive a single log entry via HTTP POST.
     *
     * @param sourceId   The ID of the HTTP log source configuration
     * @param authToken  The authentication token (if required)
     * @param logRequest The log entry request
     * @return A response indicating success or failure
     */
    @PostMapping("/{sourceId}")
    public ResponseEntity<Map<String, Object>> receiveLog(
            @PathVariable String sourceId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody LogRequest logRequest) {

        logger.debug("Received log entry for source: {}", sourceId);

        // Validate the source and authentication
        ResponseEntity<Map<String, Object>> validationResponse = validateRequest(sourceId, authToken);
        if (validationResponse != null) {
            return validationResponse;
        }

        LogSourceConfig sourceConfig = httpSources.get(sourceId);

        try {
            // Create a log entry from the request
            LogEntry logEntry = createLogEntry(logRequest, sourceId);

            // Add the log entry to the buffer
            logBufferService.addToBuffer(logEntry);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Log entry received and processed");
            response.put("id", logEntry.id());

            logger.debug("Successfully processed log entry for source: {}", sourceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing log entry for source: {}", sourceId, e);

            // Return error response
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error processing log entry: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Receive multiple log entries via HTTP POST.
     *
     * @param sourceId    The ID of the HTTP log source configuration
     * @param authToken   The authentication token (if required)
     * @param logRequests The log entry requests
     * @return A response indicating success or failure
     */
    @PostMapping("/{sourceId}/batch")
    public ResponseEntity<Map<String, Object>> receiveLogBatch(
            @PathVariable String sourceId,
            @RequestHeader(value = "X-Auth-Token", required = false) String authToken,
            @RequestBody List<LogRequest> logRequests) {

        logger.debug("Received batch of {} log entries for source: {}", logRequests.size(), sourceId);

        // Validate the source and authentication
        ResponseEntity<Map<String, Object>> validationResponse = validateRequest(sourceId, authToken);
        if (validationResponse != null) {
            return validationResponse;
        }

        LogSourceConfig sourceConfig = httpSources.get(sourceId);

        try {
            // Create log entries from the requests
            List<LogEntry> logEntries = logRequests.stream()
                    .map(request -> createLogEntry(request, sourceId))
                    .toList();

            // Add the log entries to the buffer
            int processedCount = logBufferService.addAllToBuffer(logEntries);

            // Return success response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Batch of log entries received and processed");
            response.put("count", processedCount);

            logger.debug("Successfully processed batch of {} log entries for source: {}", processedCount, sourceId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing batch of log entries for source: {}", sourceId, e);

            // Return error response
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Error processing batch of log entries: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Validate a request against the registered HTTP log sources.
     *
     * @param sourceId  The ID of the HTTP log source configuration
     * @param authToken The authentication token (if required)
     * @return A response entity if validation fails, null if validation succeeds
     */
    private ResponseEntity<Map<String, Object>> validateRequest(String sourceId, String authToken) {
        // Check if the source exists
        LogSourceConfig sourceConfig = httpSources.get(sourceId);
        if (sourceConfig == null) {
            logger.warn("Unknown HTTP log source: {}", sourceId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Unknown log source");

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        // Check if the source is enabled
        if (!sourceConfig.isEnabled()) {
            logger.warn("HTTP log source is disabled: {}", sourceId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Log source is disabled");

            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }

        // Check authentication if required
        if (sourceConfig.isRequireAuth()) {
            if (authToken == null || !authToken.equals(sourceConfig.getHttpAuthToken())) {
                logger.warn("Invalid authentication token for HTTP log source: {}", sourceId);

                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Invalid authentication token");

                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
        }

        // Validation succeeded
        return null;
    }

    /**
     * Create a log entry from a log request.
     *
     * @param request  The log request
     * @param sourceId The ID of the HTTP log source
     * @return A LogEntry representing the request
     */
    private LogEntry createLogEntry(LogRequest request, String sourceId) {
        // Parse the timestamp if provided, otherwise use current time
        Long recordTime = request.timestamp != null ? request.timestamp : System.currentTimeMillis();

        // Use the provided level or default to INFO
        String level = request.level != null ? request.level : "INFO";

        // Create metadata map
        Map<String, String> metadata = new HashMap<>();
        if (request.metadata != null) {
            metadata.putAll(request.metadata);
        }
        metadata.put("source_type", "http");
        metadata.put("source_id", sourceId);

        // Create the log entry
        return new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                recordTime,
                level,
                request.message,
                "http:" + sourceId,
                metadata,
                request.rawContent != null ? request.rawContent : request.message
        );
    }

    /**
     * Request object for receiving log entries via HTTP.
     */
    public static class LogRequest {
        private String message;
        private Long timestamp;
        private String level;
        private Map<String, String> metadata;
        private String rawContent;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }

        public String getLevel() {
            return level;
        }

        public void setLevel(String level) {
            this.level = level;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }

        public void setMetadata(Map<String, String> metadata) {
            this.metadata = metadata;
        }

        public String getRawContent() {
            return rawContent;
        }

        public void setRawContent(String rawContent) {
            this.rawContent = rawContent;
        }
    }
}