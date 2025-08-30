package io.github.ozkanpakdil.grepwise.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a log entry in the system.
 */
public record LogEntry(
        String id,
        long timestamp,
        Long recordTime,
        String level,
        String message,
        String source,
        Map<String, String> metadata,
        String rawContent
) {
    public LogEntry {
        metadata = metadata != null ? metadata : new HashMap<>();
    }

    public LogEntry(String id, long timestamp, String level, String message, String source, Map<String, String> metadata,
                    String rawContent) {
        this(id, timestamp, null, level, message, source, metadata, rawContent);
    }

    public LogEntry() {
        this(null, 0L, null, null, null, null, new HashMap<>(), null);
    }
}
