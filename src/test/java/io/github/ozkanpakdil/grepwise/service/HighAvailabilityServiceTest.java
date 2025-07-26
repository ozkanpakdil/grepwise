package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.ClusterNode;
import io.github.ozkanpakdil.grepwise.model.ClusterState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the HighAvailabilityService.
 */
class HighAvailabilityServiceTest {

    @Mock
    private LogIngestionCoordinatorService logIngestionCoordinatorService;

    @Mock
    private ShardManagerService shardManagerService;

    @InjectMocks
    private HighAvailabilityService highAvailabilityService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up test configuration
        ReflectionTestUtils.setField(highAvailabilityService, "highAvailabilityEnabled", false);
        ReflectionTestUtils.setField(highAvailabilityService, "nodeId", "test-node-1");
        ReflectionTestUtils.setField(highAvailabilityService, "nodeUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(highAvailabilityService, "heartbeatIntervalMs", 5000L);
        ReflectionTestUtils.setField(highAvailabilityService, "heartbeatTimeoutMs", 15000L);
    }

    @Test
    void testInitWhenHighAvailabilityEnabled() {
        // Set high availability to enabled
        ReflectionTestUtils.setField(highAvailabilityService, "highAvailabilityEnabled", true);
        
        // Call init method
        highAvailabilityService.init();
        
        // Verify that the node is registered with ShardManagerService
        verify(shardManagerService).registerShardNode(anyString(), anyString());
        
        // Verify that the node is in the cluster nodes map
        Map<String, ClusterNode> clusterNodes = highAvailabilityService.getClusterNodes();
        assertNotNull(clusterNodes);
        assertEquals(1, clusterNodes.size());
        assertTrue(clusterNodes.containsKey("test-node-1"));
    }

    @Test
    void testInitWhenHighAvailabilityDisabled() {
        // Set high availability to disabled
        ReflectionTestUtils.setField(highAvailabilityService, "highAvailabilityEnabled", false);
        
        // Call init method
        highAvailabilityService.init();
        
        // Verify that the node is not registered with ShardManagerService
        verify(shardManagerService, never()).registerShardNode(anyString(), anyString());
        
        // Verify that the cluster nodes map is empty
        Map<String, ClusterNode> clusterNodes = highAvailabilityService.getClusterNodes();
        assertNotNull(clusterNodes);
        assertEquals(0, clusterNodes.size());
    }

    @Test
    void testHandleHeartbeat() {
        // Set high availability to enabled
        ReflectionTestUtils.setField(highAvailabilityService, "highAvailabilityEnabled", true);
        
        // Call handleHeartbeat method
        String nodeId = "test-node-2";
        String nodeUrl = "http://localhost:8081";
        long timestamp = System.currentTimeMillis();
        boolean isLeader = true;
        
        highAvailabilityService.handleHeartbeat(nodeId, nodeUrl, timestamp, isLeader);
        
        // Verify that the node is registered with ShardManagerService
        verify(shardManagerService).registerShardNode(nodeId, nodeUrl);
        
        // Verify that the node is in the cluster nodes map
        Map<String, ClusterNode> clusterNodes = highAvailabilityService.getClusterNodes();
        assertNotNull(clusterNodes);
        assertTrue(clusterNodes.containsKey(nodeId));
        
        // Verify that the node is recognized as the leader
        assertEquals(nodeId, highAvailabilityService.getCurrentLeaderId());
        assertFalse(highAvailabilityService.isLeader());
    }

    @Test
    void testHandleNodeLeaving() {
        // Set high availability to enabled
        ReflectionTestUtils.setField(highAvailabilityService, "highAvailabilityEnabled", true);
        
        // Add a node to the cluster
        String nodeId = "test-node-2";
        ClusterNode node = new ClusterNode(nodeId, "http://localhost:8081", System.currentTimeMillis(), true);
        Map<String, ClusterNode> clusterNodes = highAvailabilityService.getClusterNodes();
        clusterNodes.put(nodeId, node);
        
        // Set the node as the leader
        ReflectionTestUtils.setField(highAvailabilityService, "currentLeaderId", new java.util.concurrent.atomic.AtomicReference<>(nodeId));
        
        // Call handleNodeLeaving method
        highAvailabilityService.handleNodeLeaving(nodeId);
        
        // Verify that the node is unregistered from ShardManagerService
        verify(shardManagerService).unregisterShardNode(nodeId);
        
        // Verify that the node is removed from the cluster nodes map
        clusterNodes = highAvailabilityService.getClusterNodes();
        assertNotNull(clusterNodes);
        assertFalse(clusterNodes.containsKey(nodeId));
    }

