package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Repository for storing and retrieving log directory configurations.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class LogDirectoryConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(LogDirectoryConfigRepository.class);
    private final Map<String, LogDirectoryConfig> configs = new ConcurrentHashMap<>();

    /**
     * Constructor that initializes the repository with a default configuration.
     */
    public LogDirectoryConfigRepository() {
        logger.info("Initializing LogDirectoryConfigRepository");

        // Create a default configuration for the logs directory in the user's home directory
        String userHome = System.getProperty("user.home");
        logger.debug("User home directory: {}", userHome);

        String defaultLogDir = userHome + File.separator + "logs";
        logger.debug("Default log directory path: {}", defaultLogDir);

        // Create the directory if it doesn't exist
        File logDir = new File(defaultLogDir);
        if (!logDir.exists()) {
            logger.info("Default log directory does not exist, attempting to create it");
            boolean created = logDir.mkdirs();
            if (created) {
                logger.info("Created default log directory: {}", defaultLogDir);
            } else {
                logger.warn("Failed to create default log directory: {}", defaultLogDir);
                // Use the current directory as a fallback
                defaultLogDir = System.getProperty("user.dir");
                logger.info("Using current directory as fallback: {}", defaultLogDir);
            }
        } else {
            logger.info("Default log directory already exists: {}", defaultLogDir);
        }

        LogDirectoryConfig defaultConfig = new LogDirectoryConfig();
        defaultConfig.setDirectoryPath(defaultLogDir);
        defaultConfig.setEnabled(true);
        defaultConfig.setFilePattern("*.log");
        defaultConfig.setScanIntervalSeconds(60);

        save(defaultConfig);
        logger.info("Initialized default log directory configuration: {}", defaultConfig);
    }

    /**
     * Save a log directory configuration.
     *
     * @param config The configuration to save
     * @return The saved configuration with a generated ID
     */
    public LogDirectoryConfig save(LogDirectoryConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        configs.put(config.getId(), config);
        return config;
    }

    /**
     * Find a log directory configuration by ID.
     *
     * @param id The ID of the configuration to find
     * @return The configuration, or null if not found
     */
    public LogDirectoryConfig findById(String id) {
        return configs.get(id);
    }

    /**
     * Find all log directory configurations.
     *
     * @return A list of all configurations
     */
    public List<LogDirectoryConfig> findAll() {
        return new ArrayList<>(configs.values());
    }

    /**
     * Delete a log directory configuration by ID.
     *
     * @param id The ID of the configuration to delete
     * @return true if the configuration was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return configs.remove(id) != null;
    }

    /**
     * Delete all log directory configurations.
     *
     * @return The number of configurations deleted
     */
    public int deleteAll() {
        int count = configs.size();
        configs.clear();
        return count;
    }

    /**
     * Get the total number of log directory configurations.
     *
     * @return The total number of configurations
     */
    public int count() {
        return configs.size();
    }
}
