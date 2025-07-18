package io.github.ozkanpakdil.grepwise.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the LogSourceConfig model.
 */
public class LogSourceConfigTest {

    @Test
    public void testDefaultConstructor() {
        LogSourceConfig config = new LogSourceConfig();
        
        // Check default values
        assertTrue(config.isEnabled());
        assertEquals(LogSourceConfig.SourceType.FILE, config.getSourceType());
        assertEquals("*.log", config.getFilePattern());
        assertEquals(60, config.getScanIntervalSeconds());
        assertEquals(514, config.getSyslogPort());
        assertEquals("UDP", config.getSyslogProtocol());
        assertEquals("RFC5424", config.getSyslogFormat());
        assertEquals("/api/logs", config.getHttpEndpoint());
        assertTrue(config.isRequireAuth());
    }
    
    @Test
    public void testCreateFileSource() {
        String id = "test-id";
        String name = "Test File Source";
        String directoryPath = "/var/log";
        String filePattern = "*.log";
        long scanIntervalSeconds = 120;
        boolean enabled = true;
        
        LogSourceConfig config = LogSourceConfig.createFileSource(
                id, name, directoryPath, filePattern, scanIntervalSeconds, enabled);
        
        assertEquals(id, config.getId());
        assertEquals(name, config.getName());
        assertEquals(LogSourceConfig.SourceType.FILE, config.getSourceType());
        assertEquals(directoryPath, config.getDirectoryPath());
        assertEquals(filePattern, config.getFilePattern());
        assertEquals(scanIntervalSeconds, config.getScanIntervalSeconds());
        assertEquals(enabled, config.isEnabled());
    }
    
    @Test
    public void testCreateSyslogSource() {
        String id = "test-id";
        String name = "Test Syslog Source";
        int syslogPort = 1514;
        String syslogProtocol = "TCP";
        String syslogFormat = "RFC3164";
        boolean enabled = true;
        
        LogSourceConfig config = LogSourceConfig.createSyslogSource(
                id, name, syslogPort, syslogProtocol, syslogFormat, enabled);
        
        assertEquals(id, config.getId());
        assertEquals(name, config.getName());
        assertEquals(LogSourceConfig.SourceType.SYSLOG, config.getSourceType());
        assertEquals(syslogPort, config.getSyslogPort());
        assertEquals(syslogProtocol, config.getSyslogProtocol());
        assertEquals(syslogFormat, config.getSyslogFormat());
        assertEquals(enabled, config.isEnabled());
    }
    
    @Test
    public void testCreateHttpSource() {
        String id = "test-id";
        String name = "Test HTTP Source";
        String httpEndpoint = "/custom/logs";
        String httpAuthToken = "secret-token";
        boolean requireAuth = true;
        boolean enabled = true;
        
        LogSourceConfig config = LogSourceConfig.createHttpSource(
                id, name, httpEndpoint, httpAuthToken, requireAuth, enabled);
        
        assertEquals(id, config.getId());
        assertEquals(name, config.getName());
        assertEquals(LogSourceConfig.SourceType.HTTP, config.getSourceType());
        assertEquals(httpEndpoint, config.getHttpEndpoint());
        assertEquals(httpAuthToken, config.getHttpAuthToken());
        assertEquals(requireAuth, config.isRequireAuth());
        assertEquals(enabled, config.isEnabled());
    }
    
    @Test
    public void testFromLogDirectoryConfig() {
        // Create a legacy LogDirectoryConfig
        LogDirectoryConfig legacyConfig = new LogDirectoryConfig();
        legacyConfig.setId("legacy-id");
        legacyConfig.setDirectoryPath("/var/log");
        legacyConfig.setFilePattern("*.log");
        legacyConfig.setScanIntervalSeconds(120);
        legacyConfig.setEnabled(true);
        
        // Convert to LogSourceConfig
        LogSourceConfig config = LogSourceConfig.fromLogDirectoryConfig(legacyConfig);
        
        // Check conversion
        assertEquals(legacyConfig.getId(), config.getId());
        assertEquals("Converted from " + legacyConfig.getDirectoryPath(), config.getName());
        assertEquals(LogSourceConfig.SourceType.FILE, config.getSourceType());
        assertEquals(legacyConfig.getDirectoryPath(), config.getDirectoryPath());
        assertEquals(legacyConfig.getFilePattern(), config.getFilePattern());
        assertEquals(legacyConfig.getScanIntervalSeconds(), config.getScanIntervalSeconds());
        assertEquals(legacyConfig.isEnabled(), config.isEnabled());
    }
    
    @Test
    public void testToLogDirectoryConfig() {
        // Create a LogSourceConfig for a file source
        LogSourceConfig config = LogSourceConfig.createFileSource(
                "source-id", "Test Source", "/var/log", "*.log", 120, true);
        
        // Convert to legacy LogDirectoryConfig
        LogDirectoryConfig legacyConfig = config.toLogDirectoryConfig();
        
        // Check conversion
        assertEquals(config.getId(), legacyConfig.getId());
        assertEquals(config.getDirectoryPath(), legacyConfig.getDirectoryPath());
        assertEquals(config.getFilePattern(), legacyConfig.getFilePattern());
        assertEquals(config.getScanIntervalSeconds(), legacyConfig.getScanIntervalSeconds());
        assertEquals(config.isEnabled(), legacyConfig.isEnabled());
    }
    
    @Test
    public void testToLogDirectoryConfigWithNonFileSource() {
        // Create a LogSourceConfig for a syslog source
        LogSourceConfig config = LogSourceConfig.createSyslogSource(
                "source-id", "Test Source", 1514, "TCP", "RFC3164", true);
        
        // Attempt to convert to legacy LogDirectoryConfig should throw an exception
        assertThrows(IllegalStateException.class, () -> {
            config.toLogDirectoryConfig();
        });
    }
    
    @Test
    public void testEqualsAndHashCode() {
        // Create two identical configs
        LogSourceConfig config1 = LogSourceConfig.createFileSource(
                "source-id", "Test Source", "/var/log", "*.log", 120, true);
        LogSourceConfig config2 = LogSourceConfig.createFileSource(
                "source-id", "Test Source", "/var/log", "*.log", 120, true);
        
        // Check equals and hashCode
        assertEquals(config1, config2);
        assertEquals(config1.hashCode(), config2.hashCode());
        
        // Modify one config
        config2.setFilePattern("*.txt");
        
        // Check equals and hashCode again
        assertNotEquals(config1, config2);
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }
}