package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.FieldConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for storing and retrieving field configurations.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class FieldConfigurationRepository {
    private static final Logger logger = LoggerFactory.getLogger(FieldConfigurationRepository.class);
    private final Map<String, FieldConfiguration> configurations = new ConcurrentHashMap<>();

    /**
     * Constructor that initializes the repository with default field configurations.
     */
    public FieldConfigurationRepository() {
        logger.info("Initializing FieldConfigurationRepository");

        // Create default field configurations for common log fields
        createDefaultFieldConfigurations();
        
        logger.info("Initialized FieldConfigurationRepository with {} default configurations", configurations.size());
    }

    /**
     * Create default field configurations for common log fields.
     */
    private void createDefaultFieldConfigurations() {
        // Timestamp field configuration
        FieldConfiguration timestampConfig = new FieldConfiguration(
            "timestamp",
            "Log entry timestamp",
            FieldConfiguration.FieldType.DATE,
            null,
            "timestamp",
            true,
            true,
            false,
            true
        );
        save(timestampConfig);
        logger.debug("Created default timestamp field configuration: {}", timestampConfig);

        // Level field configuration
        FieldConfiguration levelConfig = new FieldConfiguration(
            "level",
            "Log level (INFO, WARN, ERROR, etc.)",
            FieldConfiguration.FieldType.STRING,
            null,
            "level",
            true,
            true,
            false,
            true
        );
        save(levelConfig);
        logger.debug("Created default level field configuration: {}", levelConfig);

        // Message field configuration
        FieldConfiguration messageConfig = new FieldConfiguration(
            "message",
            "Log message content",
            FieldConfiguration.FieldType.STRING,
            null,
            "message",
            true,
            true,
            true,
            true
        );
        save(messageConfig);
        logger.debug("Created default message field configuration: {}", messageConfig);

        // Source field configuration
        FieldConfiguration sourceConfig = new FieldConfiguration(
            "source",
            "Log source (file, application, etc.)",
            FieldConfiguration.FieldType.STRING,
            null,
            "source",
            true,
            true,
            false,
            true
        );
        save(sourceConfig);
        logger.debug("Created default source field configuration: {}", sourceConfig);

        // IP Address field configuration (example of a custom extracted field)
        FieldConfiguration ipAddressConfig = new FieldConfiguration(
            "ip_address",
            "IP address extracted from log message",
            FieldConfiguration.FieldType.STRING,
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b",
            "message",
            true,
            true,
            false,
            true
        );
        save(ipAddressConfig);
        logger.debug("Created default IP address field configuration: {}", ipAddressConfig);
    }

    /**
     * Save a field configuration.
     *
     * @param configuration The configuration to save
     * @return The saved configuration with a generated ID
     */
    public FieldConfiguration save(FieldConfiguration configuration) {
        if (configuration.getId() == null || configuration.getId().isEmpty()) {
            configuration.setId(UUID.randomUUID().toString());
        }
        configurations.put(configuration.getId(), configuration);
        return configuration;
    }

    /**
     * Find a field configuration by ID.
     *
     * @param id The ID of the configuration to find
     * @return The configuration, or null if not found
     */
    public FieldConfiguration findById(String id) {
        return configurations.get(id);
    }

    /**
     * Find a field configuration by name.
     *
     * @param name The name of the configuration to find
     * @return The configuration, or null if not found
     */
    public FieldConfiguration findByName(String name) {
        return configurations.values().stream()
                .filter(config -> config.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find all field configurations.
     *
     * @return A list of all configurations
     */
    public List<FieldConfiguration> findAll() {
        return new ArrayList<>(configurations.values());
    }

    /**
     * Find all enabled field configurations.
     *
     * @return A list of all enabled configurations
     */
    public List<FieldConfiguration> findAllEnabled() {
        return configurations.values().stream()
                .filter(FieldConfiguration::isEnabled)
                .toList();
    }

    /**
     * Delete a field configuration by ID.
     *
     * @param id The ID of the configuration to delete
     * @return true if the configuration was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return configurations.remove(id) != null;
    }

    /**
     * Delete all field configurations.
     *
     * @return The number of configurations deleted
     */
    public int deleteAll() {
        int count = configurations.size();
        configurations.clear();
        return count;
    }

    /**
     * Get the total number of field configurations.
     *
     * @return The total number of configurations
     */
    public int count() {
        return configurations.size();
    }
}