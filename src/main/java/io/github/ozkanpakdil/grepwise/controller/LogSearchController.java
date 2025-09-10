package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogRepository;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import io.github.ozkanpakdil.grepwise.service.RedactionUtil;
import io.github.ozkanpakdil.grepwise.service.SearchCacheService;
import io.github.ozkanpakdil.grepwise.service.SplQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * REST controller for searching logs.
 */
@Tag(name = "Log Search", description = "API endpoints for searching and analyzing logs")
@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class LogSearchController {
    private static final String SEARCH_MASK = "*****";

    private LogEntry redactLog(LogEntry log, String mask) {
        if (log == null) return null;
        // Redact message, rawContent, and metadata values
        String redactedMessage = RedactionUtil.redactIfSensitive(log.message(), mask);
        String redactedRaw = RedactionUtil.redactIfSensitive(log.rawContent(), mask);
        Map<String, String> md = new HashMap<>(log.metadata() == null ? Collections.emptyMap() : log.metadata());
        RedactionUtil.redactMetadataValues(md, mask);
        return new io.github.ozkanpakdil.grepwise.model.LogEntry(
                log.id(),
                log.timestamp(),
                log.recordTime(),
                log.level(),
                redactedMessage,
                log.source(),
                md,
                redactedRaw
        );
    }

    private final LuceneService luceneService;
    private final SplQueryService splQueryService;
    private final SearchCacheService searchCacheService;

    public LogSearchController(LogRepository logRepository, LuceneService luceneService, SplQueryService splQueryService, SearchCacheService searchCacheService) {
        this.luceneService = luceneService;
        this.splQueryService = splQueryService;
        this.searchCacheService = searchCacheService;
    }

    /**
     * Search logs by query with advanced options.
     *
     * @param query     The search query
     * @param isRegex   Whether the query is a regex pattern
     * @param timeRange Predefined time range (1h, 3h, 12h, 24h, custom)
     * @param startTime Custom start time (for custom time range)
     * @param endTime   Custom end time (for custom time range)
     * @return A list of matching log entries
     */
    @Operation(
            summary = "Search logs",
            description = "Search logs using a query string with support for regex and time range filtering"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful search operation",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LogEntry.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during search operation",
                    content = @Content
            )
    })
    @GetMapping("/search")
    public ResponseEntity<List<LogEntry>> searchLogs(
            @Parameter(description = "Search query string")
            @RequestParam(required = false) String query,

            @Parameter(description = "Whether to treat the query as a regular expression")
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,

            @Parameter(description = "Predefined time range (1h, 3h, 12h, 24h, custom)")
            @RequestParam(required = false) String timeRange,

            @Parameter(description = "Custom start time in milliseconds since epoch (for custom time range)")
            @RequestParam(required = false) Long startTime,

            @Parameter(description = "Custom end time in milliseconds since epoch (for custom time range)")
            @RequestParam(required = false) Long endTime) {

        // Calculate time range if a predefined range is specified
        if (timeRange != null && !timeRange.equals("custom")) {
            long now = System.currentTimeMillis();
            long hours = 0;

            switch (timeRange) {
                case "1h":
                    hours = 1;
                    break;
                case "3h":
                    hours = 3;
                    break;
                case "12h":
                    hours = 12;
                    break;
                case "24h":
                    hours = 24;
                    break;
                default:
                    // Invalid time range, ignore
                    break;
            }

            if (hours > 0) {
                endTime = now;
                startTime = now - (hours * 60 * 60 * 1000); // Convert hours to milliseconds
            }
        }

        try {
            List<LogEntry> logs = luceneService.search(query, isRegex, startTime, endTime);
            List<LogEntry> redacted = logs.stream().map(l -> redactLog(l, SEARCH_MASK)).collect(Collectors.toList());
            return ResponseEntity.ok(redacted);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Execute SPL (Splunk Processing Language) query.
     *
     * @param splQuery The SPL query string (e.g., "search error | stats count by level")
     * @return Query results (either log entries or statistics)
     */
    @Operation(
            summary = "Execute SPL query",
            description = "Execute a Splunk Processing Language (SPL) query to search and analyze logs. " +
                    "Supports commands like 'search', 'stats', 'where', 'eval', etc."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful query execution",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid SPL query syntax",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during query execution",
                    content = @Content(
                            mediaType = "application/json"
                    )
            )
    })
    @PostMapping("/spl")
    public ResponseEntity<?> executeSplQuery(
            @Parameter(
                    description = "SPL query string (e.g., \"search error | stats count by level\")",
                    required = true,
                    example = "search error | stats count by level"
            )
            @RequestBody String splQuery) {
        try {
            SplQueryService.SplQueryResult result = splQueryService.executeSplQuery(splQuery);

            return switch (result.getResultType()) {
                case LOG_ENTRIES -> {
                    List<LogEntry> redacted = result.getLogEntries().stream()
                            .map(l -> redactLog(l, SEARCH_MASK))
                            .collect(Collectors.toList());
                    yield ResponseEntity.ok(redacted);
                }
                case STATISTICS -> ResponseEntity.ok(result.getStatistics());
                default -> ResponseEntity.badRequest().body("Unknown result type");
            };
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body("Error executing SPL query: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Invalid SPL query: " + e.getMessage());
        }
    }

    /**
     * Get a log entry by ID.
     *
     * @param id The ID of the log entry to get
     * @return The log entry
     */
    @GetMapping("/{id}")
    public ResponseEntity<LogEntry> getLogById(@PathVariable String id,
                                               @RequestParam(required = false, defaultValue = "false") boolean reveal) {
        try {
            LogEntry log = luceneService.findById(id);
            if (log == null) {
                return ResponseEntity.notFound().build();
            }
            if (reveal) {
                return ResponseEntity.ok(log);
            }
            return ResponseEntity.ok(redactLog(log, SEARCH_MASK));
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get all log entries.
     *
     * @return A list of all log entries
     */
    @GetMapping
    public ResponseEntity<List<LogEntry>> getAllLogs() {
        try {
            // Pass null for query and time range to get all logs
            List<LogEntry> logs = luceneService.search(null, false, null, null);
            List<LogEntry> redacted = logs.stream().map(l -> redactLog(l, SEARCH_MASK)).collect(Collectors.toList());
            return ResponseEntity.ok(redacted);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get log entries by level.
     *
     * @param level The level to filter by
     * @return A list of log entries with the specified level
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<LogEntry>> getLogsByLevel(@PathVariable String level) {
        try {
            List<LogEntry> logs = luceneService.findByLevel(level);
            List<LogEntry> redacted = logs.stream().map(l -> redactLog(l, SEARCH_MASK)).collect(Collectors.toList());
            return ResponseEntity.ok(redacted);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get log entries by source.
     *
     * @param source The source to filter by
     * @return A list of log entries with the specified source
     */
    @GetMapping("/source/{source}")
    public ResponseEntity<List<LogEntry>> getLogsBySource(@PathVariable String source) {
        try {
            List<LogEntry> logs = luceneService.findBySource(source);
            List<LogEntry> redacted = logs.stream().map(l -> redactLog(l, SEARCH_MASK)).collect(Collectors.toList());
            return ResponseEntity.ok(redacted);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get log entries by time range.
     *
     * @param startTime The start time (inclusive)
     * @param endTime   The end time (inclusive)
     * @return A list of log entries within the specified time range
     */
    @GetMapping("/time-range")
    public ResponseEntity<List<LogEntry>> getLogsByTimeRange(
            @RequestParam long startTime,
            @RequestParam long endTime) {
        try {
            // Use search with null query and specified time range
            List<LogEntry> logs = luceneService.search(null, false, startTime, endTime);
            List<LogEntry> redacted = logs.stream().map(l -> redactLog(l, SEARCH_MASK)).collect(Collectors.toList());
            return ResponseEntity.ok(redacted);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get available log levels.
     *
     * @return A list of unique log levels
     */
    @GetMapping("/levels")
    public ResponseEntity<List<String>> getLogLevels() {
        try {
            List<String> levels = luceneService.search(null, false, null, null).stream()
                    .map(LogEntry::level)
                    .distinct()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(levels);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get available log sources.
     *
     * @return A list of unique log sources
     */
    @GetMapping("/sources")
    public ResponseEntity<List<String>> getLogSources() {
        try {
            List<String> sources = luceneService.search(null, false, null, null).stream()
                    .map(LogEntry::source)
                    .distinct()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(sources);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Get log count aggregation by time slots.
     *
     * @param query     The search query
     * @param isRegex   Whether the query is a regex pattern
     * @param timeRange Predefined time range (1h, 3h, 12h, 24h, custom)
     * @param startTime Custom start time (for custom time range)
     * @param endTime   Custom end time (for custom time range)
     * @param slots     Number of time slots to divide the range into
     * @return A map of time slots to log counts
     */
    @Operation(
            summary = "Get log count by time slots",
            description = "Aggregates log counts into time slots for visualization. " +
                    "This endpoint is useful for creating time-based charts and graphs."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful aggregation",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error during aggregation",
                    content = @Content
            )
    })
    @GetMapping("/time-aggregation")
    public ResponseEntity<Map<Long, Integer>> getLogCountByTimeSlots(
            @Parameter(description = "Search query string")
            @RequestParam(required = false) String query,

            @Parameter(description = "Whether to treat the query as a regular expression")
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,

            @Parameter(description = "Predefined time range (1h, 3h, 12h, 24h, custom)")
            @RequestParam(required = false) String timeRange,

            @Parameter(description = "Custom start time in milliseconds since epoch (for custom time range)")
            @RequestParam(required = false) Long startTime,

            @Parameter(description = "Custom end time in milliseconds since epoch (for custom time range)")
            @RequestParam(required = false) Long endTime,

            @Parameter(description = "Number of time slots to divide the range into (default: 24)")
            @RequestParam(required = false, defaultValue = "24") int slots) {

        // Calculate time range if a predefined range is specified
        if (timeRange != null && !timeRange.equals("custom")) {
            long now = System.currentTimeMillis();
            long hours = 0;

            switch (timeRange) {
                case "1h":
                    hours = 1;
                    break;
                case "3h":
                    hours = 3;
                    break;
                case "12h":
                    hours = 12;
                    break;
                case "24h":
                    hours = 24;
                    break;
                default:
                    // Invalid time range, ignore
                    break;
            }

            if (hours > 0) {
                endTime = now;
                startTime = now - (hours * 60 * 60 * 1000); // Convert hours to milliseconds
            }
        }

        // Default to last 24 hours if no time range is specified
        if (startTime == null || endTime == null) {
            endTime = System.currentTimeMillis();
            startTime = endTime - (24 * 60 * 60 * 1000); // 24 hours
        }

        // Get logs matching the query and time range
        List<LogEntry> logs;
        try {
            logs = luceneService.search(query, isRegex, startTime, endTime);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        // Calculate the size of each time slot
        long timeRangeMs = endTime - startTime;
        long slotSizeMs = timeRangeMs / slots;

        // Initialize the result map with all slots (even empty ones)
        Map<Long, Integer> result = new TreeMap<>();
        for (int i = 0; i < slots; i++) {
            long slotStartTime = startTime + (i * slotSizeMs);
            result.put(slotStartTime, 0);
        }

        // Count logs in each time slot
        for (LogEntry log : logs) {
            // Use record time if available, otherwise use entry time
            long timeToCheck = log.recordTime() != null ? log.recordTime() : log.timestamp();

            // Find which slot this log belongs to
            int slotIndex = (int) ((timeToCheck - startTime) / slotSizeMs);

            // Ensure the slot index is valid
            if (slotIndex >= 0 && slotIndex < slots) {
                long slotStartTime = startTime + (slotIndex * slotSizeMs);
                result.put(slotStartTime, result.get(slotStartTime) + 1);
            }
        }

        return ResponseEntity.ok(result);
    }

    /**
     * Get histogram data for logs.
     * This endpoint provides time-based histogram data for logs matching the given query and time range.
     * It groups logs into time intervals and returns the count for each interval.
     *
     * @param query    Search query string
     * @param isRegex  Whether to treat the query as a regular expression
     * @param from     Start time in milliseconds since epoch
     * @param to       End time in milliseconds since epoch
     * @param interval Time interval (e.g., 1m, 5m, 1h)
     * @return List of timestamp-count pairs
     */
    @Operation(
            summary = "Get log histogram data",
            description = "Provides time-based histogram data for logs matching the given query and time range"
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful histogram generation",
                    content = @Content(
                            mediaType = "application/json"
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid parameters",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content
            )
    })
    @GetMapping("/histogram")
    public ResponseEntity<List<Map<String, Object>>> getLogHistogram(
            @Parameter(description = "Search query string")
            @RequestParam(required = false) String query,

            @Parameter(description = "Whether to treat the query as a regular expression")
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,

            @Parameter(description = "Start time in milliseconds since epoch")
            @RequestParam(required = true) Long from,

            @Parameter(description = "End time in milliseconds since epoch")
            @RequestParam(required = true) Long to,

            @Parameter(description = "Time interval (1m, 5m, 15m, 30m, 1h, 3h, 6h, 12h, 24h)")
            @RequestParam(required = true) String interval) {

        // Validate parameters
        if (from >= to) {
            return ResponseEntity.badRequest().build();
        }

        // Parse interval string to milliseconds
        long intervalMs;
        switch (interval) {
            case "1m":
                intervalMs = 60 * 1000; // 1 minute
                break;
            case "5m":
                intervalMs = 5 * 60 * 1000; // 5 minutes
                break;
            case "15m":
                intervalMs = 15 * 60 * 1000; // 15 minutes
                break;
            case "30m":
                intervalMs = 30 * 60 * 1000; // 30 minutes
                break;
            case "1h":
                intervalMs = 60 * 60 * 1000; // 1 hour
                break;
            case "3h":
                intervalMs = 3 * 60 * 60 * 1000; // 3 hours
                break;
            case "6h":
                intervalMs = 6 * 60 * 60 * 1000; // 6 hours
                break;
            case "12h":
                intervalMs = 12 * 60 * 60 * 1000; // 12 hours
                break;
            case "24h":
                intervalMs = 24 * 60 * 60 * 1000; // 24 hours
                break;
            default:
                return ResponseEntity.badRequest().build();
        }

        // Get logs matching the query and time range
        List<LogEntry> logs;
        try {
            logs = luceneService.search(query, isRegex, from, to);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        // Calculate number of intervals
        long timeRangeMs = to - from;
        int intervals = (int) Math.ceil((double) timeRangeMs / intervalMs);

        // Initialize the result map with all intervals (even empty ones)
        Map<Long, Integer> countsByTimestamp = new TreeMap<>();
        for (int i = 0; i < intervals; i++) {
            long intervalStartTime = from + (i * intervalMs);
            countsByTimestamp.put(intervalStartTime, 0);
        }

        // Count logs in each interval using consistent within-range time selection
        // We treat the upper bound as exclusive: [from, to)
        for (LogEntry log : logs) {
            Long rt = log.recordTime();
            long ts = log.timestamp();

            Long chosen = null;
            if (rt != null && rt >= from && rt < to) {
                chosen = rt;
            } else if (ts >= from && ts < to) {
                chosen = ts;
            }
            if (chosen == null) continue; // skip logs that matched only via the other field outside range

            int intervalIndex = (int) ((chosen - from) / intervalMs);
            if (intervalIndex >= 0 && intervalIndex < intervals) {
                long intervalStartTime = from + (intervalIndex * intervalMs);
                countsByTimestamp.put(intervalStartTime, countsByTimestamp.get(intervalStartTime) + 1);
            }
        }

        // Convert to the required output format
        List<Map<String, Object>> result = countsByTimestamp.entrySet().stream()
                .map(entry -> {
                    Map<String, Object> item = new java.util.HashMap<>();
                    // Format timestamp as ISO-8601 string
                    String timestamp = Instant.ofEpochMilli(entry.getKey())
                            .atZone(ZoneId.of("UTC"))
                            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    item.put("timestamp", timestamp);
                    item.put("count", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }

    /**
     * Get search cache statistics.
     * This endpoint provides information about the search cache performance,
     * including cache size, hit ratio, and other metrics.
     *
     * @return Cache statistics
     */
    @Operation(
            summary = "Get search cache statistics",
            description = "Provides information about the search cache performance, including cache size, hit ratio, and other metrics"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cache statistics retrieved successfully",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping("/cache/stats")
    public ResponseEntity<Map<String, Object>> getCacheStats() {
        Map<String, Object> stats = searchCacheService.getCacheStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear the search cache.
     * This endpoint clears all entries from the search cache.
     *
     * @return A message indicating the cache was cleared
     */
    @Operation(
            summary = "Clear search cache",
            description = "Clears all entries from the search cache"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cache cleared successfully",
            content = @Content(mediaType = "application/json")
    )
    @PostMapping("/cache/clear")
    public ResponseEntity<Map<String, String>> clearCache() {
        searchCacheService.clearCache();
        Map<String, String> response = Map.of("message", "Search cache cleared successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * Update search cache configuration.
     * This endpoint allows updating the cache configuration parameters.
     *
     * @param enabled      Whether the cache is enabled
     * @param maxSize      Maximum cache size
     * @param expirationMs Cache entry expiration time in milliseconds
     * @return The updated cache configuration
     */
    @Operation(
            summary = "Update search cache configuration",
            description = "Updates the search cache configuration parameters such as enabled status, maximum size, and expiration time"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Cache configuration updated successfully",
            content = @Content(mediaType = "application/json")
    )
    @PostMapping("/cache/config")
    public ResponseEntity<Map<String, Object>> updateCacheConfig(
            @Parameter(description = "Whether the cache is enabled")
            @RequestParam(required = false) Boolean enabled,

            @Parameter(description = "Maximum cache size (number of entries)")
            @RequestParam(required = false) Integer maxSize,

            @Parameter(description = "Cache entry expiration time in milliseconds")
            @RequestParam(required = false) Integer expirationMs) {

        if (enabled != null) {
            searchCacheService.setCacheEnabled(enabled);
        }

        if (maxSize != null && maxSize > 0) {
            searchCacheService.setMaxCacheSize(maxSize);
        }

        if (expirationMs != null && expirationMs > 0) {
            searchCacheService.setExpirationMs(expirationMs);
        }

        return ResponseEntity.ok(searchCacheService.getCacheStats());
    }

    /**
     * Export logs as CSV.
     * This endpoint exports logs matching the search criteria in CSV format.
     *
     * @param query     The search query
     * @param isRegex   Whether the query is a regular expression
     * @param timeRange Predefined time range (1h, 3h, 12h, 24h, custom)
     * @param startTime Custom start time (epoch milliseconds)
     * @param endTime   Custom end time (epoch milliseconds)
     * @return CSV file containing the logs
     */
    @Operation(
            summary = "Export logs as CSV",
            description = "Exports logs matching the search criteria in CSV format"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Logs exported successfully",
            content = @Content(mediaType = "text/csv")
    )
    @GetMapping(value = "/export/csv", produces = "text/csv")
    public ResponseEntity<String> exportLogsAsCsv(
            @Parameter(description = "Search query")
            @RequestParam(required = false, defaultValue = "") String query,

            @Parameter(description = "Whether the query is a regular expression")
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,

            @Parameter(description = "Predefined time range (1h, 3h, 12h, 24h, custom)")
            @RequestParam(required = false) String timeRange,

            @Parameter(description = "Custom start time (epoch milliseconds)")
            @RequestParam(required = false) Long startTime,

            @Parameter(description = "Custom end time (epoch milliseconds)")
            @RequestParam(required = false) Long endTime) {

        try {
            // Use the existing searchLogs method to get the logs
            ResponseEntity<List<LogEntry>> searchResponse = searchLogs(query, isRegex, timeRange, startTime, endTime);

            if (searchResponse.getStatusCode().isError() || searchResponse.getBody() == null) {
                return ResponseEntity.internalServerError().build();
            }

            List<LogEntry> logs = searchResponse.getBody();

            // Convert logs to CSV
            StringBuilder csvBuilder = new StringBuilder();

            // Add CSV header
            csvBuilder.append("ID,Timestamp,DateTime,Level,Source,Message,RawContent\n");

            // Format for timestamp conversion
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.systemDefault());

            // Add each log as a CSV row
            for (LogEntry log : logs) {
                String timestamp = formatter.format(Instant.ofEpochMilli(log.timestamp()));

                // Escape fields that might contain commas or quotes
                String message = escapeCSV(log.message());
                String source = escapeCSV(log.source());
                String rawContent = escapeCSV(log.rawContent());

                csvBuilder.append(log.id()).append(",")
                        .append(log.timestamp()).append(",")
                        .append(timestamp).append(",")
                        .append(log.level()).append(",")
                        .append(source).append(",")
                        .append(message).append(",")
                        .append(rawContent).append("\n");
            }

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "logs_export_" + System.currentTimeMillis() + ".csv");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvBuilder.toString());

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Export logs as JSON.
     * This endpoint exports logs matching the search criteria in JSON format.
     *
     * @param query     The search query
     * @param isRegex   Whether the query is a regular expression
     * @param timeRange Predefined time range (1h, 3h, 12h, 24h, custom)
     * @param startTime Custom start time (epoch milliseconds)
     * @param endTime   Custom end time (epoch milliseconds)
     * @return JSON file containing the logs
     */
    @Operation(
            summary = "Export logs as JSON",
            description = "Exports logs matching the search criteria in JSON format"
    )
    @ApiResponse(
            responseCode = "200",
            description = "Logs exported successfully",
            content = @Content(mediaType = "application/json")
    )
    @GetMapping(value = "/export/json", produces = "application/json")
    public ResponseEntity<List<LogEntry>> exportLogsAsJson(
            @Parameter(description = "Search query")
            @RequestParam(required = false, defaultValue = "") String query,

            @Parameter(description = "Whether the query is a regular expression")
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,

            @Parameter(description = "Predefined time range (1h, 3h, 12h, 24h, custom)")
            @RequestParam(required = false) String timeRange,

            @Parameter(description = "Custom start time (epoch milliseconds)")
            @RequestParam(required = false) Long startTime,

            @Parameter(description = "Custom end time (epoch milliseconds)")
            @RequestParam(required = false) Long endTime) {

        try {
            // Use the existing searchLogs method to get the logs
            ResponseEntity<List<LogEntry>> searchResponse = searchLogs(query, isRegex, timeRange, startTime, endTime);

            if (searchResponse.getStatusCode().isError() || searchResponse.getBody() == null) {
                return ResponseEntity.internalServerError().build();
            }

            List<LogEntry> logs = searchResponse.getBody();

            // Set headers for file download
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setContentDispositionFormData("attachment", "logs_export_" + System.currentTimeMillis() + ".json");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(logs);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Helper method to escape special characters in CSV fields.
     *
     * @param field The field to escape
     * @return The escaped field
     */
    private String escapeCSV(String field) {
        if (field == null) {
            return "";
        }

        // If the field contains commas, quotes, or newlines, wrap it in quotes and escape any quotes
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }

    @Operation(
            summary = "Progressive search stream",
            description = "Streams initial page of logs and progressive histogram updates via SSE"
    )
    @GetMapping(value = "/search/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "100") Integer pageSize,
            @RequestParam(required = false) String interval
    ) {
        // Resolve time range. If no explicit range, default histogram window to last 30 days (as per requirements)
        if (timeRange != null && !timeRange.equals("custom")) {
            long now = System.currentTimeMillis();
            long hours = switch (timeRange) {
                case "1h" -> 1;
                case "3h" -> 3;
                case "12h" -> 12;
                case "24h" -> 24;
                default -> 0;
            };
            if (hours > 0) {
                endTime = now;
                startTime = now - (hours * 60 * 60 * 1000);
            }
        }
        if (startTime == null || endTime == null) {
            // Default histogram range to last 30 days
            endTime = System.currentTimeMillis();
            startTime = endTime - (30L * 24 * 60 * 60 * 1000);
        }

        // Determine interval if not provided
        long rangeMs = endTime - startTime;
        final long fStartTime = startTime;
        final long fEndTime = endTime;
        if (interval == null || interval.isBlank()) {
            // Choose interval to target ~30 buckets and daily buckets for 30-day range
            long dayMs = 24L * 60 * 60 * 1000;
            if (rangeMs >= 25L * dayMs) {
                interval = "24h"; // daily for ~last 30 days
            } else if (rangeMs <= 60 * 60 * 1000) interval = "1m";
            else if (rangeMs <= 3 * 60 * 60 * 1000) interval = "5m";
            else if (rangeMs <= 12 * 60 * 60 * 1000) interval = "15m";
            else if (rangeMs <= 24 * 60 * 60 * 1000) interval = "30m";
            else interval = "1h";
        }
        long intervalMs = switch (interval) {
            case "1m" -> 60_000L;
            case "5m" -> 300_000L;
            case "15m" -> 900_000L;
            case "30m" -> 1_800_000L;
            case "1h" -> 3_600_000L;
            case "3h" -> 10_800_000L;
            case "6h" -> 21_600_000L;
            case "12h" -> 43_200_000L;
            case "24h" -> 86_400_000L;
            default -> 300_000L;
        };

        SseEmitter emitter = new SseEmitter(300000L);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        final long fIntervalMs = intervalMs;
        final long fRangeMs = rangeMs;
        final long fs = fStartTime;
        final long fe = fEndTime;
        final String fInterval = interval;

        exec.submit(() -> {
            try {
                // Initialize bucket count and counts array
                int bucketCount = (int) Math.ceil((double) fRangeMs / fIntervalMs);
                if (bucketCount <= 0) bucketCount = 1;

                // Send compact init (no full buckets array)
                emitter.send(SseEmitter.event().name("init").data(Map.of(
                        "from", fs,
                        "to", fe,
                        "interval", fInterval,
                        "bucketCount", bucketCount
                )));

                // Fetch all matching logs (synchronously for now)
                // Treat literal "*" as match-all
                String effectiveQuery = (query != null && query.trim().equals("*")) ? null : query;
                List<LogEntry> logs = luceneService.search(effectiveQuery, isRegex, fs, fe);

                // Send first page quickly
                int ps = Math.max(1, pageSize == null ? 100 : pageSize);
                List<LogEntry> firstPage = logs.subList(0, Math.min(ps, logs.size()));
                List<LogEntry> redFirst = firstPage.stream().map(l -> redactLog(l, SEARCH_MASK)).collect(Collectors.toList());
                emitter.send(SseEmitter.event().name("page").data(redFirst));

                // Progressively aggregate
                int batch = 200; // update cadence
                int processed = 0;
                // Process logs in descending time order (latest to earliest)
                logs.sort((a, b) -> Long.compare(
                        b.recordTime() != null ? b.recordTime() : b.timestamp(),
                        a.recordTime() != null ? a.recordTime() : a.timestamp()
                ));

                // Final snapshot and done
                emitter.send(SseEmitter.event().name("done").data(Map.of("total", logs.size())));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(ex);
            } finally {
                exec.shutdown();
            }
        });

        return emitter;
    }

    @Operation(
            summary = "Progressive timetable stream",
            description = "Streams progressive histogram (time table) updates via SSE without log pages"
    )
    @GetMapping(value = "/search/timetable/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamTimeTable(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String interval
    ) {
        // Resolve time range similar to streamSearch. If no explicit range, default to last 30 days
        if (timeRange != null && !timeRange.equals("custom")) {
            long now = System.currentTimeMillis();
            long hours = switch (timeRange) {
                case "1h" -> 1;
                case "3h" -> 3;
                case "12h" -> 12;
                case "24h" -> 24;
                default -> 0;
            };
            if (hours > 0) {
                endTime = now;
                startTime = now - (hours * 60 * 60 * 1000);
            }
        }
        if (startTime == null || endTime == null) {
            endTime = System.currentTimeMillis();
            startTime = endTime - (30L * 24 * 60 * 60 * 1000);
        }

        long rangeMs = endTime - startTime;
        final long fs = startTime;
        final long fe = endTime;
        if (interval == null || interval.isBlank()) {
            long dayMs = 24L * 60 * 60 * 1000;
            if (rangeMs >= 25L * dayMs) {
                interval = "24h";
            } else if (rangeMs <= 60 * 60 * 1000) interval = "1m";
            else if (rangeMs <= 3 * 60 * 60 * 1000) interval = "5m";
            else if (rangeMs <= 12 * 60 * 60 * 1000) interval = "15m";
            else if (rangeMs <= 24 * 60 * 60 * 1000) interval = "30m";
            else interval = "1h";
        }
        long intervalMs = switch (interval) {
            case "1m" -> 60_000L;
            case "5m" -> 300_000L;
            case "15m" -> 900_000L;
            case "30m" -> 1_800_000L;
            case "1h" -> 3_600_000L;
            case "3h" -> 10_800_000L;
            case "6h" -> 21_600_000L;
            case "12h" -> 43_200_000L;
            case "24h" -> 86_400_000L;
            default -> 300_000L;
        };

        SseEmitter emitter = new SseEmitter(300000L);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        final long fIntervalMs = intervalMs;
        final long fRangeMs = rangeMs;
        final String fInterval = interval;

        exec.submit(() -> {
            try {
                // Initialize buckets
                long buckets = (long) Math.ceil((double) fRangeMs / fIntervalMs);
                Map<Long, Integer> countsByTs = new TreeMap<>();
                for (int i = 0; i < buckets; i++) {
                    long ts = fs + (i * fIntervalMs);
                    countsByTs.put(ts, 0);
                }

                // Send init with zeroed buckets (UTC ISO timestamps)
                List<Map<String, Object>> initBuckets = new ArrayList<>();
                for (Long ts : countsByTs.keySet()) {
                    Map<String, Object> b = new HashMap<>();
                    String iso = Instant.ofEpochMilli(ts).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    b.put("timestamp", iso);
                    b.put("count", 0);
                    initBuckets.add(b);
                }
                emitter.send(SseEmitter.event().name("init").data(Map.of(
                        "from", fs,
                        "to", fe,
                        "interval", fInterval,
                        "buckets", initBuckets
                )));

                // Query logs and progressively aggregate only histogram
                String effectiveQuery = (query != null && query.trim().equals("*")) ? null : query;
                List<LogEntry> logs = luceneService.search(effectiveQuery, isRegex, fs, fe);

                // Latest-to-earliest to fill from rightmost backwards visually
                logs.sort((a, b) -> Long.compare(
                        b.recordTime() != null ? b.recordTime() : b.timestamp(),
                        a.recordTime() != null ? a.recordTime() : a.timestamp()
                ));

                int batch = 200;
                int processed = 0;
                for (LogEntry log : logs) {
                    Long rt = log.recordTime();
                    long ts = log.timestamp();
                    Long chosen = null;
                    if (rt != null && rt >= fs && rt < fe) {
                        chosen = rt;
                    } else if (ts >= fs && ts < fe) {
                        chosen = ts;
                    }
                    if (chosen != null) {
                        int idx = (int) ((chosen - fs) / fIntervalMs);
                        if (idx >= 0 && idx < buckets) {
                            long bucketTs = fs + (idx * fIntervalMs);
                            countsByTs.put(bucketTs, countsByTs.get(bucketTs) + 1);
                        }
                    }
                    processed++;
                    if (processed % batch == 0) {
                        List<Map<String, Object>> snapshot = countsByTs.entrySet().stream().map(e -> {
                            Map<String, Object> m = new HashMap<>();
                            String iso = Instant.ofEpochMilli(e.getKey()).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                            m.put("timestamp", iso);
                            m.put("count", e.getValue());
                            return m;
                        }).collect(Collectors.toList());
                        emitter.send(SseEmitter.event().name("hist").data(snapshot));
                    }
                }

                // Final snapshot & done
                List<Map<String, Object>> finalSnapshot = countsByTs.entrySet().stream().map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    String iso = Instant.ofEpochMilli(e.getKey()).atZone(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
                    m.put("timestamp", iso);
                    m.put("count", e.getValue());
                    return m;
                }).collect(Collectors.toList());
                emitter.send(SseEmitter.event().name("hist").data(finalSnapshot));
                emitter.send(SseEmitter.event().name("done").data(Map.of("total", logs.size())));
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(ex.getMessage()));
                } catch (IOException ignored) {
                }
                emitter.completeWithError(ex);
            } finally {
                exec.shutdown();
            }
        });

        return emitter;
    }

    /**
     * Paged search endpoint to support pagination in UI.
     */
    @GetMapping("/search/page")
    public ResponseEntity<Map<String, Object>> searchLogsPaged(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "100") Integer pageSize
    ) {
        // Resolve time range similar to other endpoints
        if (timeRange != null && !timeRange.equals("custom")) {
            long now = System.currentTimeMillis();
            long hours = switch (timeRange) {
                case "1h" -> 1;
                case "3h" -> 3;
                case "12h" -> 12;
                case "24h" -> 24;
                default -> 0;
            };
            if (hours > 0) {
                endTime = now;
                startTime = now - (hours * 60 * 60 * 1000);
            }
        }
        if (startTime == null || endTime == null) {
            // Align default range with SSE stream (last 30 days) to keep totals and pages consistent
            endTime = System.currentTimeMillis();
            startTime = endTime - (30L * 24 * 60 * 60 * 1000);
        }

        try {
            // Treat blank query as null (match all)
            if (query != null && query.trim().equals("*")) {
                query = null;
            }
            List<LogEntry> logs = luceneService.search(query, isRegex, startTime, endTime);
            int total = logs.size();
            int ps = Math.max(1, pageSize == null ? 100 : pageSize);
            int p = Math.max(1, page == null ? 1 : page);
            int fromIndex = Math.min((p - 1) * ps, total);
            int toIndex = Math.min(fromIndex + ps, total);
            List<LogEntry> items = logs.subList(fromIndex, toIndex);
            List<LogEntry> redactedItems = items.stream().map(l -> redactLog(l, SEARCH_MASK)).collect(Collectors.toList());

            Map<String, Object> body = new HashMap<>();
            body.put("items", redactedItems);
            body.put("total", total);
            body.put("page", p);
            body.put("pageSize", ps);
            return ResponseEntity.ok(body);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

}
