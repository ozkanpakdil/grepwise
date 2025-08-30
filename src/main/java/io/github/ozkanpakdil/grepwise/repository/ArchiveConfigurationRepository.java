package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.ArchiveConfiguration;
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
 * Repository for storing and retrieving archive configurations.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class ArchiveConfigurationRepository {
    private static final Logger logger = LoggerFactory.getLogger(ArchiveConfigurationRepository.class);
    private final Map<String, ArchiveConfiguration> configurations = new ConcurrentHashMap<>();

    /**
     * Constructor that initializes the repository with default archive configuration.
     */
    public ArchiveConfigurationRepository() {
        logger.info("Initializing ArchiveConfigurationRepository");

        // Create default archive configuration
        createDefaultArchiveConfiguration();

        logger.info("Initialized ArchiveConfigurationRepository with {} default configurations", configurations.size());
    }

    /**
     * Create default archive configuration.
     */
    private void createDefaultArchiveConfiguration() {
        // Default archive configuration
        ArchiveConfiguration defaultConfig = new ArchiveConfiguration();
        defaultConfig.setArchiveDirectory("./archives");
        defaultConfig.setAutoArchiveEnabled(true);
        defaultConfig.setCompressionLevel(5);
        defaultConfig.setMaxArchiveSizeMb(100);
        defaultConfig.setArchiveRetentionDays(90);

        save(defaultConfig);
        logger.debug("Created default archive configuration: {}", defaultConfig);
    }

    /**
     * Save an archive configuration.
     *
     * @param configuration The configuration to save
     * @return The saved configuration with a generated ID
     */
    public ArchiveConfiguration save(ArchiveConfiguration configuration) {
        if (configuration.getId() == null || configuration.getId().isEmpty()) {
            configuration.setId(UUID.randomUUID().toString());
        }
        configuration.setUpdatedAt(Instant.now());
        configurations.put(configuration.getId(), configuration);
        return configuration;
    }

    /**
     * Find an archive configuration by ID.
     *
     * @param id The ID of the configuration to find
     * @return The configuration, or null if not found
     */
    public ArchiveConfiguration findById(String id) {
        return configurations.get(id);
    }

    /**
     * Find all archive configurations.
     *
     * @return A list of all configurations
     */
    public List<ArchiveConfiguration> findAll() {
        return new ArrayList<>(configurations.values());
    }

    /**
     * Get the default archive configuration.
     *
     * @return The default archive configuration
     */
    public ArchiveConfiguration getDefaultConfiguration() {
        if (configurations.isEmpty()) {
            createDefaultArchiveConfiguration();
        }
        return configurations.values().iterator().next();
    }

    /**
     * Delete an archive configuration by ID.
     *
     * @param id The ID of the configuration to delete
     * @return true if the configuration was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return configurations.remove(id) != null;
    }

    /**
     * Delete all archive configurations.
     *
     * @return The number of configurations deleted
     */
    public int deleteAll() {
        int count = configurations.size();
        configurations.clear();
        return count;
    }

    /**
     * Get the total number of archive configurations.
     *
     * @return The total number of configurations
     */
    public int count() {
        return configurations.size();
    }
}