package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.ArchiveConfiguration;
import io.github.ozkanpakdil.grepwise.model.ArchiveMetadata;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.repository.ArchiveConfigurationRepository;
import io.github.ozkanpakdil.grepwise.repository.ArchiveMetadataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ArchiveServiceTest {

    @Mock
    private ArchiveConfigurationRepository archiveConfigurationRepository;

    @Mock
    private ArchiveMetadataRepository archiveMetadataRepository;

    @Spy
    @InjectMocks
    private ArchiveService archiveService;

    @TempDir
    Path tempDir;

    private ArchiveConfiguration testConfig;
    private List<LogEntry> testLogs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create test configuration
        testConfig = new ArchiveConfiguration();
        testConfig.setId("test-config-id");
        testConfig.setArchiveDirectory(tempDir.toString());
        testConfig.setAutoArchiveEnabled(true);
        testConfig.setCompressionLevel(5);
        testConfig.setMaxArchiveSizeMb(100);
        testConfig.setArchiveRetentionDays(90);
        
        // Mock the configuration repository
        when(archiveConfigurationRepository.getDefaultConfiguration()).thenReturn(testConfig);
        
        // Create test logs
        testLogs = createTestLogs(10);
        
        // Initialize the service
        try {
            archiveService.init();
        } catch (IOException e) {
            fail("Failed to initialize ArchiveService: " + e.getMessage());
        }
    }

    @Test
    void testArchiveLogs() throws IOException {
        // Mock the metadata repository
        when(archiveMetadataRepository.save(any(ArchiveMetadata.class))).thenAnswer(invocation -> {
            ArchiveMetadata metadata = invocation.getArgument(0);
            if (metadata.getId() == null) {
                metadata.setId(UUID.randomUUID().toString());
            }
            return metadata;
        });
        
        // Archive logs
        ArchiveMetadata metadata = archiveService.archiveLogs(testLogs);
        
        // Verify results
        assertNotNull(metadata, "Archive metadata should not be null");
        assertEquals(testLogs.size(), metadata.getLogCount(), "Log count should match");
        assertTrue(metadata.getSizeBytes() > 0, "Archive size should be greater than 0");
        
        // Verify file was created
        Path archivePath = Path.of(testConfig.getArchiveDirectory(), metadata.getFilename());
        assertTrue(Files.exists(archivePath), "Archive file should exist");
        
        // Verify repository was called
        verify(archiveMetadataRepository).save(any(ArchiveMetadata.class));
    }

    @Test
    void testArchiveLogsBeforeDeletion() throws IOException {
        // Mock the archiveLogs method
        doReturn(new ArchiveMetadata()).when(archiveService).archiveLogs(any());
        
        // Test with auto-archive enabled
        boolean result = archiveService.archiveLogsBeforeDeletion(testLogs);
        assertTrue(result, "Archive should succeed");
        verify(archiveService).archiveLogs(testLogs);
        
        // Test with auto-archive disabled
        testConfig.setAutoArchiveEnabled(false);
        result = archiveService.archiveLogsBeforeDeletion(testLogs);
        assertTrue(result, "Should return true when auto-archive is disabled");
        
        // Test with empty logs
        result = archiveService.archiveLogsBeforeDeletion(List.of());
        assertTrue(result, "Should return true when logs are empty");
    }

    @Test
    void testGetArchiveConfiguration() {
        ArchiveConfiguration config = archiveService.getArchiveConfiguration();
        assertNotNull(config, "Configuration should not be null");
        assertEquals(testConfig, config, "Configuration should match test config");
        verify(archiveConfigurationRepository).getDefaultConfiguration();
    }

    @Test
    void testUpdateArchiveConfiguration() {
        // Mock the save method
        when(archiveConfigurationRepository.save(any(ArchiveConfiguration.class))).thenReturn(testConfig);
        
        // Update configuration
        ArchiveConfiguration updatedConfig = archiveService.updateArchiveConfiguration(testConfig);
        assertNotNull(updatedConfig, "Updated configuration should not be null");
        assertEquals(testConfig, updatedConfig, "Updated configuration should match test config");
        verify(archiveConfigurationRepository).save(testConfig);
    }

    @Test
    void testDeleteArchive() throws IOException {
        // Create test metadata
        ArchiveMetadata metadata = new ArchiveMetadata();
        metadata.setId("test-archive-id");
        metadata.setFilename("test-archive.zip");
        
        // Create test file
        Path archivePath = Path.of(testConfig.getArchiveDirectory(), metadata.getFilename());
        Files.createFile(archivePath);
        
        // Mock repository methods
        when(archiveMetadataRepository.findById("test-archive-id")).thenReturn(metadata);
        when(archiveMetadataRepository.deleteById("test-archive-id")).thenReturn(true);
        
        // Delete archive
        boolean result = archiveService.deleteArchive("test-archive-id");
        
        // Verify results
        assertTrue(result, "Delete should succeed");
        assertFalse(Files.exists(archivePath), "Archive file should be deleted");
        verify(archiveMetadataRepository).findById("test-archive-id");
        verify(archiveMetadataRepository).deleteById("test-archive-id");
    }

    /**
     * Helper method to create test logs.
     */
    private List<LogEntry> createTestLogs(int count) {
        List<LogEntry> logs = new ArrayList<>();
        long baseTimestamp = System.currentTimeMillis() - (count * 1000);
        
        for (int i = 0; i < count; i++) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("test-key", "test-value-" + i);
            
            LogEntry log = new LogEntry(
                    "log-" + i,
                    baseTimestamp + (i * 1000),
                    "INFO",
                    "Test log message " + i,
                    "test-source",
                    metadata,
                    "Raw log content " + i
            );
            
            logs.add(log);
        }
        
        return logs;
    }
}