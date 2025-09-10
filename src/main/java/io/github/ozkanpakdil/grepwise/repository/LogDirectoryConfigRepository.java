package io.github.ozkanpakdil.grepwise.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import static io.github.ozkanpakdil.grepwise.GrepWiseApplication.CONFIG_DIR;

/**
 * Repository for storing and retrieving log directory configurations.
 * Persists configurations to a simple json file for durability across restarts.
 */
@Repository
public class LogDirectoryConfigRepository {
    private static final Logger logger = LoggerFactory.getLogger(LogDirectoryConfigRepository.class);
    private static final String LOG_SOURCES_CONFIG_FILE = CONFIG_DIR + File.separator + "log-sources.json";
    private static final String KEY_LOG_DIR_CONFIGS = "logDirectoryConfigs"; // JSON array value

    private final Map<String, LogDirectoryConfig> configs = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Constructor that initializes the repository, loading from json if available
     * and creating a sensible default otherwise.
     */
    public LogDirectoryConfigRepository() {
        logger.info("Initializing LogDirectoryConfigRepository");
        loadFromJson();
    }

    /**
     * Save a log directory configuration.
     *
     * @param config The configuration to save
     * @return The saved configuration with a generated ID
     */
    public synchronized LogDirectoryConfig save(LogDirectoryConfig config) {
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }
        configs.put(config.getId(), config);
        persistToJson();
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
    public synchronized boolean deleteById(String id) {
        boolean removed = configs.remove(id) != null;
        if (removed) {
            persistToJson();
        }
        return removed;
    }

    /**
     * Delete all log directory configurations.
     *
     * @return The number of configurations deleted
     */
    public synchronized int deleteAll() {
        int count = configs.size();
        configs.clear();
        persistToJson();
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

    private void loadFromJson() {
        try {
            File file = new File(LOG_SOURCES_CONFIG_FILE);
            if (!file.exists()) {
                logger.info("JSON settings file not found: {}", LOG_SOURCES_CONFIG_FILE);
                return;
            }
            Map<String, Object> root = objectMapper.readValue(file, Map.class);
            Object arr = root.get(KEY_LOG_DIR_CONFIGS);
            if (arr == null) {
                logger.info("No '{}' key in JSON settings; initializing defaults", KEY_LOG_DIR_CONFIGS);
                return;
            }
            List<LogDirectoryConfig> list = objectMapper.convertValue(arr, new TypeReference<>() {
            });
            configs.clear();
            for (LogDirectoryConfig c : list) {
                if (c.getId() == null || c.getId().isBlank()) {
                    c.setId(UUID.randomUUID().toString());
                }
                configs.put(c.getId(), c);
            }
            logger.info("Loaded {} log directory configs from {}", configs.size(), LOG_SOURCES_CONFIG_FILE);
        } catch (Exception e) {
            logger.error("Failed to load log directory configs from JSON", e);
        }
    }

    private void persistToJson() {
        try {
            File dir = new File(CONFIG_DIR);
            if (!dir.exists() && !dir.mkdirs()) {
                logger.warn("Could not create config directory at {}", dir.getAbsolutePath());
            }
            File file = new File(LOG_SOURCES_CONFIG_FILE);
            Map<String, Object> root = Map.of(
                    KEY_LOG_DIR_CONFIGS, new ArrayList<>(configs.values())
            );
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, root);
            logger.info("Persisted {} log directory configs to {}", configs.size(), file.getAbsolutePath());
        } catch (Exception e) {
            logger.error("Failed to persist log directory configs to JSON", e);
        }
    }
}
