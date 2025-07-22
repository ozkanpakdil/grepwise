package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.ShardConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShardConfigurationRepositoryTest {

    private ShardConfigurationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new ShardConfigurationRepository();
    }

    @Test
    void testInitialization() {
        // Assert that the repository is initialized with a default configuration
        assertEquals(1, repository.count());
        
        ShardConfiguration defaultConfig = repository.getDefaultConfiguration();
        assertNotNull(defaultConfig);
        assertEquals("TIME_BASED", defaultConfig.getShardingType());
        assertEquals(3, defaultConfig.getNumberOfShards());
        assertFalse(defaultConfig.isReplicationEnabled());
        assertEquals(1, defaultConfig.getReplicationFactor());
        assertFalse(defaultConfig.isShardingEnabled());
    }

    @Test
    void testSaveAndFindById() {
        // Create a new configuration
        ShardConfiguration config = new ShardConfiguration();
        config.setShardingType("SOURCE_BASED");
        config.setNumberOfShards(5);
        config.setReplicationEnabled(true);
        config.setReplicationFactor(2);
        config.setShardingEnabled(true);
        
        // Save the configuration
        ShardConfiguration savedConfig = repository.save(config);
        
        // Assert that the configuration was saved with an ID
        assertNotNull(savedConfig.getId());
        
        // Find the configuration by ID
        ShardConfiguration foundConfig = repository.findById(savedConfig.getId());
        
        // Assert that the found configuration matches the saved one
        assertNotNull(foundConfig);
        assertEquals(savedConfig.getId(), foundConfig.getId());
        assertEquals("SOURCE_BASED", foundConfig.getShardingType());
        assertEquals(5, foundConfig.getNumberOfShards());
        assertTrue(foundConfig.isReplicationEnabled());
        assertEquals(2, foundConfig.getReplicationFactor());
        assertTrue(foundConfig.isShardingEnabled());
    }

    @Test
    void testFindAll() {
        // Create and save a new configuration
        ShardConfiguration config = new ShardConfiguration();
        config.setShardingType("BALANCED");
        config.setShardingEnabled(true);
        repository.save(config);
        
        // Find all configurations
        List<ShardConfiguration> configs = repository.findAll();
        
        // Assert that both the default and the new configuration are returned
        assertEquals(2, configs.size());
    }

    @Test
    void testFindByShardingEnabledTrue() {
        // Initially, no configuration should have sharding enabled
        assertNull(repository.findByShardingEnabledTrue());
        
        // Create and save a configuration with sharding enabled
        ShardConfiguration config = new ShardConfiguration();
        config.setShardingEnabled(true);
        repository.save(config);
        
        // Find the configuration with sharding enabled
        ShardConfiguration enabledConfig = repository.findByShardingEnabledTrue();
        
        // Assert that the found configuration has sharding enabled
        assertNotNull(enabledConfig);
        assertTrue(enabledConfig.isShardingEnabled());
    }

    @Test
    void testDeleteById() {
        // Create and save a new configuration
        ShardConfiguration config = new ShardConfiguration();
        ShardConfiguration savedConfig = repository.save(config);
        
        // Assert that the configuration was saved
        assertEquals(2, repository.count());
        
        // Delete the configuration by ID
        boolean deleted = repository.deleteById(savedConfig.getId());
        
        // Assert that the configuration was deleted
        assertTrue(deleted);
        assertEquals(1, repository.count());
        assertNull(repository.findById(savedConfig.getId()));
    }

    @Test
    void testDeleteAll() {
        // Create and save a new configuration
        ShardConfiguration config = new ShardConfiguration();
        repository.save(config);
        
        // Assert that there are two configurations (default + new)
        assertEquals(2, repository.count());
        
        // Delete all configurations
        int deleted = repository.deleteAll();
        
        // Assert that both configurations were deleted
        assertEquals(2, deleted);
        assertEquals(0, repository.count());
    }

    @Test
    void testGetDefaultConfigurationWhenEmpty() {
        // Delete all configurations
        repository.deleteAll();
        
        // Get the default configuration
        ShardConfiguration defaultConfig = repository.getDefaultConfiguration();
        
        // Assert that a new default configuration was created
        assertNotNull(defaultConfig);
        assertEquals(1, repository.count());
    }
}