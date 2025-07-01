package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogRepository;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * REST controller for searching logs.
 */
@RestController
@RequestMapping("/api/logs")
@CrossOrigin(origins = "*") // Allow requests from any origin for development
public class LogSearchController {

    private final LuceneService luceneService;

    public LogSearchController(LogRepository logRepository, LuceneService luceneService) {
        this.luceneService = luceneService;
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
    @GetMapping("/search")
    public ResponseEntity<List<LogEntry>> searchLogs(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) Long startTime,
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
    @GetMapping("/time-aggregation")
    public ResponseEntity<Map<Long, Integer>> getLogCountByTimeSlots(
            @RequestParam(required = false) String query,
            @RequestParam(required = false, defaultValue = "false") boolean isRegex,
            @RequestParam(required = false) String timeRange,
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
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
}
