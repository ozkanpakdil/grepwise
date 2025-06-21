package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving log entries.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class LogRepository {
    private final Map<String, LogEntry> logs = new ConcurrentHashMap<>();

    /**
     * Constructor that initializes the repository with dummy data.
     */
    public LogRepository() {
        initializeDummyData();
    }

    /**
     * Initialize the repository with dummy data.
     * This is for demonstration purposes only.
     */
    private void initializeDummyData() {
        // Create dummy log entries with "test" in the message
        createDummyLogEntry("ERROR", "Failed to connect to test database", "app-server-1");
        createDummyLogEntry("WARNING", "High memory usage detected in test environment", "app-server-2");
        createDummyLogEntry("INFO", "User logged in successfully to test account", "auth-service");
        createDummyLogEntry("DEBUG", "Processing test request payload", "api-gateway");
        createDummyLogEntry("ERROR", "Test API rate limit exceeded", "rate-limiter");
    }

    /**
     * Helper method to create a dummy log entry.
     *
     * @param level The log level
     * @param message The log message
     * @param source The log source
     */
    private void createDummyLogEntry(String level, String message, String source) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("dummy", "true");
        metadata.put("environment", "test");

        LogEntry logEntry = new LogEntry(
                UUID.randomUUID().toString(),
                System.currentTimeMillis(),
                level,
                message,
                source,
                metadata,
                level + " [" + source + "] " + message
        );

        save(logEntry);
    }

    /**
     * Save a log entry.
     *
     * @param logEntry The log entry to save
     * @return The saved log entry with a generated ID
     */
    public LogEntry save(LogEntry logEntry) {
        if (logEntry.getId() == null || logEntry.getId().isEmpty()) {
            logEntry.setId(UUID.randomUUID().toString());
        }
        logs.put(logEntry.getId(), logEntry);
        return logEntry;
    }

    /**
     * Save multiple log entries.
     *
     * @param logEntries The log entries to save
     * @return The number of log entries saved
     */
    public int saveAll(List<LogEntry> logEntries) {
        logEntries.forEach(this::save);
        return logEntries.size();
    }

    /**
     * Find a log entry by ID.
     *
     * @param id The ID of the log entry to find
     * @return The log entry, or null if not found
     */
    public LogEntry findById(String id) {
        return logs.get(id);
    }

    /**
     * Find all log entries.
     *
     * @return A list of all log entries
     */
    public List<LogEntry> findAll() {
        return new ArrayList<>(logs.values());
    }

    /**
     * Find log entries by level.
     *
     * @param level The level to filter by
     * @return A list of log entries with the specified level
     */
    public List<LogEntry> findByLevel(String level) {
        return logs.values().stream()
                .filter(log -> level.equalsIgnoreCase(log.getLevel()))
                .collect(Collectors.toList());
    }

    /**
     * Find log entries by source.
     *
     * @param source The source to filter by
     * @return A list of log entries with the specified source
     */
    public List<LogEntry> findBySource(String source) {
        return logs.values().stream()
                .filter(log -> source.equals(log.getSource()))
                .collect(Collectors.toList());
    }

    /**
     * Find log entries by time range.
     *
     * @param startTime The start time (inclusive)
     * @param endTime   The end time (inclusive)
     * @return A list of log entries within the specified time range
     */
    public List<LogEntry> findByTimeRange(long startTime, long endTime) {
        return logs.values().stream()
                .filter(log -> log.getTimestamp() >= startTime && log.getTimestamp() <= endTime)
                .collect(Collectors.toList());
    }

    /**
     * Search log entries by a query string.
     * Supports both simple text search and regex search.
     *
     * @param query The query string
     * @param isRegex Whether the query is a regex pattern
     * @param startTime The start time for filtering (optional)
     * @param endTime The end time for filtering (optional)
     * @return A list of log entries matching the query and time range
     */
    public List<LogEntry> search(String query, boolean isRegex, Long startTime, Long endTime) {
        if ((query == null || query.isEmpty()) && startTime == null && endTime == null) {
            return findAll();
        }

        return logs.values().stream()
                .filter(log -> {
                    // Filter by time range if provided
                    if (startTime != null && endTime != null) {
                        // Use record time if available, otherwise use entry time
                        long timeToCheck = log.getRecordTime() != null ? log.getRecordTime() : log.getTimestamp();
                        if (timeToCheck < startTime || timeToCheck > endTime) {
                            return false;
                        }
                    }

                    // If no query, just filter by time
                    if (query == null || query.isEmpty()) {
                        return true;
                    }

                    // Check if message matches the query
                    if (log.getMessage() == null) {
                        return false;
                    }

                    if (isRegex) {
                        try {
                            return log.getMessage().matches(query);
                        } catch (Exception e) {
                            // If regex is invalid, fall back to simple contains
                            return log.getMessage().toLowerCase().contains(query.toLowerCase());
                        }
                    } else {
                        return log.getMessage().toLowerCase().contains(query.toLowerCase());
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Search log entries by a simple query.
     * This is a basic implementation that just checks if the message contains the query string.
     * 
     * @param query The query string
     * @return A list of log entries matching the query
     */
    public List<LogEntry> search(String query) {
        return search(query, false, null, null);
    }

    /**
     * Delete a log entry by ID.
     *
     * @param id The ID of the log entry to delete
     * @return true if the log entry was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return logs.remove(id) != null;
    }

    /**
     * Delete all log entries.
     *
     * @return The number of log entries deleted
     */
    public int deleteAll() {
        int count = logs.size();
        logs.clear();
        return count;
    }

    /**
     * Get the total number of log entries.
     *
     * @return The total number of log entries
     */
    public int count() {
        return logs.size();
    }
}
