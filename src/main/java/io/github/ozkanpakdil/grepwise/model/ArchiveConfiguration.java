package io.github.ozkanpakdil.grepwise.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Configuration for log archiving settings.
 */
public class ArchiveConfiguration {

    private String id;

    /**
     * Directory where archives will be stored.
     */
    private String archiveDirectory = "./archives";

    /**
     * Whether to automatically archive logs before deletion.
     */
    private boolean autoArchiveEnabled = true;

    /**
     * Compression level (0-9, where 0 is no compression and 9 is maximum compression).
     */
    private int compressionLevel = 5;

    /**
     * Maximum size of a single archive file in MB.
     */
    private int maxArchiveSizeMb = 100;

    /**
     * Number of days to retain archives before they can be deleted.
     */
    private int archiveRetentionDays = 90;

    /**
     * When this configuration was created.
     */
    private Instant createdAt;

    /**
     * When this configuration was last updated.
     */
    private Instant updatedAt;

    public ArchiveConfiguration() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getArchiveDirectory() {
        return archiveDirectory;
    }

    public void setArchiveDirectory(String archiveDirectory) {
        this.archiveDirectory = archiveDirectory;
    }

    public boolean isAutoArchiveEnabled() {
        return autoArchiveEnabled;
    }

    public void setAutoArchiveEnabled(boolean autoArchiveEnabled) {
        this.autoArchiveEnabled = autoArchiveEnabled;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public void setCompressionLevel(int compressionLevel) {
        if (compressionLevel < 0 || compressionLevel > 9) {
            throw new IllegalArgumentException("Compression level must be between 0 and 9");
        }
        this.compressionLevel = compressionLevel;
    }

    public int getMaxArchiveSizeMb() {
        return maxArchiveSizeMb;
    }

    public void setMaxArchiveSizeMb(int maxArchiveSizeMb) {
        this.maxArchiveSizeMb = maxArchiveSizeMb;
    }

    public int getArchiveRetentionDays() {
        return archiveRetentionDays;
    }

    public void setArchiveRetentionDays(int archiveRetentionDays) {
        this.archiveRetentionDays = archiveRetentionDays;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArchiveConfiguration that = (ArchiveConfiguration) o;
        return autoArchiveEnabled == that.autoArchiveEnabled &&
                compressionLevel == that.compressionLevel &&
                maxArchiveSizeMb == that.maxArchiveSizeMb &&
                archiveRetentionDays == that.archiveRetentionDays &&
                Objects.equals(id, that.id) &&
                Objects.equals(archiveDirectory, that.archiveDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, archiveDirectory, autoArchiveEnabled,
                compressionLevel, maxArchiveSizeMb, archiveRetentionDays);
    }

    @Override
    public String toString() {
        return "ArchiveConfiguration{" +
                "id='" + id + '\'' +
                ", archiveDirectory='" + archiveDirectory + '\'' +
                ", autoArchiveEnabled=" + autoArchiveEnabled +
                ", compressionLevel=" + compressionLevel +
                ", maxArchiveSizeMb=" + maxArchiveSizeMb +
                ", archiveRetentionDays=" + archiveRetentionDays +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}