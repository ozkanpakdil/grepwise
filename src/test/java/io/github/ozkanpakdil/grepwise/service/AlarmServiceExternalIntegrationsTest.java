package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.NotificationChannel;
import io.github.ozkanpakdil.grepwise.repository.AlarmRepository;
import io.github.ozkanpakdil.grepwise.service.alerting.OpsGenieService;
import io.github.ozkanpakdil.grepwise.service.alerting.PagerDutyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AlarmServiceExternalIntegrationsTest {

    @Mock
    private AlarmRepository alarmRepository;

    @Mock
    private LuceneService luceneService;

    @Mock
    private PagerDutyService pagerDutyService;

    @Mock
    private OpsGenieService opsGenieService;

    @Spy
    @InjectMocks
    private AlarmService alarmService;

    private Alarm testAlarm;
    private final String pagerDutyIntegrationKey = "test-pagerduty-key";
    private final String opsGenieApiKey = "test-opsgenie-key";

    @BeforeEach
    void setUp() {
        testAlarm = new Alarm();
        testAlarm.setId("test-alarm-id");
        testAlarm.setName("Test Alarm");
        testAlarm.setDescription("Test alarm description");
        testAlarm.setQuery("level:ERROR");
        testAlarm.setCondition("count > 5");
        testAlarm.setThreshold(5);
        testAlarm.setTimeWindowMinutes(15);
        testAlarm.setEnabled(true);
    }

    @Test
    void testPagerDutyNotification() throws IOException {
        // Set up PagerDuty notification channel
        NotificationChannel pagerDutyChannel = new NotificationChannel("PAGERDUTY", pagerDutyIntegrationKey);
        testAlarm.setNotificationChannels(Arrays.asList(pagerDutyChannel));

        // Mock LuceneService to return enough logs to trigger the alarm
        List<LogEntry> mockLogs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mockLogs.add(new LogEntry());
        }
        doReturn(mockLogs).when(luceneService).search(anyString(), anyBoolean(), anyLong(), anyLong());

        // Mock PagerDutyService to return success
        when(pagerDutyService.sendAlert(any(Alarm.class), anyString(), anyString())).thenReturn(true);

        // Call the method that would trigger notifications
        alarmService.evaluateAlarm(testAlarm);

        // Use ReflectionTestUtils to call the private method directly
        ReflectionTestUtils.invokeMethod(alarmService, "triggerNotifications", testAlarm);

        // Verify PagerDutyService was called with correct parameters
        verify(pagerDutyService).sendAlert(eq(testAlarm), eq(pagerDutyIntegrationKey), anyString());
    }

    @Test
    void testOpsGenieNotification() throws IOException {
        // Set up OpsGenie notification channel
        NotificationChannel opsGenieChannel = new NotificationChannel("OPSGENIE", opsGenieApiKey);
        testAlarm.setNotificationChannels(Arrays.asList(opsGenieChannel));

        // Mock LuceneService to return enough logs to trigger the alarm
        List<LogEntry> mockLogs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mockLogs.add(new LogEntry());
        }
        doReturn(mockLogs).when(luceneService).search(anyString(), anyBoolean(), anyLong(), anyLong());

        // Mock OpsGenieService to return success
        when(opsGenieService.sendAlert(any(Alarm.class), anyString(), anyString())).thenReturn(true);

        // Call the method that would trigger notifications
        alarmService.evaluateAlarm(testAlarm);

        // Use ReflectionTestUtils to call the private method directly
        ReflectionTestUtils.invokeMethod(alarmService, "triggerNotifications", testAlarm);

        // Verify OpsGenieService was called with correct parameters
        verify(opsGenieService).sendAlert(eq(testAlarm), eq(opsGenieApiKey), anyString());
    }

    @Test
    void testMultipleNotificationChannels() throws IOException {
        // Set up multiple notification channels
        NotificationChannel pagerDutyChannel = new NotificationChannel("PAGERDUTY", pagerDutyIntegrationKey);
        NotificationChannel opsGenieChannel = new NotificationChannel("OPSGENIE", opsGenieApiKey);
        NotificationChannel emailChannel = new NotificationChannel("EMAIL", "test@example.com");
        testAlarm.setNotificationChannels(Arrays.asList(pagerDutyChannel, opsGenieChannel, emailChannel));

        // Mock LuceneService to return enough logs to trigger the alarm
        List<LogEntry> mockLogs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            mockLogs.add(new LogEntry());
        }
        doReturn(mockLogs).when(luceneService).search(anyString(), anyBoolean(), anyLong(), anyLong());

        // Mock services to return success
        when(pagerDutyService.sendAlert(any(Alarm.class), anyString(), anyString())).thenReturn(true);
        when(opsGenieService.sendAlert(any(Alarm.class), anyString(), anyString())).thenReturn(true);

        // Call the method that would trigger notifications
        alarmService.evaluateAlarm(testAlarm);

        // Use ReflectionTestUtils to call the private method directly
        ReflectionTestUtils.invokeMethod(alarmService, "triggerNotifications", testAlarm);

        // Verify both services were called with correct parameters
        verify(pagerDutyService).sendAlert(eq(testAlarm), eq(pagerDutyIntegrationKey), anyString());
        verify(opsGenieService).sendAlert(eq(testAlarm), eq(opsGenieApiKey), anyString());
    }

    private Alarm createTestAlarm(String name, String channelType, String destination) {
        Alarm alarm = new Alarm();
        alarm.setId("test-alarm-id-" + name);
        alarm.setName(name);
        alarm.setDescription("Test alarm description: " + name);
        alarm.setQuery("level:ERROR");
        alarm.setCondition("count > 5");
        alarm.setThreshold(5);
        alarm.setTimeWindowMinutes(15);
        alarm.setEnabled(true);
        
        NotificationChannel channel = new NotificationChannel(channelType, destination);
        alarm.setNotificationChannels(Arrays.asList(channel));
        
        return alarm;
    }
}