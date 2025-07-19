package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.controller.HttpLogController;
import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import io.github.ozkanpakdil.grepwise.repository.LogDirectoryConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for the LogSourceService.
 */
public class LogSourceServiceTest {

    private LogScannerService logScannerService;
    private SyslogServer syslogServer;
    private HttpLogController httpLogController;
    private CloudWatchLogService cloudWatchLogService;
    private LogDirectoryConfigRepository legacyConfigRepository;
    private LogSourceService logSourceService;

    @BeforeEach
    public void setup() {
        // Create mocks for dependencies
        logScannerService = Mockito.mock(LogScannerService.class);
        syslogServer = Mockito.mock(SyslogServer.class);
        httpLogController = Mockito.mock(HttpLogController.class);
        cloudWatchLogService = Mockito.mock(CloudWatchLogService.class);
        legacyConfigRepository = Mockito.mock(LogDirectoryConfigRepository.class);
        
        // Configure mocks
        when(legacyConfigRepository.findAll()).thenReturn(new ArrayList<>());
        
        // Create the service with mocked dependencies
        logSourceService = new LogSourceService(
                logScannerService,
                syslogServer,
                httpLogController,
                cloudWatchLogService,
                legacyConfigRepository
        );
    }

    @Test
    public void testCreateFileSource() {
        // Configure mocks
        when(logScannerService.saveConfig(any(LogDirectoryConfig.class))).thenReturn(new LogDirectoryConfig());
        when(logScannerService.scanDirectory(any(LogDirectoryConfig.class))).thenReturn(0);
        
        // Create a file source
        LogSourceConfig fileSource = LogSourceConfig.createFileSource(
                "file-source",
                "File Source",
                "/var/log",
                "*.log",
                60,
                true
        );
        
        // Create the source
        LogSourceConfig createdSource = logSourceService.createSource(fileSource);
        
        // Verify the result
        assertNotNull(createdSource);
        assertEquals("file-source", createdSource.getId());
        assertEquals(LogSourceConfig.SourceType.FILE, createdSource.getSourceType());
        
        // Verify that the legacy config was saved
        verify(legacyConfigRepository).save(any(LogDirectoryConfig.class));
        
        // Verify that the source was started
        verify(logScannerService).saveConfig(any(LogDirectoryConfig.class));
        verify(logScannerService).scanDirectory(any(LogDirectoryConfig.class));
    }
    
    @Test
    public void testCreateSyslogSource() {
        // Configure mocks
        when(syslogServer.startListener(any(LogSourceConfig.class))).thenReturn(true);
        
        // Create a syslog source
        LogSourceConfig syslogSource = LogSourceConfig.createSyslogSource(
                "syslog-source",
                "Syslog Source",
                514,
                "UDP",
                "RFC5424",
                true
        );
        
        // Create the source
        LogSourceConfig createdSource = logSourceService.createSource(syslogSource);
        
        // Verify the result
        assertNotNull(createdSource);
        assertEquals("syslog-source", createdSource.getId());
        assertEquals(LogSourceConfig.SourceType.SYSLOG, createdSource.getSourceType());
        
        // Verify that the legacy config was not saved
        verify(legacyConfigRepository, never()).save(any(LogDirectoryConfig.class));
        
        // Verify that the source was started
        verify(syslogServer).startListener(any(LogSourceConfig.class));
    }
    
    @Test
    public void testCreateHttpSource() {
        // Configure mocks
        when(httpLogController.registerHttpSource(any(LogSourceConfig.class))).thenReturn(true);
        
        // Create an HTTP source
        LogSourceConfig httpSource = LogSourceConfig.createHttpSource(
                "http-source",
                "HTTP Source",
                "/api/logs/http-source",
                "test-token",
                true,
                true
        );
        
        // Create the source
        LogSourceConfig createdSource = logSourceService.createSource(httpSource);
        
        // Verify the result
        assertNotNull(createdSource);
        assertEquals("http-source", createdSource.getId());
        assertEquals(LogSourceConfig.SourceType.HTTP, createdSource.getSourceType());
        
        // Verify that the legacy config was not saved
        verify(legacyConfigRepository, never()).save(any(LogDirectoryConfig.class));
        
        // Verify that the source was started
        verify(httpLogController).registerHttpSource(any(LogSourceConfig.class));
    }
    
