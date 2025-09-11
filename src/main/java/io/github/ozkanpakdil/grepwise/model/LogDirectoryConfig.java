package io.github.ozkanpakdil.grepwise.model;

import java.util.Objects;

/**
 * Configuration for log directory scanning.
 */
public class LogDirectoryConfig {
    private String id;
    private String directoryPath;
    private String filePattern;
    private long scanIntervalSeconds;
    private boolean enabled;
    // 'enabled' has been removed from configuration semantics. Kept via methods for backward compatibility.

    public LogDirectoryConfig() {
        this.filePattern = "*.log";
        this.scanIntervalSeconds = 60;
    }

    // Backward compatible ctors
    public LogDirectoryConfig(String id, String directoryPath, String filePattern, long scanIntervalSeconds) {
        this.id = id;
        this.directoryPath = directoryPath;
        this.filePattern = filePattern;
        this.scanIntervalSeconds = scanIntervalSeconds;
    }
    // Deprecated: maintained for backward compatibility with older tests/usages
    @Deprecated
    public LogDirectoryConfig(String id, String directoryPath, boolean enabled, String filePattern, long scanIntervalSeconds) {
        this(id, directoryPath, filePattern, scanIntervalSeconds);
        this.enabled = enabled; // maintain backward compatibility for tests/usages that pass enabled
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDirectoryPath() {
        return directoryPath;
    }

    public void setDirectoryPath(String directoryPath) {
        this.directoryPath = directoryPath;
    }


    public String getFilePattern() {
        return filePattern;
    }

    public void setFilePattern(String filePattern) {
        this.filePattern = filePattern;
    }

    public long getScanIntervalSeconds() {
        return scanIntervalSeconds;
    }

    public void setScanIntervalSeconds(long scanIntervalSeconds) {
        this.scanIntervalSeconds = scanIntervalSeconds;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LogDirectoryConfig that = (LogDirectoryConfig) o;
        return scanIntervalSeconds == that.scanIntervalSeconds &&
                Objects.equals(id, that.id) &&
                Objects.equals(directoryPath, that.directoryPath) &&
                Objects.equals(filePattern, that.filePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, directoryPath, filePattern, scanIntervalSeconds);
    }

    @Override
    public String toString() {
        return "LogDirectoryConfig{" +
                "id='" + id + '\'' +
                ", directoryPath='" + directoryPath + '\'' +
                ", filePattern='" + filePattern + '\'' +
                ", scanIntervalSeconds=" + scanIntervalSeconds +
                '}';
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean _enabled) { enabled = _enabled; }
}