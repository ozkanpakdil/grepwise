package io.github.ozkanpakdil.grepwise.model;

import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Represents metadata about an archive of logs.
 */
public class ArchiveMetadata {

    private String id;
    private String filename;
    private Instant creationTime;
    private long startTimestamp;
    private long endTimestamp;
    private int logCount;
    private long sizeBytes;
    private Set<String> sources;
    private String compressionType;
    private int compressionLevel;
    private boolean isAvailable;

    /**
     * Default constructor.
     */
    public ArchiveMetadata() {
        this.creationTime = Instant.now();
        this.sources = new HashSet<>();
        this.isAvailable = true;
        this.compressionType = "zip";
    }

    /**
     * Constructor with parameters.
     */
    public ArchiveMetadata(String filename, long startTimestamp, long endTimestamp,
                           int logCount, long sizeBytes, Set<String> sources,
                           String compressionType, int compressionLevel) {
        this();
        this.filename = filename;
        this.startTimestamp = startTimestamp;
        this.endTimestamp = endTimestamp;
        this.logCount = logCount;
        this.sizeBytes = sizeBytes;
        if (sources != null) {
            this.sources.addAll(sources);
        }
        this.compressionType = compressionType;
        this.compressionLevel = compressionLevel;
    }

    // Getters and setters

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Instant getCreationTime() {
        return creationTime;
    }

    public void setCreationTime(Instant creationTime) {
        this.creationTime = creationTime;
    }

    public long getStartTimestamp() {
        return startTimestamp;
    }

    public void setStartTimestamp(long startTimestamp) {
        this.startTimestamp = startTimestamp;
    }

    public long getEndTimestamp() {
        return endTimestamp;
    }

    public void setEndTimestamp(long endTimestamp) {
        this.endTimestamp = endTimestamp;
    }

    public int getLogCount() {
        return logCount;
    }

    public void setLogCount(int logCount) {
        this.logCount = logCount;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(long sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public Set<String> getSources() {
        return sources;
    }

    public void setSources(Set<String> sources) {
        this.sources = sources != null ? sources : new HashSet<>();
    }

    public void addSource(String source) {
        if (source != null && !source.isEmpty()) {
            this.sources.add(source);
        }
    }

    public String getCompressionType() {
        return compressionType;
    }

    public void setCompressionType(String compressionType) {
        this.compressionType = compressionType;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        this.compressionLevel = compressionLevel;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public void setAvailable(boolean available) {
        isAvailable = available;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArchiveMetadata that = (ArchiveMetadata) o;
        return startTimestamp == that.startTimestamp &&
                endTimestamp == that.endTimestamp &&
                logCount == that.logCount &&
                sizeBytes == that.sizeBytes &&
                compressionLevel == that.compressionLevel &&
                isAvailable == that.isAvailable &&
                Objects.equals(id, that.id) &&
                Objects.equals(filename, that.filename) &&
                Objects.equals(creationTime, that.creationTime) &&
                Objects.equals(sources, that.sources) &&
                Objects.equals(compressionType, that.compressionType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, filename, creationTime, startTimestamp, endTimestamp,
                logCount, sizeBytes, sources, compressionType, compressionLevel, isAvailable);
    }

    @Override
    public String toString() {
        return "ArchiveMetadata{" +
                "id='" + id + '\'' +
                ", filename='" + filename + '\'' +
                ", creationTime=" + creationTime +
                ", startTimestamp=" + startTimestamp +
                ", endTimestamp=" + endTimestamp +
                ", logCount=" + logCount +
                ", sizeBytes=" + sizeBytes +
                ", sources=" + sources +
                ", compressionType='" + compressionType + '\'' +
                ", compressionLevel=" + compressionLevel +
                ", isAvailable=" + isAvailable +
                '}';
    }
}