    @Test
    void testHandleLeaderChange() {
        // Set high availability to enabled
        ReflectionTestUtils.setField(highAvailabilityService, "highAvailabilityEnabled", true);
        
        // Create a cluster state with a new leader
        String leaderId = "test-node-2";
        ClusterNode leaderNode = new ClusterNode(leaderId, "http://localhost:8081", System.currentTimeMillis(), true);
        ClusterNode otherNode = new ClusterNode("test-node-3", "http://localhost:8082", System.currentTimeMillis(), true);
        ClusterState clusterState = new ClusterState(leaderId, Arrays.asList(leaderNode, otherNode));
        
        // Call handleLeaderChange method
        highAvailabilityService.handleLeaderChange(clusterState);
        
        // Verify that the leader is updated
        assertEquals(leaderId, highAvailabilityService.getCurrentLeaderId());
        assertFalse(highAvailabilityService.isLeader());
        
        // Verify that the nodes are registered with ShardManagerService
        verify(shardManagerService).registerShardNode(leaderId, leaderNode.getUrl());
        verify(shardManagerService).registerShardNode("test-node-3", otherNode.getUrl());
        
        // Verify that the nodes are in the cluster nodes map
        Map<String, ClusterNode> clusterNodes = highAvailabilityService.getClusterNodes();
        assertNotNull(clusterNodes);
        assertTrue(clusterNodes.containsKey(leaderId));
        assertTrue(clusterNodes.containsKey("test-node-3"));
    }

    @Test
    void testSetHighAvailabilityEnabled() {
        // Initially disabled
        assertFalse(highAvailabilityService.isHighAvailabilityEnabled());
        
        // Enable high availability
        highAvailabilityService.setHighAvailabilityEnabled(true);
        
        // Verify that high availability is enabled
        assertTrue(highAvailabilityService.isHighAvailabilityEnabled());
        
        // Verify that the node is registered with ShardManagerService
        verify(shardManagerService).registerShardNode(anyString(), anyString());
        
        // Disable high availability
        highAvailabilityService.setHighAvailabilityEnabled(false);
        
        // Verify that high availability is disabled
        assertFalse(highAvailabilityService.isHighAvailabilityEnabled());
        
        // Verify that the node is unregistered from ShardManagerService
        verify(shardManagerService).unregisterShardNode(anyString());
    }

    @Test
    void testGetClusterState() {
        // Set high availability to enabled
        ReflectionTestUtils.setField(highAvailabilityService, "highAvailabilityEnabled", true);
        
        // Add nodes to the cluster
        String nodeId1 = "test-node-1";
        String nodeId2 = "test-node-2";
        ClusterNode node1 = new ClusterNode(nodeId1, "http://localhost:8080", System.currentTimeMillis(), true);
        ClusterNode node2 = new ClusterNode(nodeId2, "http://localhost:8081", System.currentTimeMillis(), true);
        
        Map<String, ClusterNode> clusterNodes = highAvailabilityService.getClusterNodes();
        clusterNodes.put(nodeId1, node1);
        clusterNodes.put(nodeId2, node2);
        
        // Set the leader
        ReflectionTestUtils.setField(highAvailabilityService, "currentLeaderId", new java.util.concurrent.atomic.AtomicReference<>(nodeId1));
        
        // Get the cluster state
        ClusterState clusterState = highAvailabilityService.getClusterState();
        
        // Verify the cluster state
        assertNotNull(clusterState);
        assertEquals(nodeId1, clusterState.getLeaderId());
        assertEquals(2, clusterState.getNodes().size());
        assertTrue(clusterState.getNodes().contains(node1));
        assertTrue(clusterState.getNodes().contains(node2));
    }
}