package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.controller.HttpLogController;
import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import io.github.ozkanpakdil.grepwise.repository.LogDirectoryConfigRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing log sources of different types (file, syslog, HTTP).
 * This service integrates with LogScannerService, SyslogServer, and HttpLogController
 * to provide a unified interface for managing log sources.
 */
@Service
public class LogSourceService {
    private static final Logger logger = LoggerFactory.getLogger(LogSourceService.class);

    private final LogScannerService logScannerService;
    private final SyslogServer syslogServer;
    private final HttpLogController httpLogController;
    private final CloudWatchLogService cloudWatchLogService;
    private final LogDirectoryConfigRepository legacyConfigRepository;
    private final LogIngestionCoordinatorService coordinatorService;

    private final Map<String, LogSourceConfig> sources = new ConcurrentHashMap<>();

    public LogSourceService(
            LogScannerService logScannerService,
            SyslogServer syslogServer,
            HttpLogController httpLogController,
            CloudWatchLogService cloudWatchLogService,
            LogDirectoryConfigRepository legacyConfigRepository,
            LogIngestionCoordinatorService coordinatorService) {
        this.logScannerService = logScannerService;
        this.syslogServer = syslogServer;
        this.httpLogController = httpLogController;
        this.cloudWatchLogService = cloudWatchLogService;
        this.legacyConfigRepository = legacyConfigRepository;
        this.coordinatorService = coordinatorService;
        logger.info("LogSourceService initialized with horizontal scaling support");
    }

    @PostConstruct
    public void init() {
        logger.info("LogSourceService started");

        // Load legacy file source configurations
        loadLegacyConfigurations();
    }

    @PreDestroy
    public void destroy() {
        logger.info("Shutting down LogSourceService");

        // Stop all sources
        stopAllSources();
    }

    /**
     * Load legacy file source configurations from the LogDirectoryConfigRepository.
     * This is for backward compatibility with existing file source configurations.
     */
    private void loadLegacyConfigurations() {
        logger.info("Loading legacy file source configurations");

        List<LogDirectoryConfig> legacyConfigs = legacyConfigRepository.findAll();
        logger.info("Found {} legacy file source configurations", legacyConfigs.size());

        for (LogDirectoryConfig legacyConfig : legacyConfigs) {
            LogSourceConfig sourceConfig = LogSourceConfig.fromLogDirectoryConfig(legacyConfig);
            sources.put(sourceConfig.getId(), sourceConfig);
            logger.info("Loaded legacy file source configuration: {}", sourceConfig.getId());

            // Start the source if it's enabled
            if (sourceConfig.isEnabled()) {
                startSource(sourceConfig);
            }
        }
    }

    /**
     * Get all log source configurations.
     * When horizontal scaling is enabled, only returns sources that should be processed by this instance.
     *
     * @return A list of log source configurations for this instance
     */
    public List<LogSourceConfig> getAllSources() {
        List<LogSourceConfig> allSources = new ArrayList<>(sources.values());

        // If horizontal scaling is enabled, filter sources for this instance
        if (coordinatorService.isHorizontalScalingEnabled()) {
            logger.debug("Horizontal scaling is enabled, filtering sources for instance {}",
                    coordinatorService.getInstanceId());
            return coordinatorService.filterSourcesForThisInstance(allSources);
        }

        return allSources;
    }

    /**
     * Get a log source configuration by ID.
     *
     * @param id The ID of the log source configuration
     * @return The log source configuration, or null if not found
     */
    public LogSourceConfig getSourceById(String id) {
        return sources.get(id);
    }

    /**
     * Get all log source configurations of a specific type.
     *
     * @param type The type of log source
     * @return A list of log source configurations of the specified type
     */
    public List<LogSourceConfig> getSourcesByType(LogSourceConfig.SourceType type) {
        return sources.values().stream()
                .filter(config -> config.getSourceType() == type)
                .toList();
    }

