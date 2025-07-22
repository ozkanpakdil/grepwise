package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.config.TestConfig;
import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.repository.AlarmRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Integration test for SystemHealthMonitoringService.
 * This test verifies that the service works correctly with the AlarmService.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
public class SystemHealthMonitoringIntegrationTest {

    @Autowired
    private SystemHealthMonitoringService systemHealthMonitoringService;

    @Autowired
    private AlarmService alarmService;

    @MockBean
    private AlarmRepository alarmRepository;

    @Test
    void testSystemHealthMonitoringIntegration() {
        // Mock repository to return alarms
        when(alarmRepository.findAll()).thenReturn(List.of());
        when(alarmRepository.save(org.mockito.ArgumentMatchers.any(Alarm.class)))
                .thenAnswer(invocation -> {
                    Alarm alarm = invocation.getArgument(0);
                    // Set an ID for the alarm
                    if (alarm.getId() == null) {
                        alarm.setId("test-" + System.currentTimeMillis());
                    }
                    return alarm;
                });

        // Initialize the service
        systemHealthMonitoringService.init();

        // Collect metrics
        systemHealthMonitoringService.collectSystemMetrics();

        // Get system metrics
        Map<String, Object> metrics = systemHealthMonitoringService.getSystemMetrics();

        // Verify metrics are collected
        assertNotNull(metrics.get("cpu.usage"));
        assertNotNull(metrics.get("memory.usage"));
        assertNotNull(metrics.get("disk.usage"));
        assertNotNull(metrics.get("health.status"));

        // Get all alarms
        List<Alarm> alarms = alarmService.getAllAlarms();

        // Verify system alarms are created
        List<String> alarmNames = alarms.stream()
                .map(Alarm::getName)
                .collect(Collectors.toList());

        assertTrue(alarmNames.contains("System CPU Usage Alert"));
        assertTrue(alarmNames.contains("System Memory Usage Alert"));
        assertTrue(alarmNames.contains("System Disk Usage Alert"));
        assertTrue(alarmNames.contains("System Health Check Alert"));

        // Update CPU threshold
        systemHealthMonitoringService.setCpuThreshold(95.0);

        // Verify CPU alarm is updated
        Alarm cpuAlarm = alarms.stream()
                .filter(a -> a.getName().equals("System CPU Usage Alert"))
                .findFirst()
                .orElse(null);

        assertNotNull(cpuAlarm);
        assertEquals("system.cpu.usage > 95.0", cpuAlarm.getQuery());
    }
}