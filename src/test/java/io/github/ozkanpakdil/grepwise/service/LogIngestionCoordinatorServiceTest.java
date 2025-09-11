package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the LogIngestionCoordinatorService.
 * Tests the horizontal scaling functionality for log ingestion.
 */
public class LogIngestionCoordinatorServiceTest {

    private LogIngestionCoordinatorService coordinatorService;
    private String instanceId;

    @BeforeEach
    public void setUp() {
        // Create a new coordinator service with a fixed instance ID for testing
        coordinatorService = new LogIngestionCoordinatorService();
        instanceId = "test-instance-1";
        ReflectionTestUtils.setField(coordinatorService, "instanceId", instanceId);
        ReflectionTestUtils.setField(coordinatorService, "configuredInstanceId", instanceId);
        
        // Enable horizontal scaling
        coordinatorService.setHorizontalScalingEnabled(true);
    }

    @Test
    public void testInstanceRegistration() {
        // Verify that the instance is registered
        List<String> activeInstances = coordinatorService.getActiveInstances();
        assertEquals(1, activeInstances.size());
        assertTrue(activeInstances.contains(instanceId));
        
        // Register another instance
        String anotherInstanceId = "test-instance-2";
        coordinatorService.registerInstance(anotherInstanceId);
        
        // Verify that both instances are registered
        activeInstances = coordinatorService.getActiveInstances();
        assertEquals(2, activeInstances.size());
        assertTrue(activeInstances.contains(instanceId));
        assertTrue(activeInstances.contains(anotherInstanceId));
        
        // Unregister the other instance
        coordinatorService.unregisterInstance(anotherInstanceId);
        
        // Verify that only the original instance is registered
        activeInstances = coordinatorService.getActiveInstances();
        assertEquals(1, activeInstances.size());
        assertTrue(activeInstances.contains(instanceId));
    }
    
    @Test
    public void testSourceAssignment() {
        // Create some test sources
        List<LogSourceConfig> sources = createTestSources(10);
        
        // With only one instance, all sources should be assigned to this instance
        List<LogSourceConfig> assignedSources = coordinatorService.filterSourcesForThisInstance(sources);
        assertEquals(sources.size(), assignedSources.size());
        
        // Register another instance
        String anotherInstanceId = "test-instance-2";
        coordinatorService.registerInstance(anotherInstanceId);
        
        // With two instances, sources should be distributed
        assignedSources = coordinatorService.filterSourcesForThisInstance(sources);
        assertTrue(assignedSources.size() > 0);
        assertTrue(assignedSources.size() < sources.size());
        
        // Create a second coordinator service with the other instance ID
        LogIngestionCoordinatorService otherCoordinatorService = new LogIngestionCoordinatorService();
        ReflectionTestUtils.setField(otherCoordinatorService, "instanceId", anotherInstanceId);
        ReflectionTestUtils.setField(otherCoordinatorService, "configuredInstanceId", anotherInstanceId);
        
        // Set up the instance heartbeats map to match the first service
        Map<String, Long> instanceHeartbeats = new ConcurrentHashMap<>();
        instanceHeartbeats.put(instanceId, System.currentTimeMillis());
        instanceHeartbeats.put(anotherInstanceId, System.currentTimeMillis());
        ReflectionTestUtils.setField(otherCoordinatorService, "instanceHeartbeats", instanceHeartbeats);
        otherCoordinatorService.setHorizontalScalingEnabled(true);
        
        // Get sources assigned to the other instance
        List<LogSourceConfig> otherAssignedSources = otherCoordinatorService.filterSourcesForThisInstance(sources);
        
        // Verify that all sources are assigned to exactly one instance
        assertEquals(sources.size(), assignedSources.size() + otherAssignedSources.size());
        
        // Verify that no source is assigned to both instances
        for (LogSourceConfig source : assignedSources) {
            assertFalse(otherAssignedSources.contains(source));
        }
    }
    
    @Test
    public void testHeartbeatAndCleanup() throws InterruptedException {
        // Register another instance
        String anotherInstanceId = "test-instance-2";
        coordinatorService.registerInstance(anotherInstanceId);
        
        // Verify that both instances are registered
        List<String> activeInstances = coordinatorService.getActiveInstances();
        assertEquals(2, activeInstances.size());
        
        // Set a short heartbeat timeout for testing
        ReflectionTestUtils.setField(coordinatorService, "heartbeatTimeoutMs", 100L);
        
        // Wait for the heartbeat timeout
        Thread.sleep(200);
        
        // Trigger cleanup by updating the heartbeat for this instance
        coordinatorService.updateHeartbeat();
        
        // Verify that the other instance was removed due to heartbeat timeout
        activeInstances = coordinatorService.getActiveInstances();
        assertEquals(1, activeInstances.size());
        assertTrue(activeInstances.contains(instanceId));
        assertFalse(activeInstances.contains(anotherInstanceId));
    }
    
    /**
     * Helper method to create test log sources.
     * 
     * @param count The number of sources to create
     * @return A list of test log sources
     */
    private List<LogSourceConfig> createTestSources(int count) {
        List<LogSourceConfig> sources = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            LogSourceConfig source = new LogSourceConfig();
            source.setId(UUID.randomUUID().toString());
            source.setName("Test Source " + i);
            source.setEnabled(true);
            source.setSourceType(LogSourceConfig.SourceType.FILE);
            sources.add(source);
        }
        return sources;
    }
}