    /**
     * Create a new log source configuration.
     *
     * @param config The log source configuration to create
     * @return The created log source configuration
     */
    public LogSourceConfig createSource(LogSourceConfig config) {
        // Generate an ID if not provided
        if (config.getId() == null || config.getId().isEmpty()) {
            config.setId(UUID.randomUUID().toString());
        }

        // Save the configuration
        sources.put(config.getId(), config);
        logger.info("Created log source configuration: {}", config.getId());

        // If it's a file source, also save it to the legacy repository for backward compatibility
        if (config.getSourceType() == LogSourceConfig.SourceType.FILE) {
            LogDirectoryConfig legacyConfig = config.toLogDirectoryConfig();
            legacyConfigRepository.save(legacyConfig);
            logger.info("Saved file source configuration to legacy repository: {}", config.getId());
        }

        // Start the source if it's enabled
        if (config.isEnabled()) {
            startSource(config);
        }

        return config;
    }

    /**
     * Update an existing log source configuration.
     *
     * @param id     The ID of the log source configuration to update
     * @param config The updated log source configuration
     * @return The updated log source configuration, or null if not found
     */
    public LogSourceConfig updateSource(String id, LogSourceConfig config) {
        LogSourceConfig existingConfig = sources.get(id);
        if (existingConfig == null) {
            logger.warn("Cannot update non-existent log source configuration: {}", id);
            return null;
        }

        // Stop the source if it's running
        stopSource(existingConfig);

        // Update the configuration
        config.setId(id); // Ensure the ID is preserved
        sources.put(id, config);
        logger.info("Updated log source configuration: {}", id);

        // If it's a file source, also update it in the legacy repository for backward compatibility
        if (config.getSourceType() == LogSourceConfig.SourceType.FILE) {
            LogDirectoryConfig legacyConfig = config.toLogDirectoryConfig();
            legacyConfigRepository.save(legacyConfig);
            logger.info("Updated file source configuration in legacy repository: {}", id);
        } else if (existingConfig.getSourceType() == LogSourceConfig.SourceType.FILE) {
            // If the source type changed from FILE to something else, delete it from the legacy repository
            legacyConfigRepository.deleteById(id);
            logger.info("Deleted file source configuration from legacy repository: {}", id);
        }

        // Start the source if it's enabled
        if (config.isEnabled()) {
            startSource(config);
        }

        return config;
    }

    /**
     * Delete a log source configuration.
     *
     * @param id The ID of the log source configuration to delete
     * @return true if the configuration was deleted, false otherwise
     */
    public boolean deleteSource(String id) {
        LogSourceConfig config = sources.get(id);
        if (config == null) {
            logger.warn("Cannot delete non-existent log source configuration: {}", id);
            return false;
        }

        // Stop the source if it's running
        stopSource(config);

        // Remove the configuration
        sources.remove(id);
        logger.info("Deleted log source configuration: {}", id);

        // If it's a file source, also delete it from the legacy repository for backward compatibility
        if (config.getSourceType() == LogSourceConfig.SourceType.FILE) {
            legacyConfigRepository.deleteById(id);
            logger.info("Deleted file source configuration from legacy repository: {}", id);
        }

        return true;
    }

