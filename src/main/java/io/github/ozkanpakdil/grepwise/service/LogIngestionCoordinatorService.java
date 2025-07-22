package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Service responsible for coordinating log ingestion across multiple instances.
 * This service enables horizontal scaling of log ingestion by:
 * 1. Generating a unique instance ID for each instance
 * 2. Partitioning log sources across instances using consistent hashing
 * 3. Providing a heartbeat mechanism for failure detection
 * 4. Offering configuration options for enabling/disabling horizontal scaling
 */
@Service
public class LogIngestionCoordinatorService {

    private static final Logger logger = LoggerFactory.getLogger(LogIngestionCoordinatorService.class);
    
    private final String instanceId;
    private final Map<String, Long> instanceHeartbeats = new ConcurrentHashMap<>();
    private final AtomicBoolean horizontalScalingEnabled = new AtomicBoolean(false);
    
    @Value("${grepwise.horizontal-scaling.enabled:false}")
    private boolean scalingEnabledConfig;
    
    @Value("${grepwise.horizontal-scaling.heartbeat-timeout-ms:30000}")
    private long heartbeatTimeoutMs;
    
    @Value("${grepwise.horizontal-scaling.instance-id:}")
    private String configuredInstanceId;

    /**
     * Constructor that generates a unique instance ID.
     */
    public LogIngestionCoordinatorService() {
        // Generate a unique instance ID based on hostname and a random UUID
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
            logger.warn("Unable to determine hostname", e);
        }
        
        // Use configured instance ID if provided, otherwise generate one
        if (configuredInstanceId != null && !configuredInstanceId.isEmpty()) {
            this.instanceId = configuredInstanceId;
        } else {
            this.instanceId = hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        logger.info("Log ingestion coordinator initialized with instance ID: {}", instanceId);
    }
    
    @PostConstruct
    public void init() {
        // Initialize the service
        horizontalScalingEnabled.set(scalingEnabledConfig);
        
        if (horizontalScalingEnabled.get()) {
            logger.info("Horizontal scaling for log ingestion is enabled");
            // Register this instance
            registerInstance(instanceId);
        } else {
            logger.info("Horizontal scaling for log ingestion is disabled");
        }
    }
    
    @PreDestroy
    public void destroy() {
        if (horizontalScalingEnabled.get()) {
            // Unregister this instance
            unregisterInstance(instanceId);
            logger.info("Instance {} unregistered", instanceId);
        }
    }
    
    /**
     * Register an instance with the coordinator.
     * 
     * @param instanceId The ID of the instance to register
     */
    public void registerInstance(String instanceId) {
        instanceHeartbeats.put(instanceId, System.currentTimeMillis());
        logger.info("Instance {} registered", instanceId);
    }
    
    /**
     * Unregister an instance from the coordinator.
     * 
     * @param instanceId The ID of the instance to unregister
     */
    public void unregisterInstance(String instanceId) {
        instanceHeartbeats.remove(instanceId);
        logger.info("Instance {} unregistered", instanceId);
    }
    
    /**
     * Update the heartbeat for this instance.
     */
    @Scheduled(fixedRateString = "${grepwise.horizontal-scaling.heartbeat-interval-ms:10000}")
    public void updateHeartbeat() {
        if (horizontalScalingEnabled.get()) {
            instanceHeartbeats.put(instanceId, System.currentTimeMillis());
            logger.debug("Updated heartbeat for instance {}", instanceId);
            
            // Clean up stale instances
            cleanupStaleInstances();
        }
    }
    
    /**
     * Clean up instances that haven't sent a heartbeat in a while.
     */
    private void cleanupStaleInstances() {
        long now = System.currentTimeMillis();
        List<String> staleInstances = instanceHeartbeats.entrySet().stream()
                .filter(entry -> (now - entry.getValue()) > heartbeatTimeoutMs)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        for (String staleInstance : staleInstances) {
            instanceHeartbeats.remove(staleInstance);
            logger.info("Removed stale instance: {}", staleInstance);
        }
    }
    
    /**
     * Check if a log source should be processed by this instance.
     * Uses consistent hashing to distribute sources across instances.
     * 
     * @param sourceId The ID of the log source
     * @return true if this instance should process the source, false otherwise
     */
    public boolean shouldProcessSource(String sourceId) {
        if (!horizontalScalingEnabled.get()) {
            // If horizontal scaling is disabled, process all sources
            return true;
        }
        
        // Get active instances
        List<String> activeInstances = new ArrayList<>(instanceHeartbeats.keySet());
        if (activeInstances.isEmpty()) {
            // If there are no active instances, process all sources
            return true;
        }
        
        // Sort instances for consistent results
        Collections.sort(activeInstances);
        
        // Use consistent hashing to determine which instance should process this source
        int hash = Math.abs(sourceId.hashCode());
        int instanceIndex = hash % activeInstances.size();
        String assignedInstance = activeInstances.get(instanceIndex);
        
        boolean shouldProcess = assignedInstance.equals(instanceId);
        if (shouldProcess) {
            logger.debug("Instance {} assigned to process source {}", instanceId, sourceId);
        }
        
        return shouldProcess;
    }
    
    /**
     * Filter a list of log sources to only include those that should be processed by this instance.
     * 
     * @param sources The list of log sources
     * @return A filtered list of log sources
     */
    public List<LogSourceConfig> filterSourcesForThisInstance(List<LogSourceConfig> sources) {
        if (!horizontalScalingEnabled.get()) {
            // If horizontal scaling is disabled, process all sources
            return sources;
        }
        
        return sources.stream()
                .filter(source -> shouldProcessSource(source.getId()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get the current instance ID.
     * 
     * @return The instance ID
     */
    public String getInstanceId() {
        return instanceId;
    }
    
    /**
     * Check if horizontal scaling is enabled.
     * 
     * @return true if horizontal scaling is enabled, false otherwise
     */
    public boolean isHorizontalScalingEnabled() {
        return horizontalScalingEnabled.get();
    }
    
    /**
     * Enable or disable horizontal scaling.
     * 
     * @param enabled true to enable horizontal scaling, false to disable
     */
    public void setHorizontalScalingEnabled(boolean enabled) {
        boolean wasEnabled = horizontalScalingEnabled.getAndSet(enabled);
        
        if (enabled && !wasEnabled) {
            // If we're enabling horizontal scaling, register this instance
            registerInstance(instanceId);
            logger.info("Horizontal scaling for log ingestion enabled");
        } else if (!enabled && wasEnabled) {
            // If we're disabling horizontal scaling, unregister this instance
            unregisterInstance(instanceId);
            logger.info("Horizontal scaling for log ingestion disabled");
        }
    }
    
    /**
     * Get the list of active instances.
     * 
     * @return The list of active instances
     */
    public List<String> getActiveInstances() {
        return new ArrayList<>(instanceHeartbeats.keySet());
    }
    
    /**
     * Get the number of active instances.
     * 
     * @return The number of active instances
     */
    public int getActiveInstanceCount() {
        return instanceHeartbeats.size();
    }
}