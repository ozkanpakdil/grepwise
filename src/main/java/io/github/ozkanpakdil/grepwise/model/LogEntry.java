package io.github.ozkanpakdil.grepwise.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a log entry in the system.
 */
public class LogEntry {
    private String id;
    private long timestamp;
    private Long recordTime;
    private String level;
    private String message;
    private String source;
    private Map<String, String> metadata;
    private String rawContent;

    public LogEntry() {
        this.metadata = new HashMap<>();
    }

    public LogEntry(String id, long timestamp, Long recordTime, String level, String message, String source, Map<String, String> metadata, String rawContent) {
        this.id = id;
        this.timestamp = timestamp;
        this.recordTime = recordTime;
        this.level = level;
        this.message = message;
        this.source = source;
        this.metadata = metadata != null ? metadata : new HashMap<>();
        this.rawContent = rawContent;
    }

    public LogEntry(String id, long timestamp, String level, String message, String source, Map<String, String> metadata, String rawContent) {
        this(id, timestamp, null, level, message, source, metadata, rawContent);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getRecordTime() {
        return recordTime;
    }

    public void setRecordTime(Long recordTime) {
        this.recordTime = recordTime;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogEntry logEntry = (LogEntry) o;
        return timestamp == logEntry.timestamp &&
                Objects.equals(id, logEntry.id) &&
                Objects.equals(recordTime, logEntry.recordTime) &&
                Objects.equals(level, logEntry.level) &&
                Objects.equals(message, logEntry.message) &&
                Objects.equals(source, logEntry.source) &&
                Objects.equals(metadata, logEntry.metadata) &&
                Objects.equals(rawContent, logEntry.rawContent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, timestamp, recordTime, level, message, source, metadata, rawContent);
    }

    @Override
    public String toString() {
        return "LogEntry{" +
                "id='" + id + '\'' +
                ", timestamp=" + timestamp +
                ", recordTime=" + recordTime +
                ", level='" + level + '\'' +
                ", message='" + message + '\'' +
                ", source='" + source + '\'' +
                ", metadata=" + metadata +
                ", rawContent='" + rawContent + '\'' +
                '}';
    }
}
