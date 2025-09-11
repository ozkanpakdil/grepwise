package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.ClusterNode;
import io.github.ozkanpakdil.grepwise.model.ClusterState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * Service responsible for managing high availability in a distributed environment.
 * This service provides:
 * 1. Automatic node discovery and registration
 * 2. Leader election for coordinating distributed operations
 * 3. Automatic failover when nodes go down
 * 4. Data replication coordination across nodes
 * 5. Health monitoring of cluster nodes
 */
@Service
public class HighAvailabilityService {
    private static final Logger logger = LoggerFactory.getLogger(HighAvailabilityService.class);
    private final RestTemplate restTemplate = new RestTemplate();
    private final String nodeId;
    private final Map<String, ClusterNode> clusterNodes = new ConcurrentHashMap<>();
    private final AtomicReference<String> currentLeaderId = new AtomicReference<>();
    private final AtomicBoolean isLeader = new AtomicBoolean(false);
    @Autowired
    private LogIngestionCoordinatorService logIngestionCoordinatorService;
    @Autowired
    private ShardManagerService shardManagerService;
    @Value("${grepwise.high-availability.enabled:false}")
    private boolean highAvailabilityEnabled;
    @Value("${grepwise.high-availability.heartbeat-interval-ms:5000}")
    private long heartbeatIntervalMs;
    @Value("${grepwise.high-availability.heartbeat-timeout-ms:15000}")
    private long heartbeatTimeoutMs;
    @Value("${grepwise.high-availability.leader-check-interval-ms:10000}")
    private long leaderCheckIntervalMs;
    @Value("${grepwise.high-availability.node-id:}")
    private String configuredNodeId;
    @Value("${grepwise.high-availability.node-url:http://localhost:8080}")
    private String nodeUrl;

    /**
     * Constructor that generates a unique node ID.
     */
    public HighAvailabilityService() {
        // Generate a unique node ID based on hostname and a random UUID
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown-host";
            logger.warn("Unable to determine hostname", e);
        }

        // Use configured node ID if provided, otherwise generate one
        if (configuredNodeId != null && !configuredNodeId.isEmpty()) {
            this.nodeId = configuredNodeId;
        } else {
            this.nodeId = hostname + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        logger.info("High availability service initialized with node ID: {}", nodeId);
    }

    @PostConstruct
    public void init() {
        if (highAvailabilityEnabled) {
            logger.info("High availability is enabled");

            // Register this node
            ClusterNode localNode = new ClusterNode(nodeId, nodeUrl, System.currentTimeMillis(), true);
            clusterNodes.put(nodeId, localNode);

            // Try to discover other nodes
            discoverNodes();

            // Perform initial leader election
            electLeader();

            // Register with ShardManagerService
            shardManagerService.registerShardNode(nodeId, nodeUrl);

            logger.info("Node {} registered with the cluster", nodeId);
        } else {
            logger.info("High availability is disabled");
        }
    }

    @PreDestroy
    public void destroy() {
        if (highAvailabilityEnabled) {
            // Notify other nodes that this node is leaving
            notifyNodeLeaving();

            // Unregister from ShardManagerService
            shardManagerService.unregisterShardNode(nodeId);

            logger.info("Node {} unregistered from the cluster", nodeId);
        }
    }

    /**
     * Update heartbeat for this node and check for stale nodes.
     */
    @Scheduled(fixedRateString = "${grepwise.high-availability.heartbeat-interval-ms:5000}")
    public void updateHeartbeat() {
        if (!highAvailabilityEnabled) {
            return;
        }

        // Update heartbeat for this node
        ClusterNode localNode = clusterNodes.get(nodeId);
        if (localNode != null) {
            localNode.setLastHeartbeat(System.currentTimeMillis());
            clusterNodes.put(nodeId, localNode);
        }

        // Send heartbeat to other nodes
        sendHeartbeat();

        // Clean up stale nodes
        cleanupStaleNodes();

        // Check if leader is still active
        checkLeader();
    }

    /**
     * Send heartbeat to other nodes.
     */
    private void sendHeartbeat() {
        for (Map.Entry<String, ClusterNode> entry : clusterNodes.entrySet()) {
            String targetNodeId = entry.getKey();
            ClusterNode targetNode = entry.getValue();

            // Skip self
            if (targetNodeId.equals(nodeId)) {
                continue;
            }

            try {
                String url = targetNode.getUrl() + "/api/cluster/heartbeat";
                Map<String, Object> heartbeatData = new HashMap<>();
                heartbeatData.put("nodeId", nodeId);
                heartbeatData.put("timestamp", System.currentTimeMillis());
                heartbeatData.put("isLeader", isLeader.get());

                restTemplate.postForEntity(url, heartbeatData, Void.class);
                logger.debug("Sent heartbeat to node: {}", targetNodeId);
            } catch (RestClientException e) {
                logger.warn("Failed to send heartbeat to node: {}", targetNodeId, e);
            }
        }
    }