    @Test
    public void testUpdateFileSource() {
        // Configure mocks
        when(logScannerService.saveConfig(any(LogDirectoryConfig.class))).thenReturn(new LogDirectoryConfig());
        when(logScannerService.scanDirectory(any(LogDirectoryConfig.class))).thenReturn(0);
        
        // Create a file source
        LogSourceConfig fileSource = LogSourceConfig.createFileSource(
                "file-source",
                "File Source",
                "/var/log",
                "*.log",
                60,
                true
        );
        
        // Create the source
        logSourceService.createSource(fileSource);
        
        // Reset mocks
        reset(logScannerService, legacyConfigRepository);
        when(logScannerService.saveConfig(any(LogDirectoryConfig.class))).thenReturn(new LogDirectoryConfig());
        when(logScannerService.scanDirectory(any(LogDirectoryConfig.class))).thenReturn(0);
        
        // Update the source
        fileSource.setFilePattern("*.txt");
        LogSourceConfig updatedSource = logSourceService.updateSource("file-source", fileSource);
        
        // Verify the result
        assertNotNull(updatedSource);
        assertEquals("file-source", updatedSource.getId());
        assertEquals("*.txt", updatedSource.getFilePattern());
        
        // Verify that the legacy config was updated
        verify(legacyConfigRepository).save(any(LogDirectoryConfig.class));
        
        // Verify that the source was restarted
        verify(logScannerService).saveConfig(any(LogDirectoryConfig.class));
        verify(logScannerService).scanDirectory(any(LogDirectoryConfig.class));
    }
    
    @Test
    public void testUpdateSyslogSource() {
        // Configure mocks
        when(syslogServer.startListener(any(LogSourceConfig.class))).thenReturn(true);
        when(syslogServer.stopListener(anyString())).thenReturn(true);
        
        // Create a syslog source
        LogSourceConfig syslogSource = LogSourceConfig.createSyslogSource(
                "syslog-source",
                "Syslog Source",
                514,
                "UDP",
                "RFC5424",
                true
        );
        
        // Create the source
        logSourceService.createSource(syslogSource);
        
        // Reset mocks
        reset(syslogServer);
        when(syslogServer.startListener(any(LogSourceConfig.class))).thenReturn(true);
        when(syslogServer.stopListener(anyString())).thenReturn(true);
        
        // Update the source
        syslogSource.setSyslogPort(1514);
        LogSourceConfig updatedSource = logSourceService.updateSource("syslog-source", syslogSource);
        
        // Verify the result
        assertNotNull(updatedSource);
        assertEquals("syslog-source", updatedSource.getId());
        assertEquals(1514, updatedSource.getSyslogPort());
        
        // Verify that the source was restarted
        verify(syslogServer).stopListener("syslog-source");
        verify(syslogServer).startListener(any(LogSourceConfig.class));
    }
    
    @Test
    public void testUpdateHttpSource() {
        // Configure mocks
        when(httpLogController.registerHttpSource(any(LogSourceConfig.class))).thenReturn(true);
        when(httpLogController.unregisterHttpSource(anyString())).thenReturn(true);
        
        // Create an HTTP source
        LogSourceConfig httpSource = LogSourceConfig.createHttpSource(
                "http-source",
                "HTTP Source",
                "/api/logs/http-source",
                "test-token",
                true,
                true
        );
        
        // Create the source
        logSourceService.createSource(httpSource);
        
        // Reset mocks
        reset(httpLogController);
        when(httpLogController.registerHttpSource(any(LogSourceConfig.class))).thenReturn(true);
        when(httpLogController.unregisterHttpSource(anyString())).thenReturn(true);
        
        // Update the source
        httpSource.setHttpAuthToken("new-token");
        LogSourceConfig updatedSource = logSourceService.updateSource("http-source", httpSource);
        
        // Verify the result
        assertNotNull(updatedSource);
        assertEquals("http-source", updatedSource.getId());
        assertEquals("new-token", updatedSource.getHttpAuthToken());
        
        // Verify that the source was restarted
        verify(httpLogController).unregisterHttpSource("http-source");
        verify(httpLogController).registerHttpSource(any(LogSourceConfig.class));
    }
    
    @Test
    public void testDeleteFileSource() {
        // Configure mocks
        when(logScannerService.saveConfig(any(LogDirectoryConfig.class))).thenReturn(new LogDirectoryConfig());
        when(logScannerService.scanDirectory(any(LogDirectoryConfig.class))).thenReturn(0);
        
        // Create a file source
        LogSourceConfig fileSource = LogSourceConfig.createFileSource(
                "file-source",
                "File Source",
                "/var/log",
                "*.log",
                60,
                true
        );
        
        // Create the source
        logSourceService.createSource(fileSource);
        
        // Reset mocks
        reset(logScannerService, legacyConfigRepository);
        when(legacyConfigRepository.deleteById(anyString())).thenReturn(true);
        
        // Delete the source
        boolean deleted = logSourceService.deleteSource("file-source");
        
        // Verify the result
        assertTrue(deleted);
        
        // Verify that the legacy config was deleted
        verify(legacyConfigRepository).deleteById("file-source");
    }
    
