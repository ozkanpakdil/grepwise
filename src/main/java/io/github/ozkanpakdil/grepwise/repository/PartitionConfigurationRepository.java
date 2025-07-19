package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.PartitionConfiguration;
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
 * Repository for storing and retrieving partition configurations.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class PartitionConfigurationRepository {
    private static final Logger logger = LoggerFactory.getLogger(PartitionConfigurationRepository.class);
    private final Map<String, PartitionConfiguration> configurations = new ConcurrentHashMap<>();

    /**
     * Constructor that initializes the repository with default partition configuration.
     */
    public PartitionConfigurationRepository() {
        logger.info("Initializing PartitionConfigurationRepository");

        // Create default partition configuration
        createDefaultPartitionConfiguration();
        
        logger.info("Initialized PartitionConfigurationRepository with {} default configurations", configurations.size());
    }

    /**
     * Create default partition configuration.
     */
    private void createDefaultPartitionConfiguration() {
        // Default partition configuration
        PartitionConfiguration defaultConfig = new PartitionConfiguration();
        defaultConfig.setPartitionType("MONTHLY");
        defaultConfig.setMaxActivePartitions(3);
        defaultConfig.setAutoArchivePartitions(true);
        defaultConfig.setPartitionBaseDirectory("./lucene-index/partitions");
        defaultConfig.setPartitioningEnabled(true);
        
        save(defaultConfig);
        logger.debug("Created default partition configuration: {}", defaultConfig);
    }

    /**
     * Save a partition configuration.
     *
     * @param configuration The configuration to save
     * @return The saved configuration with a generated ID
     */
    public PartitionConfiguration save(PartitionConfiguration configuration) {
        if (configuration.getId() == null || configuration.getId().isEmpty()) {
            configuration.setId(UUID.randomUUID().toString());
        }
        configuration.setUpdatedAt(Instant.now());
        configurations.put(configuration.getId(), configuration);
        return configuration;
    }

    /**
     * Find a partition configuration by ID.
     *
     * @param id The ID of the configuration to find
     * @return The configuration, or null if not found
     */
    public PartitionConfiguration findById(String id) {
        return configurations.get(id);
    }

    /**
     * Find all partition configurations.
     *
     * @return A list of all configurations
     */
    public List<PartitionConfiguration> findAll() {
        return new ArrayList<>(configurations.values());
    }

    /**
     * Get the default partition configuration.
     * 
     * @return The default partition configuration
     */
    public PartitionConfiguration getDefaultConfiguration() {
        if (configurations.isEmpty()) {
            createDefaultPartitionConfiguration();
        }
        return configurations.values().iterator().next();
    }

    /**
     * Delete a partition configuration by ID.
     *
     * @param id The ID of the configuration to delete
     * @return true if the configuration was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return configurations.remove(id) != null;
    }

    /**
     * Delete all partition configurations.
     *
     * @return The number of configurations deleted
     */
    public int deleteAll() {
        int count = configurations.size();
        configurations.clear();
        return count;
    }

    /**
     * Get the total number of partition configurations.
     *
     * @return The total number of configurations
     */
    public int count() {
        return configurations.size();
    }
}