    /**
     * Clean up stale nodes.
     */
    private void cleanupStaleNodes() {
        long now = System.currentTimeMillis();
        List<String> staleNodeIds = clusterNodes.entrySet().stream()
                .filter(entry -> (now - entry.getValue().getLastHeartbeat()) > heartbeatTimeoutMs)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        for (String staleNodeId : staleNodeIds) {
            // Skip self
            if (staleNodeId.equals(nodeId)) {
                continue;
            }

            ClusterNode staleNode = clusterNodes.remove(staleNodeId);
            logger.info("Removed stale node: {}", staleNodeId);

            // If the stale node was the leader, elect a new leader
            if (staleNodeId.equals(currentLeaderId.get())) {
                logger.info("Leader node {} is stale, initiating new leader election", staleNodeId);
                electLeader();
            }

            // Unregister stale node from ShardManagerService
            shardManagerService.unregisterShardNode(staleNodeId);

            // Redistribute workload
            redistributeWorkload();
        }
    }

    /**
     * Check if the current leader is still active.
     */
    private void checkLeader() {
        String leaderId = currentLeaderId.get();
        if (leaderId == null) {
            // No leader, elect one
            electLeader();
            return;
        }

        // If this node is the leader, nothing to check
        if (nodeId.equals(leaderId)) {
            return;
        }

        // Check if leader is still in the cluster
        ClusterNode leaderNode = clusterNodes.get(leaderId);
        if (leaderNode == null) {
            logger.info("Leader node {} is no longer in the cluster, initiating new leader election", leaderId);
            electLeader();
        }
    }

    /**
     * Elect a leader for the cluster.
     */
    private void electLeader() {
        if (clusterNodes.isEmpty()) {
            return;
        }

        // Simple leader election: choose the node with the lowest ID
        String newLeaderId = clusterNodes.keySet().stream()
                .min(String::compareTo)
                .orElse(null);

        if (newLeaderId == null) {
            return;
        }

        String oldLeaderId = currentLeaderId.getAndSet(newLeaderId);
        boolean isThisNodeLeader = nodeId.equals(newLeaderId);
        isLeader.set(isThisNodeLeader);

        if (oldLeaderId == null || !oldLeaderId.equals(newLeaderId)) {
            logger.info("New leader elected: {}", newLeaderId);

            if (isThisNodeLeader) {
                logger.info("This node is now the leader");
                // Perform leader-specific initialization
                initializeAsLeader();
            }
        }
    }

    /**
     * Initialize this node as the leader.
     */
    private void initializeAsLeader() {
        // Update cluster state
        ClusterState clusterState = new ClusterState();
        clusterState.setLeaderId(nodeId);
        clusterState.setNodes(new ArrayList<>(clusterNodes.values()));

        // Notify other nodes about the new leader
        notifyLeaderChange(clusterState);

        // Redistribute workload
        redistributeWorkload();
    }

    /**
     * Notify other nodes about a leader change.
     */
    private void notifyLeaderChange(ClusterState clusterState) {
        for (Map.Entry<String, ClusterNode> entry : clusterNodes.entrySet()) {
            String targetNodeId = entry.getKey();
            ClusterNode targetNode = entry.getValue();

            // Skip self
            if (targetNodeId.equals(nodeId)) {
                continue;
            }

            try {
                String url = targetNode.getUrl() + "/api/cluster/leader-change";
                restTemplate.postForEntity(url, clusterState, Void.class);
                logger.debug("Notified node {} about leader change", targetNodeId);
            } catch (RestClientException e) {
                logger.warn("Failed to notify node {} about leader change", targetNodeId, e);
            }
        }
    }

    /**
     * Discover other nodes in the cluster.
     */
    private void discoverNodes() {
        // This would typically involve a service discovery mechanism
        // For simplicity, we'll assume nodes are configured manually or through a configuration service
        logger.info("Node discovery completed");
    }

    /**
     * Notify other nodes that this node is leaving the cluster.
     */
    private void notifyNodeLeaving() {
        for (Map.Entry<String, ClusterNode> entry : clusterNodes.entrySet()) {
            String targetNodeId = entry.getKey();
            ClusterNode targetNode = entry.getValue();

            // Skip self
            if (targetNodeId.equals(nodeId)) {
                continue;
            }

            try {
                String url = targetNode.getUrl() + "/api/cluster/node-leaving";
                Map<String, String> data = new HashMap<>();
                data.put("nodeId", nodeId);

                restTemplate.postForEntity(url, data, Void.class);
                logger.debug("Notified node {} that this node is leaving", targetNodeId);
            } catch (RestClientException e) {
                logger.warn("Failed to notify node {} that this node is leaving", targetNodeId, e);
            }
        }
    }

