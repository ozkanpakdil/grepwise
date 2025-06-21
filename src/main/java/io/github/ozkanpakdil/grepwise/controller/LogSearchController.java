package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.LogRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    private final LogRepository logRepository;

    public LogSearchController(LogRepository logRepository) {
        this.logRepository = logRepository;
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

        List<LogEntry> logs = logRepository.search(query, isRegex, startTime, endTime);
        return ResponseEntity.ok(logs);
    }

    /**
     * Get a log entry by ID.
     *
     * @param id The ID of the log entry to get
     * @return The log entry
     */
    @GetMapping("/{id}")
    public ResponseEntity<LogEntry> getLogById(@PathVariable String id) {
        LogEntry log = logRepository.findById(id);
        if (log == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(log);
    }

    /**
     * Get all log entries.
     *
     * @return A list of all log entries
     */
    @GetMapping
    public ResponseEntity<List<LogEntry>> getAllLogs() {
        return ResponseEntity.ok(logRepository.findAll());
    }

    /**
     * Get log entries by level.
     *
     * @param level The level to filter by
     * @return A list of log entries with the specified level
     */
    @GetMapping("/level/{level}")
    public ResponseEntity<List<LogEntry>> getLogsByLevel(@PathVariable String level) {
        return ResponseEntity.ok(logRepository.findByLevel(level));
    }

    /**
     * Get log entries by source.
     *
     * @param source The source to filter by
     * @return A list of log entries with the specified source
     */
    @GetMapping("/source/{source}")
    public ResponseEntity<List<LogEntry>> getLogsBySource(@PathVariable String source) {
        return ResponseEntity.ok(logRepository.findBySource(source));
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
        return ResponseEntity.ok(logRepository.findByTimeRange(startTime, endTime));
    }

    /**
     * Get available log levels.
     *
     * @return A list of unique log levels
     */
    @GetMapping("/levels")
    public ResponseEntity<List<String>> getLogLevels() {
        List<String> levels = logRepository.findAll().stream()
                .map(LogEntry::getLevel)
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(levels);
    }

    /**
     * Get available log sources.
     *
     * @return A list of unique log sources
     */
    @GetMapping("/sources")
    public ResponseEntity<List<String>> getLogSources() {
        List<String> sources = logRepository.findAll().stream()
                .map(LogEntry::getSource)
                .distinct()
                .collect(Collectors.toList());
        return ResponseEntity.ok(sources);
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
        List<LogEntry> logs = logRepository.search(query, isRegex, startTime, endTime);

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
            long timeToCheck = log.getRecordTime() != null ? log.getRecordTime() : log.getTimestamp();

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
