package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.ShardConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ShardManagerServiceTest {

    @Mock
    private LuceneService luceneService;

    @Mock
    private SearchCacheService searchCacheService;

    @InjectMocks
    private ShardManagerService shardManagerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set default values for properties
        ReflectionTestUtils.setField(shardManagerService, "shardingEnabled", false);
        ReflectionTestUtils.setField(shardManagerService, "localNodeId", "node1");
        ReflectionTestUtils.setField(shardManagerService, "localNodeUrl", "http://localhost:8080");
        
        // Initialize the service
        shardManagerService.init();
    }

    @Test
    void testDistributedSearchWhenShardingDisabled() throws IOException {
        // Arrange
        String queryStr = "test query";
        boolean isRegex = false;
        Long startTime = 1000L;
        Long endTime = 2000L;
        
        List<LogEntry> expectedResults = new ArrayList<>();
        expectedResults.add(new LogEntry("1", 1500L, "INFO", "Test message", "test.log", null, "Raw content"));
        
        when(luceneService.search(queryStr, isRegex, startTime, endTime)).thenReturn(expectedResults);
        
        // Act
        List<LogEntry> results = shardManagerService.distributedSearch(queryStr, isRegex, startTime, endTime);
        
        // Assert
        assertEquals(expectedResults, results);
        verify(luceneService, times(1)).search(queryStr, isRegex, startTime, endTime);
        verify(searchCacheService, never()).getFromCache(anyString(), anyBoolean(), any(), any());
    }

    @Test
    void testDistributedSearchWithCacheHit() throws IOException {
        // Arrange
        String queryStr = "test query";
        boolean isRegex = false;
        Long startTime = 1000L;
        Long endTime = 2000L;
        
        List<LogEntry> cachedResults = new ArrayList<>();
        cachedResults.add(new LogEntry("1", 1500L, "INFO", "Test message", "test.log", null, "Raw content"));
        
        // Enable sharding
        ReflectionTestUtils.setField(shardManagerService, "shardingEnabled", true);
        ReflectionTestUtils.setField(shardManagerService, "isInitialized", true);
        
        // Create a shard configuration
        ShardConfiguration config = new ShardConfiguration();
        config.setShardingEnabled(true);
        config.setShardingType("TIME_BASED");
        ReflectionTestUtils.setField(shardManagerService, "shardConfiguration", config);
        
        when(searchCacheService.getFromCache(queryStr, isRegex, startTime, endTime)).thenReturn(cachedResults);
        
        // Act
        List<LogEntry> results = shardManagerService.distributedSearch(queryStr, isRegex, startTime, endTime);
        
        // Assert
        assertEquals(cachedResults, results);
        verify(searchCacheService, times(1)).getFromCache(queryStr, isRegex, startTime, endTime);
        verify(luceneService, never()).search(anyString(), anyBoolean(), any(), any());
    }

    @Test
    void testRegisterAndUnregisterShardNode() {
        // Arrange
        ReflectionTestUtils.setField(shardManagerService, "shardingEnabled", true);
        
        ShardConfiguration config = new ShardConfiguration();
        config.setShardingEnabled(true);
        ReflectionTestUtils.setField(shardManagerService, "shardConfiguration", config);
        
        // Act - Register node
        shardManagerService.registerShardNode("node2", "http://node2:8080");
        
        // Assert
        Map<String, String> nodes = shardManagerService.getShardNodes();
        assertEquals(2, nodes.size());
        assertTrue(nodes.containsKey("node2"));
        assertEquals("http://node2:8080", nodes.get("node2"));
        
        // Act - Unregister node
        shardManagerService.unregisterShardNode("node2");
        
        // Assert
        nodes = shardManagerService.getShardNodes();
        assertEquals(1, nodes.size());
        assertFalse(nodes.containsKey("node2"));
    }

    @Test
    void testUpdateConfiguration() {
        // Arrange
        ShardConfiguration config = new ShardConfiguration();
        config.setShardingEnabled(true);
        config.setShardingType("SOURCE_BASED");
        config.setNumberOfShards(5);
        
        List<String> shardNodes = new ArrayList<>();
        shardNodes.add("http://localhost:8080");
        shardNodes.add("http://node2:8080");
        config.setShardNodes(shardNodes);
        
        // Act
        shardManagerService.updateConfiguration(config);
        
        // Assert
        assertEquals(config, shardManagerService.getConfiguration());
        assertTrue(shardManagerService.isShardingEnabled());
        
        Map<String, String> nodes = shardManagerService.getShardNodes();
        assertEquals(2, nodes.size());
        assertTrue(nodes.containsKey("node1"));
        assertTrue(nodes.containsKey("node2"));
    }

    @Test
    void testEnableDisableSharding() {
        // Arrange
        ShardConfiguration config = new ShardConfiguration();
        ReflectionTestUtils.setField(shardManagerService, "shardConfiguration", config);
        
        // Act - Enable
        shardManagerService.setShardingEnabled(true);
        
        // Assert
        assertTrue(shardManagerService.isShardingEnabled());
        assertTrue(config.isShardingEnabled());
        
        // Act - Disable
        shardManagerService.setShardingEnabled(false);
        
        // Assert
        assertFalse(shardManagerService.isShardingEnabled());
        assertFalse(config.isShardingEnabled());
    }
}