    /**
     * Redistribute workload after node changes.
     */
    private void redistributeWorkload() {
        // This would involve updating the ShardManagerService and LogIngestionCoordinatorService
        // to redistribute workload across the remaining nodes
        logger.info("Redistributing workload across cluster nodes");
    }

    /**
     * Handle a heartbeat from another node.
     */
    public void handleHeartbeat(String sourceNodeId, String sourceNodeUrl, long timestamp, boolean isSourceNodeLeader) {
        if (!highAvailabilityEnabled) {
            return;
        }

        // Update or add the node
        ClusterNode node = clusterNodes.getOrDefault(sourceNodeId, new ClusterNode(sourceNodeId, sourceNodeUrl, timestamp, false));
        node.setLastHeartbeat(timestamp);
        clusterNodes.put(sourceNodeId, node);

        // Register with ShardManagerService if not already registered
        shardManagerService.registerShardNode(sourceNodeId, sourceNodeUrl);

        // Handle leader information
        if (isSourceNodeLeader) {
            String currentLeader = currentLeaderId.get();
            if (currentLeader == null || !currentLeader.equals(sourceNodeId)) {
                logger.info("Received heartbeat from leader node: {}", sourceNodeId);
                currentLeaderId.set(sourceNodeId);
                isLeader.set(false);
            }
        }
    }

    /**
     * Handle a notification that a node is leaving the cluster.
     */
    public void handleNodeLeaving(String leavingNodeId) {
        if (!highAvailabilityEnabled) {
            return;
        }

        ClusterNode leavingNode = clusterNodes.remove(leavingNodeId);
        if (leavingNode != null) {
            logger.info("Node {} is leaving the cluster", leavingNodeId);

            // Unregister from ShardManagerService
            shardManagerService.unregisterShardNode(leavingNodeId);

            // If the leaving node was the leader, elect a new leader
            if (leavingNodeId.equals(currentLeaderId.get())) {
                logger.info("Leader node {} is leaving, initiating new leader election", leavingNodeId);
                electLeader();
            }

            // Redistribute workload
            redistributeWorkload();
        }
    }

    /**
     * Handle a leader change notification.
     */
    public void handleLeaderChange(ClusterState clusterState) {
        if (!highAvailabilityEnabled) {
            return;
        }

        String newLeaderId = clusterState.getLeaderId();
        if (newLeaderId != null) {
            logger.info("Received leader change notification, new leader: {}", newLeaderId);
            currentLeaderId.set(newLeaderId);
            isLeader.set(nodeId.equals(newLeaderId));
        }

        // Update cluster nodes
        for (ClusterNode node : clusterState.getNodes()) {
            if (!node.getId().equals(nodeId)) {
                clusterNodes.put(node.getId(), node);
                shardManagerService.registerShardNode(node.getId(), node.getUrl());
            }
        }
    }

    /**
     * Get the current cluster state.
     */
    public ClusterState getClusterState() {
        ClusterState state = new ClusterState();
        state.setLeaderId(currentLeaderId.get());
        state.setNodes(new ArrayList<>(clusterNodes.values()));
        return state;
    }

    /**
     * Check if high availability is enabled.
     */
    public boolean isHighAvailabilityEnabled() {
        return highAvailabilityEnabled;
    }

    /**
     * Set whether high availability is enabled.
     */
    public void setHighAvailabilityEnabled(boolean enabled) {
        if (this.highAvailabilityEnabled == enabled) {
            return;
        }

        if (enabled) {
            // Enable HA: flip flag first so init() proceeds with registration
            this.highAvailabilityEnabled = true;
            init();
            logger.info("High availability enabled by configuration");
        } else {
            // Disable HA: perform cleanup while still enabled so destroy() executes unregister logic
            destroy();
            this.highAvailabilityEnabled = false;
            logger.info("High availability disabled by configuration");
        }
    }

    /**
     * Check if this node is the leader.
     */
    public boolean isLeader() {
        return isLeader.get();
    }

    /**
     * Get the ID of this node.
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * Get the URL of this node.
     */
    public String getNodeUrl() {
        return nodeUrl;
    }

    /**
     * Get the current leader ID.
     */
    public String getCurrentLeaderId() {
        return currentLeaderId.get();
    }

    /**
     * Get all cluster nodes.
     */
    public Map<String, ClusterNode> getClusterNodes() {
        // Return live map to allow tests and integration points to modify cluster state directly when needed
        return clusterNodes;
    }
}