    /**
     * Start a log source.
     * When horizontal scaling is enabled, only starts the source if it should be processed by this instance.
     *
     * @param config The log source configuration to start
     * @return true if the source was started successfully, false otherwise
     */
    public boolean startSource(LogSourceConfig config) {
        if (!config.isEnabled()) {
            logger.warn("Cannot start disabled log source: {}", config.getId());
            return false;
        }

        // If horizontal scaling is enabled, check if this instance should process this source
        if (coordinatorService.isHorizontalScalingEnabled()) {
            if (!coordinatorService.shouldProcessSource(config.getId())) {
                logger.debug("Skipping log source {} as it's assigned to another instance (this instance: {})",
                        config.getId(), coordinatorService.getInstanceId());
                return true; // Return true as this is not an error condition
            }
            logger.info("Starting log source: {} on instance: {}",
                    config.getId(), coordinatorService.getInstanceId());
        } else {
            logger.info("Starting log source: {}", config.getId());
        }

        try {
            switch (config.getSourceType()) {
                case FILE:
                    // For file sources, we use the legacy LogScannerService
                    LogDirectoryConfig legacyConfig = config.toLogDirectoryConfig();
                    logScannerService.saveConfig(legacyConfig);
                    logScannerService.scanDirectory(legacyConfig);
                    logger.info("Started file log source: {}", config.getId());
                    return true;

                case SYSLOG:
                    // For syslog sources, we use the SyslogServer
                    boolean syslogStarted = syslogServer.startListener(config);
                    if (syslogStarted) {
                        logger.info("Started syslog log source: {}", config.getId());
                    } else {
                        logger.error("Failed to start syslog log source: {}", config.getId());
                    }
                    return syslogStarted;

                case HTTP:
                    // For HTTP sources, we register them with the HttpLogController
                    boolean httpRegistered = httpLogController.registerHttpSource(config);
                    if (httpRegistered) {
                        logger.info("Started HTTP log source: {}", config.getId());
                    } else {
                        logger.error("Failed to start HTTP log source: {}", config.getId());
                    }
                    return httpRegistered;

                case CLOUDWATCH:
                    // For CloudWatch sources, we register them with the CloudWatchLogService
                    boolean cloudWatchRegistered = cloudWatchLogService.registerSource(config);
                    if (cloudWatchRegistered) {
                        logger.info("Started CloudWatch log source: {}", config.getId());
                    } else {
                        logger.error("Failed to start CloudWatch log source: {}", config.getId());
                    }
                    return cloudWatchRegistered;

                default:
                    logger.error("Unsupported log source type: {}", config.getSourceType());
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error starting log source: {}", config.getId(), e);
            return false;
        }
    }

    /**
     * Stop a log source.
     *
     * @param config The log source configuration to stop
     * @return true if the source was stopped successfully, false otherwise
     */
    public boolean stopSource(LogSourceConfig config) {
        logger.info("Stopping log source: {}", config.getId());

        try {
            switch (config.getSourceType()) {
                case FILE:
                    // For file sources, do not delete persisted configuration on stop.
                    // Stopping is a runtime concern; configurations persist across restarts.
                    logger.info("Stopped file log source (configuration retained): {}", config.getId());
                    return true;

                case SYSLOG:
                    // For syslog sources, we stop the listener in the SyslogServer
                    boolean syslogStopped = syslogServer.stopListener(config.getId());
                    if (syslogStopped) {
                        logger.info("Stopped syslog log source: {}", config.getId());
                    } else {
                        logger.warn("Failed to stop syslog log source: {}", config.getId());
                    }
                    return syslogStopped;

                case HTTP:
                    // For HTTP sources, we unregister them from the HttpLogController
                    boolean httpUnregistered = httpLogController.unregisterHttpSource(config.getId());
                    if (httpUnregistered) {
                        logger.info("Stopped HTTP log source: {}", config.getId());
                    } else {
                        logger.warn("Failed to stop HTTP log source: {}", config.getId());
                    }
                    return httpUnregistered;

                case CLOUDWATCH:
                    // For CloudWatch sources, we unregister them from the CloudWatchLogService
                    boolean cloudWatchUnregistered = cloudWatchLogService.unregisterSource(config.getId());
                    if (cloudWatchUnregistered) {
                        logger.info("Stopped CloudWatch log source: {}", config.getId());
                    } else {
                        logger.warn("Failed to stop CloudWatch log source: {}", config.getId());
                    }
                    return cloudWatchUnregistered;

                default:
                    logger.error("Unsupported log source type: {}", config.getSourceType());
                    return false;
            }
        } catch (Exception e) {
            logger.error("Error stopping log source: {}", config.getId(), e);
            return false;
        }
    }

    /**
     * Stop all log sources.
     */
    public void stopAllSources() {
        logger.info("Stopping all log sources");

        for (LogSourceConfig config : sources.values()) {
            stopSource(config);
        }
    }

    /**
     * Get the number of active sources of each type.
     *
     * @return A map of source type to count
     */
    public Map<LogSourceConfig.SourceType, Integer> getActiveSourceCounts() {
        Map<LogSourceConfig.SourceType, Integer> counts = new ConcurrentHashMap<>();

        // Initialize counts
        for (LogSourceConfig.SourceType type : LogSourceConfig.SourceType.values()) {
            counts.put(type, 0);
        }

        // Count active sources
        for (LogSourceConfig config : sources.values()) {
            if (config.isEnabled()) {
                LogSourceConfig.SourceType type = config.getSourceType();
                counts.put(type, counts.get(type) + 1);
            }
        }

        return counts;
    }

    /**
     * Get the total number of active sources.
     *
     * @return The number of active sources
     */
    public int getTotalActiveSourceCount() {
        return (int) sources.values().stream()
                .filter(LogSourceConfig::isEnabled)
                .count();
    }
}