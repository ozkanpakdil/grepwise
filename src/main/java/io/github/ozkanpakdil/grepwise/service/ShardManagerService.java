package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.ShardConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Service for managing distributed search across multiple shards.
 * This service coordinates search queries across multiple nodes and merges results.
 */
@Service
public class ShardManagerService {
    private static final Logger logger = LoggerFactory.getLogger(ShardManagerService.class);

    @Autowired
    private LuceneService luceneService;

    @Autowired
    private SearchCacheService searchCacheService;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ExecutorService executorService = Executors.newFixedThreadPool(10);

    @Value("${grepwise.sharding.enabled:false}")
    private boolean shardingEnabled;

    @Value("${grepwise.sharding.local-node-id:node1}")
    private String localNodeId;

    @Value("${grepwise.sharding.local-node-url:http://localhost:8080}")
    private String localNodeUrl;

    private ShardConfiguration shardConfiguration;
    private Map<String, String> shardNodeMap = new ConcurrentHashMap<>();
    private boolean isInitialized = false;

    @PostConstruct
    public void init() {
        logger.info("Initializing ShardManagerService with shardingEnabled={}", shardingEnabled);
        if (shardingEnabled) {
            // Initialize with default configuration
            shardConfiguration = new ShardConfiguration();
            shardConfiguration.setShardingEnabled(true);
            shardConfiguration.getShardNodes().add(localNodeUrl);
            
            // Register this node
            shardNodeMap.put(localNodeId, localNodeUrl);
            isInitialized = true;
            
            logger.info("ShardManagerService initialized with local node: {}", localNodeId);
        }
    }

    /**
     * Update the shard configuration
     */
    public void updateConfiguration(ShardConfiguration config) {
        this.shardConfiguration = config;
        this.shardingEnabled = config.isShardingEnabled();
        
        // Update shard node map
        shardNodeMap.clear();
        shardNodeMap.put(localNodeId, localNodeUrl);
        
        if (config.getShardNodes() != null) {
            for (int i = 0; i < config.getShardNodes().size(); i++) {
                String nodeUrl = config.getShardNodes().get(i);
                if (!nodeUrl.equals(localNodeUrl)) {
                    shardNodeMap.put("node" + (i + 2), nodeUrl);
                }
            }
        }
        
        logger.info("ShardConfiguration updated: {}", config);
        logger.info("Shard node map: {}", shardNodeMap);
    }

    /**
     * Get the current shard configuration
     */
    public ShardConfiguration getConfiguration() {
        return shardConfiguration;
    }

    /**
     * Determine which shards should handle a search query based on the sharding type
     */
    private List<String> determineTargetShards(String queryStr, boolean isRegex, Long startTime, Long endTime) {
        if (!shardingEnabled || shardNodeMap.isEmpty()) {
            return Collections.singletonList(localNodeId);
        }

        String shardingType = shardConfiguration.getShardingType();
        
        switch (shardingType) {
            case "TIME_BASED":
                // For time-based sharding, determine shards based on time range
                return determineTimeBasedShards(startTime, endTime);
            case "SOURCE_BASED":
                // For source-based sharding, determine shards based on source pattern in query
                return determineSourceBasedShards(queryStr);
            case "BALANCED":
            default:
                // For balanced sharding, distribute to all shards
                return new ArrayList<>(shardNodeMap.keySet());
        }
    }

    /**
     * Determine which shards should handle a time-based query
     */
    private List<String> determineTimeBasedShards(Long startTime, Long endTime) {
        // If no time range specified, query all shards
        if (startTime == null || endTime == null) {
            return new ArrayList<>(shardNodeMap.keySet());
        }

        // Simple implementation: distribute by time range
        // In a real implementation, this would be based on time ranges assigned to each shard
        int numberOfShards = shardConfiguration.getNumberOfShards();
        List<String> nodeIds = new ArrayList<>(shardNodeMap.keySet());
        
        // Ensure we don't exceed available nodes
        int actualShards = Math.min(numberOfShards, nodeIds.size());
        return nodeIds.subList(0, actualShards);
    }

    /**
     * Determine which shards should handle a source-based query
     */
    private List<String> determineSourceBasedShards(String queryStr) {
        // Simple implementation: check if query contains source information
        // In a real implementation, this would be based on sources assigned to each shard
        if (queryStr == null || queryStr.isEmpty()) {
            return new ArrayList<>(shardNodeMap.keySet());
        }

        // Check if query contains source information
        if (queryStr.contains("source:")) {
            // Extract source from query and determine appropriate shard
            // This is a simplified example
            int sourceIndex = queryStr.indexOf("source:");
            int endIndex = queryStr.indexOf(" ", sourceIndex);
            if (endIndex == -1) {
                endIndex = queryStr.length();
            }
            
            String sourceValue = queryStr.substring(sourceIndex + 7, endIndex);
            
            // Simple hash-based routing
            int nodeIndex = Math.abs(sourceValue.hashCode() % shardNodeMap.size());
            List<String> nodeIds = new ArrayList<>(shardNodeMap.keySet());
            return Collections.singletonList(nodeIds.get(nodeIndex));
        }
        
        // If no source specified, query all shards
        return new ArrayList<>(shardNodeMap.keySet());
    }

