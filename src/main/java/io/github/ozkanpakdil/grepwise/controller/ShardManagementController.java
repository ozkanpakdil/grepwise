package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.ShardConfiguration;
import io.github.ozkanpakdil.grepwise.repository.ShardConfigurationRepository;
import io.github.ozkanpakdil.grepwise.service.ShardManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST controller for managing search sharding configuration and operations.
 */
@RestController
@RequestMapping("/api/shards")
public class ShardManagementController {
    private static final Logger logger = LoggerFactory.getLogger(ShardManagementController.class);

    @Autowired
    private ShardManagerService shardManagerService;

    @Autowired
    private ShardConfigurationRepository shardConfigurationRepository;

    /**
     * Get the current shard configuration
     */
    @GetMapping("/config")
    public ResponseEntity<ShardConfiguration> getConfiguration() {
        ShardConfiguration config = shardManagerService.getConfiguration();
        if (config == null) {
            config = shardConfigurationRepository.getDefaultConfiguration();
        }
        return ResponseEntity.ok(config);
    }

    /**
     * Update the shard configuration
     */
    @PutMapping("/config")
    public ResponseEntity<ShardConfiguration> updateConfiguration(@RequestBody ShardConfiguration config) {
        logger.info("Updating shard configuration: {}", config);

        // Save to repository
        ShardConfiguration savedConfig = shardConfigurationRepository.save(config);

        // Update service configuration
        shardManagerService.updateConfiguration(savedConfig);

        return ResponseEntity.ok(savedConfig);
    }

    /**
     * Get all shard configurations
     */
    @GetMapping("/configs")
    public ResponseEntity<List<ShardConfiguration>> getAllConfigurations() {
        return ResponseEntity.ok(shardConfigurationRepository.findAll());
    }

    /**
     * Enable or disable sharding
     */
    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enableSharding(@RequestParam boolean enabled) {
        logger.info("Setting sharding enabled: {}", enabled);

        shardManagerService.setShardingEnabled(enabled);

        Map<String, Object> response = new HashMap<>();
        response.put("shardingEnabled", shardManagerService.isShardingEnabled());
        return ResponseEntity.ok(response);
    }

    /**
     * Get all registered shard nodes
     */
    @GetMapping("/nodes")
    public ResponseEntity<Map<String, String>> getShardNodes() {
        return ResponseEntity.ok(shardManagerService.getShardNodes());
    }

    /**
     * Register a new shard node
     */
    @PostMapping("/nodes")
    public ResponseEntity<Map<String, Object>> registerShardNode(
            @RequestParam String nodeId,
            @RequestParam String nodeUrl) {

        logger.info("Registering shard node: {} at {}", nodeId, nodeUrl);

        shardManagerService.registerShardNode(nodeId, nodeUrl);

        Map<String, Object> response = new HashMap<>();
        response.put("nodeId", nodeId);
        response.put("nodeUrl", nodeUrl);
        response.put("registered", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Unregister a shard node
     */
    @DeleteMapping("/nodes/{nodeId}")
    public ResponseEntity<Map<String, Object>> unregisterShardNode(@PathVariable String nodeId) {
        logger.info("Unregistering shard node: {}", nodeId);

        shardManagerService.unregisterShardNode(nodeId);

        Map<String, Object> response = new HashMap<>();
        response.put("nodeId", nodeId);
        response.put("unregistered", true);
        return ResponseEntity.ok(response);
    }

    /**
     * Get shard statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getShardStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("shardingEnabled", shardManagerService.isShardingEnabled());
        stats.put("nodeCount", shardManagerService.getShardNodes().size());
        stats.put("configuration", shardManagerService.getConfiguration());
        return ResponseEntity.ok(stats);
    }
}