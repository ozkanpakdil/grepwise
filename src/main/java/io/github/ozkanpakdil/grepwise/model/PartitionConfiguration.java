package io.github.ozkanpakdil.grepwise.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Configuration for log data partitioning settings.
 * This class defines how log data is partitioned in the Lucene index.
 */
public class PartitionConfiguration {

    private String id;

    /**
     * The type of partitioning to use.
     * Possible values: DAILY, WEEKLY, MONTHLY
     */
    private String partitionType = "MONTHLY";

    /**
     * The maximum number of partitions to keep active.
     * Older partitions will be archived and removed from the active set.
     */
    private int maxActivePartitions = 3;

    /**
     * Whether to automatically archive partitions when they are removed from the active set.
     */
    private boolean autoArchivePartitions = true;

    /**
     * Base directory where partitioned indices will be stored.
     * Each partition will be a subdirectory of this directory.
     */
    private String partitionBaseDirectory = "./lucene-index/partitions";

    /**
     * Whether partitioning is enabled.
     */
    private boolean partitioningEnabled = true;

    /**
     * When this configuration was created.
     */
    private Instant createdAt;

    /**
     * When this configuration was last updated.
     */
    private Instant updatedAt;

    /**
     * Default constructor.
     */
    public PartitionConfiguration() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPartitionType() {
        return partitionType;
    }

    public void setPartitionType(String partitionType) {
        if (!partitionType.equals("DAILY") && !partitionType.equals("WEEKLY") && !partitionType.equals("MONTHLY")) {
            throw new IllegalArgumentException("Partition type must be one of: DAILY, WEEKLY, MONTHLY");
        }
        this.partitionType = partitionType;
    }

    public int getMaxActivePartitions() {
        return maxActivePartitions;
    }

    public void setMaxActivePartitions(int maxActivePartitions) {
        if (maxActivePartitions < 1) {
            throw new IllegalArgumentException("Maximum active partitions must be at least 1");
        }
        this.maxActivePartitions = maxActivePartitions;
    }

    public boolean isAutoArchivePartitions() {
        return autoArchivePartitions;
    }

    public void setAutoArchivePartitions(boolean autoArchivePartitions) {
        this.autoArchivePartitions = autoArchivePartitions;
    }

    public String getPartitionBaseDirectory() {
        return partitionBaseDirectory;
    }

    public void setPartitionBaseDirectory(String partitionBaseDirectory) {
        this.partitionBaseDirectory = partitionBaseDirectory;
    }

    public boolean isPartitioningEnabled() {
        return partitioningEnabled;
    }

    public void setPartitioningEnabled(boolean partitioningEnabled) {
        this.partitioningEnabled = partitioningEnabled;
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
        PartitionConfiguration that = (PartitionConfiguration) o;
        return maxActivePartitions == that.maxActivePartitions &&
                autoArchivePartitions == that.autoArchivePartitions &&
                partitioningEnabled == that.partitioningEnabled &&
                Objects.equals(id, that.id) &&
                Objects.equals(partitionType, that.partitionType) &&
                Objects.equals(partitionBaseDirectory, that.partitionBaseDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, partitionType, maxActivePartitions, autoArchivePartitions,
                partitionBaseDirectory, partitioningEnabled);
    }

    @Override
    public String toString() {
        return "PartitionConfiguration{" +
                "id='" + id + '\'' +
                ", partitionType='" + partitionType + '\'' +
                ", maxActivePartitions=" + maxActivePartitions +
                ", autoArchivePartitions=" + autoArchivePartitions +
                ", partitionBaseDirectory='" + partitionBaseDirectory + '\'' +
                ", partitioningEnabled=" + partitioningEnabled +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}