package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.ShardConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for storing and retrieving shard configurations.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class ShardConfigurationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ShardConfigurationRepository.class);
    private final Map<String, ShardConfiguration> configurations = new ConcurrentHashMap<>();

    /**
     * Constructor that initializes the repository with default shard configuration.
     */
    public ShardConfigurationRepository() {
        logger.info("Initializing ShardConfigurationRepository");

        // Create default shard configuration
        createDefaultShardConfiguration();

        logger.info("Initialized ShardConfigurationRepository with {} default configurations", configurations.size());
    }

    /**
     * Create default shard configuration.
     */
    private void createDefaultShardConfiguration() {
        // Default shard configuration
        ShardConfiguration defaultConfig = new ShardConfiguration();
        defaultConfig.setShardingType("TIME_BASED");
        defaultConfig.setNumberOfShards(3);
        defaultConfig.setReplicationEnabled(false);
        defaultConfig.setReplicationFactor(1);
        defaultConfig.setShardingEnabled(false); // Disabled by default

        save(defaultConfig);
        logger.debug("Created default shard configuration: {}", defaultConfig);
    }

    /**
     * Save a shard configuration.
     *
     * @param configuration The configuration to save
     * @return The saved configuration with a generated ID
     */
    public ShardConfiguration save(ShardConfiguration configuration) {
        if (configuration.getId() == null || configuration.getId().isEmpty()) {
            configuration.setId(UUID.randomUUID().toString());
        }
        configuration.setUpdatedAt(Instant.now());
        configurations.put(configuration.getId(), configuration);
        return configuration;
    }

    /**
     * Find a shard configuration by ID.
     *
     * @param id The ID of the configuration to find
     * @return The configuration, or null if not found
     */
    public ShardConfiguration findById(String id) {
        return configurations.get(id);
    }

    /**
     * Find all shard configurations.
     *
     * @return A list of all configurations
     */
    public List<ShardConfiguration> findAll() {
        return new ArrayList<>(configurations.values());
    }

    /**
     * Find the active shard configuration.
     *
     * @return The active shard configuration, or null if none exists
     */
    public ShardConfiguration findByShardingEnabledTrue() {
        return configurations.values().stream()
                .filter(ShardConfiguration::isShardingEnabled)
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the default shard configuration.
     *
     * @return The default shard configuration
     */
    public ShardConfiguration getDefaultConfiguration() {
        if (configurations.isEmpty()) {
            createDefaultShardConfiguration();
        }
        return configurations.values().iterator().next();
    }

    /**
     * Delete a shard configuration by ID.
     *
     * @param id The ID of the configuration to delete
     * @return true if the configuration was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return configurations.remove(id) != null;
    }

    /**
     * Delete all shard configurations.
     *
     * @return The number of configurations deleted
     */
    public int deleteAll() {
        int count = configurations.size();
        configurations.clear();
        return count;
    }

    /**
     * Get the total number of shard configurations.
     *
     * @return The total number of configurations
     */
    public int count() {
        return configurations.size();
    }
}