    @Test
    public void testDeleteSyslogSource() {
        // Configure mocks
        when(syslogServer.startListener(any(LogSourceConfig.class))).thenReturn(true);
        when(syslogServer.stopListener(anyString())).thenReturn(true);
        
        // Create a syslog source
        LogSourceConfig syslogSource = LogSourceConfig.createSyslogSource(
                "syslog-source",
                "Syslog Source",
                514,
                "UDP",
                "RFC5424",
                true
        );
        
        // Create the source
        logSourceService.createSource(syslogSource);
        
        // Reset mocks
        reset(syslogServer);
        when(syslogServer.stopListener(anyString())).thenReturn(true);
        
        // Delete the source
        boolean deleted = logSourceService.deleteSource("syslog-source");
        
        // Verify the result
        assertTrue(deleted);
        
        // Verify that the source was stopped
        verify(syslogServer).stopListener("syslog-source");
    }
    
    @Test
    public void testDeleteHttpSource() {
        // Configure mocks
        when(httpLogController.registerHttpSource(any(LogSourceConfig.class))).thenReturn(true);
        when(httpLogController.unregisterHttpSource(anyString())).thenReturn(true);
        
        // Create an HTTP source
        LogSourceConfig httpSource = LogSourceConfig.createHttpSource(
                "http-source",
                "HTTP Source",
                "/api/logs/http-source",
                "test-token",
                true,
                true
        );
        
        // Create the source
        logSourceService.createSource(httpSource);
        
        // Reset mocks
        reset(httpLogController);
        when(httpLogController.unregisterHttpSource(anyString())).thenReturn(true);
        
        // Delete the source
        boolean deleted = logSourceService.deleteSource("http-source");
        
        // Verify the result
        assertTrue(deleted);
        
        // Verify that the source was stopped
        verify(httpLogController).unregisterHttpSource("http-source");
    }
    
    @Test
    public void testGetActiveSourceCounts() {
        // Configure mocks
        when(logScannerService.saveConfig(any(LogDirectoryConfig.class))).thenReturn(new LogDirectoryConfig());
        when(syslogServer.startListener(any(LogSourceConfig.class))).thenReturn(true);
        when(httpLogController.registerHttpSource(any(LogSourceConfig.class))).thenReturn(true);
        
        // Create sources of different types
        LogSourceConfig fileSource = LogSourceConfig.createFileSource(
                "file-source",
                "File Source",
                "/var/log",
                "*.log",
                60,
                true
        );
        
        LogSourceConfig syslogSource = LogSourceConfig.createSyslogSource(
                "syslog-source",
                "Syslog Source",
                514,
                "UDP",
                "RFC5424",
                true
        );
        
        LogSourceConfig httpSource = LogSourceConfig.createHttpSource(
                "http-source",
                "HTTP Source",
                "/api/logs/http-source",
                "test-token",
                true,
                true
        );
        
        // Create the sources
        logSourceService.createSource(fileSource);
        logSourceService.createSource(syslogSource);
        logSourceService.createSource(httpSource);
        
        // Get active source counts
        Map<LogSourceConfig.SourceType, Integer> counts = logSourceService.getActiveSourceCounts();
        
        // Verify the result
        assertEquals(3, logSourceService.getTotalActiveSourceCount());
        assertEquals(1, counts.get(LogSourceConfig.SourceType.FILE));
        assertEquals(1, counts.get(LogSourceConfig.SourceType.SYSLOG));
        assertEquals(1, counts.get(LogSourceConfig.SourceType.HTTP));
    }
    
    @Test
    public void testLoadLegacyConfigurations() {
        // Create legacy configs
        List<LogDirectoryConfig> legacyConfigs = new ArrayList<>();
        
        LogDirectoryConfig legacyConfig1 = new LogDirectoryConfig();
        legacyConfig1.setId("legacy-1");
        legacyConfig1.setDirectoryPath("/var/log/1");
        legacyConfig1.setEnabled(true);
        legacyConfigs.add(legacyConfig1);
        
        LogDirectoryConfig legacyConfig2 = new LogDirectoryConfig();
        legacyConfig2.setId("legacy-2");
        legacyConfig2.setDirectoryPath("/var/log/2");
        legacyConfig2.setEnabled(false);
        legacyConfigs.add(legacyConfig2);
        
        // Configure mocks
        when(legacyConfigRepository.findAll()).thenReturn(legacyConfigs);
        when(logScannerService.saveConfig(any(LogDirectoryConfig.class))).thenReturn(new LogDirectoryConfig());
        when(logScannerService.scanDirectory(any(LogDirectoryConfig.class))).thenReturn(0);
        
        // Create a new service to trigger loading of legacy configs
        LogSourceService service = new LogSourceService(
                logScannerService,
                syslogServer,
                httpLogController,
                cloudWatchLogService,
                legacyConfigRepository
        );
        
        // Verify that the legacy configs were loaded
        List<LogSourceConfig> sources = service.getAllSources();
        assertEquals(2, sources.size());
        
        // Verify that the enabled source was started
        verify(logScannerService).scanDirectory(any(LogDirectoryConfig.class));
        
        // Verify that only one source was started (the enabled one)
        verify(logScannerService, times(1)).scanDirectory(any(LogDirectoryConfig.class));
    }
}