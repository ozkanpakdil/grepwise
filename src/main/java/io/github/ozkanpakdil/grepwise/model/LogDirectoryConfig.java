package io.github.ozkanpakdil.grepwise.model;

import java.util.Objects;

/**
 * Configuration for log directory scanning.
 */
public class LogDirectoryConfig {
    private String id;
    private String directoryPath;
    private boolean enabled;
    private String filePattern;
    private long scanIntervalSeconds;

    public LogDirectoryConfig() {
        this.enabled = true;
        this.filePattern = "*.log";
        this.scanIntervalSeconds = 60;
    }

    public LogDirectoryConfig(String id, String directoryPath, boolean enabled, String filePattern, long scanIntervalSeconds) {
        this.id = id;
        this.directoryPath = directoryPath;
        this.enabled = enabled;
        this.filePattern = filePattern;
        this.scanIntervalSeconds = scanIntervalSeconds;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
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
        return enabled == that.enabled &&
                scanIntervalSeconds == that.scanIntervalSeconds &&
                Objects.equals(id, that.id) &&
                Objects.equals(directoryPath, that.directoryPath) &&
                Objects.equals(filePattern, that.filePattern);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, directoryPath, enabled, filePattern, scanIntervalSeconds);
    }

    @Override
    public String toString() {
        return "LogDirectoryConfig{" +
                "id='" + id + '\'' +
                ", directoryPath='" + directoryPath + '\'' +
                ", enabled=" + enabled +
                ", filePattern='" + filePattern + '\'' +
                ", scanIntervalSeconds=" + scanIntervalSeconds +
                '}';
    }
}