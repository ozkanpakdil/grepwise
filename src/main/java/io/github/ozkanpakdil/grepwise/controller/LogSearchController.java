package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogRepository;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
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

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.StringJoiner;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * REST controller for searching logs.
 */
@Tag(name = "Log Search", description = "API endpoints for searching and analyzing logs")
@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class LogSearchController {

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
     * @param query The search query
     * @param isRegex Whether the query is a regex pattern
     * @param timeRange Predefined time range (1h, 3h, 12h, 24h, custom)
     * @param startTime Custom start time (for custom time range)
     * @param endTime Custom end time (for custom time range)
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
            return ResponseEntity.ok(logs);
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
                case LOG_ENTRIES -> ResponseEntity.ok(result.getLogEntries());
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
    public ResponseEntity<LogEntry> getLogById(@PathVariable String id) {
        try {
            LogEntry log = luceneService.findById(id);
            if (log == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(log);
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
            return ResponseEntity.ok(luceneService.search(null, false, null, null));
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
            return ResponseEntity.ok(luceneService.findByLevel(level));
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
            return ResponseEntity.ok(luceneService.findBySource(source));
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
            return ResponseEntity.ok(luceneService.search(null, false, startTime, endTime));
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
     * @param query The search query
     * @param isRegex Whether the query is a regex pattern
     * @param timeRange Predefined time range (1h, 3h, 12h, 24h, custom)
     * @param startTime Custom start time (for custom time range)
     * @param endTime Custom end time (for custom time range)
     * @param slots Number of time slots to divide the range into
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
     * @param query Search query string
     * @param isRegex Whether to treat the query as a regular expression
     * @param from Start time in milliseconds since epoch
     * @param to End time in milliseconds since epoch
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
        
        // Count logs in each interval
        for (LogEntry log : logs) {
            // Use record time if available, otherwise use entry time
            long timeToCheck = log.recordTime() != null ? log.recordTime() : log.timestamp();
            
            // Find which interval this log belongs to
            int intervalIndex = (int) ((timeToCheck - from) / intervalMs);
            
            // Ensure the interval index is valid
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
                    .atZone(ZoneId.systemDefault())
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
     * @param enabled Whether the cache is enabled
     * @param maxSize Maximum cache size
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
     * @param query The search query
     * @param isRegex Whether the query is a regular expression
     * @param timeRange Predefined time range (1h, 3h, 12h, 24h, custom)
     * @param startTime Custom start time (epoch milliseconds)
     * @param endTime Custom end time (epoch milliseconds)
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
     * @param query The search query
     * @param isRegex Whether the query is a regular expression
     * @param timeRange Predefined time range (1h, 3h, 12h, 24h, custom)
     * @param startTime Custom start time (epoch milliseconds)
     * @param endTime Custom end time (epoch milliseconds)
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
    
}
