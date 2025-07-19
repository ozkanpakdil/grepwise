package io.github.ozkanpakdil.grepwise.health;

import io.github.ozkanpakdil.grepwise.model.LogDirectoryConfig;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.service.LogBufferService;
import io.github.ozkanpakdil.grepwise.service.LogScannerService;
import io.github.ozkanpakdil.grepwise.service.LuceneService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class HealthIndicatorsTest {

    @Mock
    private LuceneService luceneService;

    @Mock
    private LogScannerService logScannerService;

    @Mock
    private LogBufferService logBufferService;

    private LuceneHealthIndicator luceneHealthIndicator;
    private LogScannerHealthIndicator logScannerHealthIndicator;
    private LogBufferHealthIndicator logBufferHealthIndicator;

    @BeforeEach
    void setUp() {
        luceneHealthIndicator = new LuceneHealthIndicator(luceneService);
        logScannerHealthIndicator = new LogScannerHealthIndicator(logScannerService);
        logBufferHealthIndicator = new LogBufferHealthIndicator(logBufferService);
    }

    @Test
    void testLuceneHealthIndicator_Up() throws IOException {
        // Arrange
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        Health health = luceneHealthIndicator.health();

        // Assert
        assertEquals(Status.UP, health.getStatus());
        assertEquals("Lucene index is operational", health.getDetails().get("status"));
    }

    @Test
    void testLuceneHealthIndicator_Down() throws IOException {
        // Arrange
        when(luceneService.search(anyString(), anyBoolean(), any(), any()))
                .thenThrow(new IOException("Test exception"));

        // Act
        Health health = luceneHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Lucene index is not operational", health.getDetails().get("status"));
        assertEquals("Test exception", health.getDetails().get("error"));
    }

    @Test
    void testLogScannerHealthIndicator_Up_WithAccessibleDirectories() {
        // Arrange
        List<LogDirectoryConfig> configs = new ArrayList<>();
        LogDirectoryConfig config = new LogDirectoryConfig();
        config.setDirectoryPath(System.getProperty("java.io.tmpdir")); // Use temp dir which should exist
        configs.add(config);
        
        when(logScannerService.getAllConfigs()).thenReturn(configs);

        // Act
        Health health = logScannerHealthIndicator.health();

        // Assert
        assertEquals(Status.UP, health.getStatus());
        assertEquals("Log scanner is operational", health.getDetails().get("status"));
        assertEquals(1, health.getDetails().get("configuredDirectories"));
        assertEquals(1, health.getDetails().get("accessibleDirectories"));
    }

    @Test
    void testLogScannerHealthIndicator_Up_WithNoDirectories() {
        // Arrange
        when(logScannerService.getAllConfigs()).thenReturn(Collections.emptyList());

        // Act
        Health health = logScannerHealthIndicator.health();

        // Assert
        assertEquals(Status.UP, health.getStatus());
        assertEquals("Log scanner is operational but no directories are configured", health.getDetails().get("status"));
    }

    @Test
    void testLogScannerHealthIndicator_Down_WithInaccessibleDirectories() {
        // Arrange
        List<LogDirectoryConfig> configs = new ArrayList<>();
        LogDirectoryConfig config = new LogDirectoryConfig();
        config.setDirectoryPath("/non/existent/directory"); // This directory should not exist
        configs.add(config);
        
        when(logScannerService.getAllConfigs()).thenReturn(configs);

        // Act
        Health health = logScannerHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Some log directories are not accessible", health.getDetails().get("status"));
        assertEquals(1, health.getDetails().get("configuredDirectories"));
        assertEquals(0, health.getDetails().get("accessibleDirectories"));
    }

    @Test
    void testLogBufferHealthIndicator_Up() {
        // Arrange
        when(logBufferService.getBufferSize()).thenReturn(100);
        when(logBufferService.getMaxBufferSize()).thenReturn(1000);
        when(logBufferService.getFlushIntervalMs()).thenReturn(30000);

        // Act
        Health health = logBufferHealthIndicator.health();

        // Assert
        assertEquals(Status.UP, health.getStatus());
        assertEquals("Log buffer is operational", health.getDetails().get("status"));
        assertEquals(100, health.getDetails().get("currentSize"));
        assertEquals(1000, health.getDetails().get("maxSize"));
        assertEquals("10.00%", health.getDetails().get("utilization"));
        assertEquals(30000, health.getDetails().get("flushIntervalMs"));
    }

    @Test
    void testLogBufferHealthIndicator_Down_NearCapacity() {
        // Arrange
        when(logBufferService.getBufferSize()).thenReturn(850);
        when(logBufferService.getMaxBufferSize()).thenReturn(1000);
        when(logBufferService.getFlushIntervalMs()).thenReturn(30000);

        // Act
        Health health = logBufferHealthIndicator.health();

        // Assert
        assertEquals(Status.DOWN, health.getStatus());
        assertEquals("Buffer is near capacity", health.getDetails().get("status"));
        assertEquals(850, health.getDetails().get("currentSize"));
        assertEquals(1000, health.getDetails().get("maxSize"));
        assertEquals("85.00%", health.getDetails().get("utilization"));
        assertEquals(30000, health.getDetails().get("flushIntervalMs"));
    }
}