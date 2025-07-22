package io.github.ozkanpakdil.grepwise.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Configuration for search sharding settings.
 * This class defines how search is distributed across multiple shards.
 */
public class ShardConfiguration {

    private String id;

    /**
     * The type of sharding to use.
     * Possible values: TIME_BASED, SOURCE_BASED, BALANCED
     */
    private String shardingType = "TIME_BASED";

    /**
     * The number of shards to distribute the search across.
     */
    private int numberOfShards = 3;

    /**
     * Whether to enable replication for high availability.
     */
    private boolean replicationEnabled = false;

    /**
     * The replication factor (number of replicas per shard).
     */
    private int replicationFactor = 1;

    /**
     * List of shard node URLs.
     */
    private List<String> shardNodes = new ArrayList<>();

    /**
     * Whether sharding is enabled.
     */
    private boolean shardingEnabled = false;

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
    public ShardConfiguration() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getShardingType() {
        return shardingType;
    }

    public void setShardingType(String shardingType) {
        if (!shardingType.equals("TIME_BASED") && !shardingType.equals("SOURCE_BASED") && !shardingType.equals("BALANCED")) {
            throw new IllegalArgumentException("Sharding type must be one of: TIME_BASED, SOURCE_BASED, BALANCED");
        }
        this.shardingType = shardingType;
    }

    public int getNumberOfShards() {
        return numberOfShards;
    }

    public void setNumberOfShards(int numberOfShards) {
        if (numberOfShards < 1) {
            throw new IllegalArgumentException("Number of shards must be at least 1");
        }
        this.numberOfShards = numberOfShards;
    }

    public boolean isReplicationEnabled() {
        return replicationEnabled;
    }

    public void setReplicationEnabled(boolean replicationEnabled) {
        this.replicationEnabled = replicationEnabled;
    }

    public int getReplicationFactor() {
        return replicationFactor;
    }

    public void setReplicationFactor(int replicationFactor) {
        if (replicationFactor < 1) {
            throw new IllegalArgumentException("Replication factor must be at least 1");
        }
        this.replicationFactor = replicationFactor;
    }

    public List<String> getShardNodes() {
        return shardNodes;
    }

    public void setShardNodes(List<String> shardNodes) {
        this.shardNodes = shardNodes;
    }

    public boolean isShardingEnabled() {
        return shardingEnabled;
    }

    public void setShardingEnabled(boolean shardingEnabled) {
        this.shardingEnabled = shardingEnabled;
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
        ShardConfiguration that = (ShardConfiguration) o;
        return numberOfShards == that.numberOfShards &&
                replicationEnabled == that.replicationEnabled &&
                replicationFactor == that.replicationFactor &&
                shardingEnabled == that.shardingEnabled &&
                Objects.equals(id, that.id) &&
                Objects.equals(shardingType, that.shardingType) &&
                Objects.equals(shardNodes, that.shardNodes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, shardingType, numberOfShards, replicationEnabled,
                replicationFactor, shardNodes, shardingEnabled);
    }

    @Override
    public String toString() {
        return "ShardConfiguration{" +
                "id='" + id + '\'' +
                ", shardingType='" + shardingType + '\'' +
                ", numberOfShards=" + numberOfShards +
                ", replicationEnabled=" + replicationEnabled +
                ", replicationFactor=" + replicationFactor +
                ", shardNodes=" + shardNodes +
                ", shardingEnabled=" + shardingEnabled +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}