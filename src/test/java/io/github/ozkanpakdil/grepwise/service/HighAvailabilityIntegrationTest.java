package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.ClusterNode;
import io.github.ozkanpakdil.grepwise.model.ClusterState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

/**
 * Integration tests for the high availability architecture.
 * These tests simulate multiple nodes running together in a cluster.
 */
class HighAvailabilityIntegrationTest {

    private ShardManagerService shardManagerService1;
    private ShardManagerService shardManagerService2;
    private LogIngestionCoordinatorService logIngestionCoordinatorService1;
    private LogIngestionCoordinatorService logIngestionCoordinatorService2;
    private HighAvailabilityService highAvailabilityService1;
    private HighAvailabilityService highAvailabilityService2;

    @BeforeEach
    void setUp() {
        // Create mock services
        shardManagerService1 = mock(ShardManagerService.class);
        shardManagerService2 = mock(ShardManagerService.class);
        logIngestionCoordinatorService1 = mock(LogIngestionCoordinatorService.class);
        logIngestionCoordinatorService2 = mock(LogIngestionCoordinatorService.class);

        // Create high availability services
        highAvailabilityService1 = new HighAvailabilityService();
        highAvailabilityService2 = new HighAvailabilityService();

        // Inject dependencies
        ReflectionTestUtils.setField(highAvailabilityService1, "shardManagerService", shardManagerService1);
        ReflectionTestUtils.setField(highAvailabilityService1, "logIngestionCoordinatorService", logIngestionCoordinatorService1);
        ReflectionTestUtils.setField(highAvailabilityService2, "shardManagerService", shardManagerService2);
        ReflectionTestUtils.setField(highAvailabilityService2, "logIngestionCoordinatorService", logIngestionCoordinatorService2);

        // Configure node 1
        ReflectionTestUtils.setField(highAvailabilityService1, "highAvailabilityEnabled", true);
        ReflectionTestUtils.setField(highAvailabilityService1, "nodeId", "test-node-1");
        ReflectionTestUtils.setField(highAvailabilityService1, "nodeUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(highAvailabilityService1, "heartbeatIntervalMs", 100L);
        ReflectionTestUtils.setField(highAvailabilityService1, "heartbeatTimeoutMs", 300L);

        // Configure node 2
        ReflectionTestUtils.setField(highAvailabilityService2, "highAvailabilityEnabled", true);
        ReflectionTestUtils.setField(highAvailabilityService2, "nodeId", "test-node-2");
        ReflectionTestUtils.setField(highAvailabilityService2, "nodeUrl", "http://localhost:8081");
        ReflectionTestUtils.setField(highAvailabilityService2, "heartbeatIntervalMs", 100L);
        ReflectionTestUtils.setField(highAvailabilityService2, "heartbeatTimeoutMs", 300L);
    }

    @AfterEach
    void tearDown() {
        // Clean up resources
        highAvailabilityService1.setHighAvailabilityEnabled(false);
        highAvailabilityService2.setHighAvailabilityEnabled(false);
    }

    @Test
    void testNodeDiscoveryAndLeaderElection() throws InterruptedException {
        // Initialize both nodes
        highAvailabilityService1.init();
        highAvailabilityService2.init();

        // Simulate node discovery by exchanging heartbeats
        simulateHeartbeatExchange();

        // Verify that both nodes are aware of each other
        Map<String, ClusterNode> nodes1 = highAvailabilityService1.getClusterNodes();
        Map<String, ClusterNode> nodes2 = highAvailabilityService2.getClusterNodes();

        assertEquals(2, nodes1.size());
        assertEquals(2, nodes2.size());
        assertTrue(nodes1.containsKey("test-node-1"));
        assertTrue(nodes1.containsKey("test-node-2"));
        assertTrue(nodes2.containsKey("test-node-1"));
        assertTrue(nodes2.containsKey("test-node-2"));

        // Verify that a leader has been elected (should be test-node-1 since it has the lowest ID)
        String leaderId1 = highAvailabilityService1.getCurrentLeaderId();
        String leaderId2 = highAvailabilityService2.getCurrentLeaderId();

        assertEquals("test-node-1", leaderId1);
        assertEquals("test-node-1", leaderId2);
        assertTrue(highAvailabilityService1.isLeader());
        assertFalse(highAvailabilityService2.isLeader());
    }

    @Test
    void testLeaderFailover() throws InterruptedException {
        // Initialize both nodes
        highAvailabilityService1.init();
        highAvailabilityService2.init();

        // Simulate node discovery by exchanging heartbeats
        simulateHeartbeatExchange();

        // Verify initial leader
        assertEquals("test-node-1", highAvailabilityService1.getCurrentLeaderId());
        assertEquals("test-node-1", highAvailabilityService2.getCurrentLeaderId());
        assertTrue(highAvailabilityService1.isLeader());
        assertFalse(highAvailabilityService2.isLeader());

        // Simulate leader failure by disabling high availability on node 1
        highAvailabilityService1.setHighAvailabilityEnabled(false);

        // Simulate node 2 detecting the leader failure
        ClusterNode node1 = highAvailabilityService2.getClusterNodes().get("test-node-1");
        if (node1 != null) {
            highAvailabilityService2.handleNodeLeaving("test-node-1");
        }

        // Verify that node 2 has elected itself as the new leader
        assertEquals("test-node-2", highAvailabilityService2.getCurrentLeaderId());
        assertTrue(highAvailabilityService2.isLeader());
    }

    @Test
    void testClusterStateConsistency() throws InterruptedException {
        // Initialize both nodes
        highAvailabilityService1.init();
        highAvailabilityService2.init();

        // Simulate node discovery by exchanging heartbeats
        simulateHeartbeatExchange();

        // Get cluster state from both nodes
        ClusterState state1 = highAvailabilityService1.getClusterState();
        ClusterState state2 = highAvailabilityService2.getClusterState();

        // Verify that both nodes have the same view of the cluster
        assertEquals(state1.getLeaderId(), state2.getLeaderId());
        assertEquals(state1.getNodes().size(), state2.getNodes().size());
        assertEquals(state1.getActiveNodeCount(), state2.getActiveNodeCount());
    }

    @Test
    void testNodeJoiningAndLeaving() throws InterruptedException {
        // Initialize node 1 only
        highAvailabilityService1.init();

        // Verify that node 1 is the leader
        assertEquals("test-node-1", highAvailabilityService1.getCurrentLeaderId());
        assertTrue(highAvailabilityService1.isLeader());

        // Initialize node 2 (joining the cluster)
        highAvailabilityService2.init();

        // Simulate node discovery by exchanging heartbeats
        simulateHeartbeatExchange();

        // Verify that both nodes are aware of each other
        assertEquals(2, highAvailabilityService1.getClusterNodes().size());
        assertEquals(2, highAvailabilityService2.getClusterNodes().size());

        // Simulate node 2 leaving the cluster
        highAvailabilityService2.setHighAvailabilityEnabled(false);
        highAvailabilityService1.handleNodeLeaving("test-node-2");

        // Verify that node 1 is aware that node 2 has left
        assertEquals(1, highAvailabilityService1.getClusterNodes().size());
        assertFalse(highAvailabilityService1.getClusterNodes().containsKey("test-node-2"));
    }

    /**
     * Helper method to simulate heartbeat exchange between nodes.
     */
    private void simulateHeartbeatExchange() throws InterruptedException {
        // Simulate node 1 sending heartbeat to node 2
        String nodeId1 = (String) ReflectionTestUtils.getField(highAvailabilityService1, "nodeId");
        String nodeUrl1 = (String) ReflectionTestUtils.getField(highAvailabilityService1, "nodeUrl");
        boolean isLeader1 = highAvailabilityService1.isLeader();
        highAvailabilityService2.handleHeartbeat(nodeId1, nodeUrl1, System.currentTimeMillis(), isLeader1);

        // Simulate node 2 sending heartbeat to node 1
        String nodeId2 = (String) ReflectionTestUtils.getField(highAvailabilityService2, "nodeId");
        String nodeUrl2 = (String) ReflectionTestUtils.getField(highAvailabilityService2, "nodeUrl");
        boolean isLeader2 = highAvailabilityService2.isLeader();
        highAvailabilityService1.handleHeartbeat(nodeId2, nodeUrl2, System.currentTimeMillis(), isLeader2);

        // Allow time for leader election
        Thread.sleep(100);
    }
}