package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.model.NotificationChannel;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SystemHealthMonitoringServiceTest {

    @Mock
    private AlarmService alarmService;

    @Mock
    private HealthEndpoint healthEndpoint;

    @Spy
    private MeterRegistry meterRegistry = new SimpleMeterRegistry();

    @InjectMocks
    private SystemHealthMonitoringService systemHealthMonitoringService;

    @BeforeEach
    void setUp() {
        // Mock health endpoint to return UP status
        Health health = Health.up().build();
        when(healthEndpoint.health()).thenReturn(health);

        // Mock AlarmService to return empty list of alarms
        when(alarmService.getAllAlarms()).thenReturn(new ArrayList<>());
        
        // Mock AlarmService to return the alarm when creating/updating
        when(alarmService.createAlarm(any(Alarm.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(alarmService.updateAlarm(any(Alarm.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testInit() {
        // Call init method
        systemHealthMonitoringService.init();

        // Verify that metrics are registered
        assertNotNull(meterRegistry.find("system.cpu.usage").gauge());
        assertNotNull(meterRegistry.find("system.memory.usage").gauge());
        assertNotNull(meterRegistry.find("system.disk.usage").gauge());

        // Verify that alarms are created
        verify(alarmService, times(4)).createAlarm(any(Alarm.class));
    }

    @Test
    void testCreateSystemAlarm() {
        // Call init method to create alarms
        systemHealthMonitoringService.init();

        // Capture the created alarms
        ArgumentCaptor<Alarm> alarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService, times(4)).createAlarm(alarmCaptor.capture());

        // Get the captured alarms
        List<Alarm> createdAlarms = alarmCaptor.getAllValues();

        // Verify CPU alarm
        Alarm cpuAlarm = createdAlarms.stream()
                .filter(a -> a.getName().equals("System CPU Usage Alert"))
                .findFirst()
                .orElse(null);
        assertNotNull(cpuAlarm);
        assertEquals("Alerts when CPU usage exceeds threshold", cpuAlarm.getDescription());
        assertEquals("system.cpu.usage > 80.0", cpuAlarm.getQuery());
        assertEquals("count > 0", cpuAlarm.getCondition());
        assertEquals(1, cpuAlarm.getThreshold());
        assertEquals(5, cpuAlarm.getTimeWindowMinutes());
        assertEquals(30, cpuAlarm.getThrottleWindowMinutes());
        assertEquals(3, cpuAlarm.getMaxNotificationsPerWindow());
        assertEquals("system-health", cpuAlarm.getGroupingKey());
        assertTrue(cpuAlarm.getEnabled());

        // Verify Memory alarm
        Alarm memoryAlarm = createdAlarms.stream()
                .filter(a -> a.getName().equals("System Memory Usage Alert"))
                .findFirst()
                .orElse(null);
        assertNotNull(memoryAlarm);
        assertEquals("system.memory.usage > 80.0", memoryAlarm.getQuery());

        // Verify Disk alarm
        Alarm diskAlarm = createdAlarms.stream()
                .filter(a -> a.getName().equals("System Disk Usage Alert"))
                .findFirst()
                .orElse(null);
        assertNotNull(diskAlarm);
        assertEquals("system.disk.usage > 90.0", diskAlarm.getQuery());

        // Verify Health alarm
        Alarm healthAlarm = createdAlarms.stream()
                .filter(a -> a.getName().equals("System Health Check Alert"))
                .findFirst()
                .orElse(null);
        assertNotNull(healthAlarm);
        assertEquals("system.health.status = DOWN", healthAlarm.getQuery());
    }

    @Test
    void testCollectSystemMetrics() {
        // Call collectSystemMetrics method
        systemHealthMonitoringService.collectSystemMetrics();

        // Get system metrics
        Map<String, Object> metrics = systemHealthMonitoringService.getSystemMetrics();

        // Verify metrics are collected
        assertNotNull(metrics.get("cpu.usage"));
        assertNotNull(metrics.get("memory.usage"));
        assertNotNull(metrics.get("disk.usage"));
        assertEquals("UP", metrics.get("health.status"));
    }

    @Test
    void testUpdateThresholds() {
        // Call init method to create alarms
        systemHealthMonitoringService.init();

        // Capture the created alarms
        ArgumentCaptor<Alarm> createAlarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService, times(4)).createAlarm(createAlarmCaptor.capture());

        // Get the CPU alarm
        Alarm cpuAlarm = createAlarmCaptor.getAllValues().stream()
                .filter(a -> a.getName().equals("System CPU Usage Alert"))
                .findFirst()
                .orElse(null);
        assertNotNull(cpuAlarm);

        // Reset mock to clear invocation count
        reset(alarmService);
        when(alarmService.updateAlarm(any(Alarm.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Update CPU threshold
        systemHealthMonitoringService.setCpuThreshold(90.0);

        // Verify that alarm is updated
        ArgumentCaptor<Alarm> updateAlarmCaptor = ArgumentCaptor.forClass(Alarm.class);
        verify(alarmService).updateAlarm(updateAlarmCaptor.capture());

        // Verify updated query
        Alarm updatedAlarm = updateAlarmCaptor.getValue();
        assertEquals("system.cpu.usage > 90.0", updatedAlarm.getQuery());
    }

    @Test
    void testGetHealthStatus() {
        // Mock health endpoint to return DOWN status
        Health downHealth = Health.down().build();
        when(healthEndpoint.health()).thenReturn(downHealth);

        // Check health status
        boolean healthStatus = systemHealthMonitoringService.getHealthStatus();
        assertFalse(healthStatus);

        // Mock health endpoint to return UP status
        Health upHealth = Health.up().build();
        when(healthEndpoint.health()).thenReturn(upHealth);

        // Check health status
        healthStatus = systemHealthMonitoringService.getHealthStatus();
        assertTrue(healthStatus);
    }

    @Test
    void testGetSystemMetrics() {
        // Call init and collect metrics
        systemHealthMonitoringService.init();
        systemHealthMonitoringService.collectSystemMetrics();

        // Get system metrics
        Map<String, Object> metrics = systemHealthMonitoringService.getSystemMetrics();

        // Verify metrics
        assertNotNull(metrics.get("cpu.usage"));
        assertNotNull(metrics.get("memory.usage"));
        assertNotNull(metrics.get("disk.usage"));
        assertNotNull(metrics.get("health.status"));
        assertEquals("80.00%", metrics.get("cpu.threshold"));
        assertEquals("80.00%", metrics.get("memory.threshold"));
        assertEquals("90.00%", metrics.get("disk.threshold"));
    }
}