    /**
     * Execute a distributed search across multiple shards
     */
    public List<LogEntry> distributedSearch(String queryStr, boolean isRegex, Long startTime, Long endTime) throws IOException {
        if (!shardingEnabled || !isInitialized) {
            // If sharding is not enabled, use local search only
            return luceneService.search(queryStr, isRegex, startTime, endTime);
        }

        // Check cache first
        List<LogEntry> cachedResults = searchCacheService.getFromCache(queryStr, isRegex, startTime, endTime);
        if (cachedResults != null) {
            logger.debug("Returning cached results for distributed search");
            return cachedResults;
        }

        // Determine which shards should handle this query
        List<String> targetShards = determineTargetShards(queryStr, isRegex, startTime, endTime);
        logger.debug("Distributed search targeting shards: {}", targetShards);

        // Execute search on all target shards in parallel
        List<Future<List<LogEntry>>> futures = new ArrayList<>();
        
        for (String nodeId : targetShards) {
            futures.add(executorService.submit(() -> {
                if (nodeId.equals(localNodeId)) {
                    // Execute search locally
                    return luceneService.search(queryStr, isRegex, startTime, endTime);
                } else {
                    // Execute search on remote node
                    String nodeUrl = shardNodeMap.get(nodeId);
                    if (nodeUrl != null) {
                        return executeRemoteSearch(nodeUrl, queryStr, isRegex, startTime, endTime);
                    }
                    return Collections.emptyList();
                }
            }));
        }

        // Collect and merge results
        List<LogEntry> mergedResults = new ArrayList<>();
        for (Future<List<LogEntry>> future : futures) {
            try {
                List<LogEntry> shardResults = future.get(10, TimeUnit.SECONDS);
                mergedResults.addAll(shardResults);
            } catch (InterruptedException | ExecutionException | TimeoutException e) {
                logger.error("Error executing distributed search", e);
            }
        }

        // Sort results by timestamp
        mergedResults.sort(Comparator.comparing(LogEntry::timestamp).reversed());

        // Add results to cache
        searchCacheService.addToCache(queryStr, isRegex, startTime, endTime, mergedResults);

        return mergedResults;
    }

    /**
     * Execute a search on a remote node
     */
    private List<LogEntry> executeRemoteSearch(String nodeUrl, String queryStr, boolean isRegex, Long startTime, Long endTime) {
        try {
            // Build URL with query parameters
            StringBuilder urlBuilder = new StringBuilder(nodeUrl);
            urlBuilder.append("/api/logs/search?");
            
            if (queryStr != null && !queryStr.isEmpty()) {
                urlBuilder.append("query=").append(queryStr).append("&");
            }
            
            urlBuilder.append("isRegex=").append(isRegex);
            
            if (startTime != null) {
                urlBuilder.append("&startTime=").append(startTime);
            }
            
            if (endTime != null) {
                urlBuilder.append("&endTime=").append(endTime);
            }
            
            // Add parameter to indicate this is a shard request to prevent infinite loops
            urlBuilder.append("&isShardRequest=true");
            
            String url = urlBuilder.toString();
            logger.debug("Executing remote search: {}", url);
            
            // Execute remote request
            ResponseEntity<LogEntry[]> response = restTemplate.getForEntity(url, LogEntry[].class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            logger.error("Error executing remote search on node: {}", nodeUrl, e);
        }
        
        return Collections.emptyList();
    }

    /**
     * Register a new shard node
     */
    public void registerShardNode(String nodeId, String nodeUrl) {
        if (!shardingEnabled) {
            return;
        }
        
        shardNodeMap.put(nodeId, nodeUrl);
        
        // Update configuration
        if (shardConfiguration != null) {
            List<String> nodes = shardConfiguration.getShardNodes();
            if (!nodes.contains(nodeUrl)) {
                nodes.add(nodeUrl);
                shardConfiguration.setShardNodes(nodes);
            }
        }
        
        logger.info("Registered shard node: {} at {}", nodeId, nodeUrl);
    }

    /**
     * Unregister a shard node
     */
    public void unregisterShardNode(String nodeId) {
        if (!shardingEnabled) {
            return;
        }
        
        String nodeUrl = shardNodeMap.remove(nodeId);
        
        // Update configuration
        if (nodeUrl != null && shardConfiguration != null) {
            List<String> nodes = shardConfiguration.getShardNodes();
            nodes.remove(nodeUrl);
            shardConfiguration.setShardNodes(nodes);
        }
        
        logger.info("Unregistered shard node: {}", nodeId);
    }

    /**
     * Get all registered shard nodes
     */
    public Map<String, String> getShardNodes() {
        return new HashMap<>(shardNodeMap);
    }

    /**
     * Check if sharding is enabled
     */
    public boolean isShardingEnabled() {
        return shardingEnabled;
    }

    /**
     * Enable or disable sharding
     */
    public void setShardingEnabled(boolean shardingEnabled) {
        this.shardingEnabled = shardingEnabled;
        
        if (shardConfiguration != null) {
            shardConfiguration.setShardingEnabled(shardingEnabled);
        }
        
        logger.info("Sharding {}", shardingEnabled ? "enabled" : "disabled");
    }
}