package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.controller.HttpLogController;
import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import io.github.ozkanpakdil.grepwise.repository.LogDirectoryConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for horizontal scaling of log ingestion.
 * Tests the integration between LogIngestionCoordinatorService and LogSourceService.
 */
public class LogSourceHorizontalScalingIntegrationTest {

    @Mock
    private LogScannerService logScannerService;
    
    @Mock
    private SyslogServer syslogServer;
    
    @Mock
    private HttpLogController httpLogController;
    
    @Mock
    private CloudWatchLogService cloudWatchLogService;
    
    @Mock
    private LogDirectoryConfigRepository legacyConfigRepository;
    
    private LogIngestionCoordinatorService coordinatorService1;
    private LogIngestionCoordinatorService coordinatorService2;
    private LogSourceService logSourceService1;
    private LogSourceService logSourceService2;
    
    private String instanceId1 = "test-instance-1";
    private String instanceId2 = "test-instance-2";
    
    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create coordinator services with fixed instance IDs
        coordinatorService1 = new LogIngestionCoordinatorService();
        ReflectionTestUtils.setField(coordinatorService1, "instanceId", instanceId1);
        ReflectionTestUtils.setField(coordinatorService1, "configuredInstanceId", instanceId1);
        
        coordinatorService2 = new LogIngestionCoordinatorService();
        ReflectionTestUtils.setField(coordinatorService2, "instanceId", instanceId2);
        ReflectionTestUtils.setField(coordinatorService2, "configuredInstanceId", instanceId2);
        
        // Create log source services with the coordinator services
        logSourceService1 = new LogSourceService(
                logScannerService,
                syslogServer,
                httpLogController,
                cloudWatchLogService,
                legacyConfigRepository,
                coordinatorService1
        );
        
        logSourceService2 = new LogSourceService(
                logScannerService,
                syslogServer,
                httpLogController,
                cloudWatchLogService,
                legacyConfigRepository,
                coordinatorService2
        );
        
        // Set up the instance heartbeats map to include both instances
        Map<String, Long> instanceHeartbeats1 = new ConcurrentHashMap<>();
        instanceHeartbeats1.put(instanceId1, System.currentTimeMillis());
        instanceHeartbeats1.put(instanceId2, System.currentTimeMillis());
        ReflectionTestUtils.setField(coordinatorService1, "instanceHeartbeats", instanceHeartbeats1);
        
        Map<String, Long> instanceHeartbeats2 = new ConcurrentHashMap<>();
        instanceHeartbeats2.put(instanceId1, System.currentTimeMillis());
        instanceHeartbeats2.put(instanceId2, System.currentTimeMillis());
        ReflectionTestUtils.setField(coordinatorService2, "instanceHeartbeats", instanceHeartbeats2);
        
        // Enable horizontal scaling
        coordinatorService1.setHorizontalScalingEnabled(true);
        coordinatorService2.setHorizontalScalingEnabled(true);
        
        // Set up the sources map in both LogSourceService instances
        Map<String, LogSourceConfig> sources = new ConcurrentHashMap<>();
        for (LogSourceConfig source : createTestSources(10)) {
            sources.put(source.getId(), source);
        }
        ReflectionTestUtils.setField(logSourceService1, "sources", sources);
        ReflectionTestUtils.setField(logSourceService2, "sources", sources);
        
        // Set up mocks
        when(logScannerService.saveConfig(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(syslogServer.startListener(any())).thenReturn(true);
        when(httpLogController.registerHttpSource(any())).thenReturn(true);
        when(cloudWatchLogService.registerSource(any())).thenReturn(true);
    }
    
    @Test
    public void testSourceDistribution() {
        // Get sources from both services
        List<LogSourceConfig> sources1 = logSourceService1.getAllSources();
        List<LogSourceConfig> sources2 = logSourceService2.getAllSources();
        
        // Verify that sources are distributed between the two instances
        assertFalse(sources1.isEmpty());
        assertFalse(sources2.isEmpty());
        assertTrue(sources1.size() < 10); // Not all sources assigned to instance 1
        assertTrue(sources2.size() < 10); // Not all sources assigned to instance 2
        assertEquals(10, sources1.size() + sources2.size()); // All sources are assigned
        
        // Verify that no source is assigned to both instances
        for (LogSourceConfig source : sources1) {
            assertFalse(sources2.contains(source));
        }
    }
    
    @Test
    public void testStartSource() {
        // Get all sources
        Map<String, LogSourceConfig> allSources = (Map<String, LogSourceConfig>) ReflectionTestUtils.getField(logSourceService1, "sources");
        
        // Start all sources on both instances
        for (LogSourceConfig source : allSources.values()) {
            logSourceService1.startSource(source);
            logSourceService2.startSource(source);
        }
        
        // Verify that each source is started on exactly one instance
        int startedOnInstance1 = 0;
        int startedOnInstance2 = 0;
        
        for (LogSourceConfig source : allSources.values()) {
            if (coordinatorService1.shouldProcessSource(source.getId())) {
                startedOnInstance1++;
            } else if (coordinatorService2.shouldProcessSource(source.getId())) {
                startedOnInstance2++;
            }
        }
        
        // Verify that all sources are started
        assertEquals(allSources.size(), startedOnInstance1 + startedOnInstance2);
        // Verify that the file scan was triggered exactly once per source overall
        verify(logScannerService, times(allSources.size())).scanDirectory(any());
    }
    
    @Test
    public void testDisableHorizontalScaling() {
        // Disable horizontal scaling on both instances
        coordinatorService1.setHorizontalScalingEnabled(false);
        coordinatorService2.setHorizontalScalingEnabled(false);
        
        // Get sources from both services
        List<LogSourceConfig> sources1 = logSourceService1.getAllSources();
        List<LogSourceConfig> sources2 = logSourceService2.getAllSources();
        
        // Verify that all sources are assigned to both instances
        assertEquals(10, sources1.size());
        assertEquals(10, sources2.size());
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