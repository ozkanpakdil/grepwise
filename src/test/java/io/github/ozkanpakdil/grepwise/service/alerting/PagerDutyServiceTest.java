package io.github.ozkanpakdil.grepwise.service.alerting;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import io.github.ozkanpakdil.grepwise.model.NotificationChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PagerDutyServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private PagerDutyService pagerDutyService;

    private Alarm testAlarm;
    private final String testIntegrationKey = "test-integration-key";
    private final String testMessage = "Test alarm message";

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
        
        NotificationChannel pagerDutyChannel = new NotificationChannel("PAGERDUTY", testIntegrationKey);
        testAlarm.setNotificationChannels(Arrays.asList(pagerDutyChannel));
    }

    @Test
    void testSendAlert_Success() {
        // Mock successful response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", "success");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.ACCEPTED);
        
        when(restTemplate.postForEntity(
            eq("https://events.pagerduty.com/v2/enqueue"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(responseEntity);

        // Call the method
        boolean result = pagerDutyService.sendAlert(testAlarm, testIntegrationKey, testMessage);

        // Verify
        assertTrue(result);
        
        // Capture and verify the request payload
        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
            eq("https://events.pagerduty.com/v2/enqueue"),
            captor.capture(),
            eq(Map.class)
        );
        
        HttpEntity<?> requestEntity = captor.getValue();
        Map<String, Object> payload = (Map<String, Object>) requestEntity.getBody();
        
        assertEquals(testIntegrationKey, payload.get("routing_key"));
        assertEquals("trigger", payload.get("event_action"));
        
        Map<String, Object> payloadDetails = (Map<String, Object>) payload.get("payload");
        assertEquals("Alarm Triggered: " + testAlarm.getName(), payloadDetails.get("summary"));
        assertEquals("GrepWise", payloadDetails.get("source"));
        assertEquals("critical", payloadDetails.get("severity"));
        
        Map<String, Object> customDetails = (Map<String, Object>) payloadDetails.get("custom_details");
        assertEquals(testAlarm.getId(), customDetails.get("alarm_id"));
        assertEquals(testAlarm.getName(), customDetails.get("alarm_name"));
        assertEquals(testMessage, customDetails.get("full_message"));
    }

    @Test
    void testSendAlert_Failure() {
        // Mock error response
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        
        when(restTemplate.postForEntity(
            eq("https://events.pagerduty.com/v2/enqueue"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(responseEntity);

        // Call the method
        boolean result = pagerDutyService.sendAlert(testAlarm, testIntegrationKey, testMessage);

        // Verify
        assertFalse(result);
    }

    @Test
    void testSendAlert_Exception() {
        // Mock exception
        when(restTemplate.postForEntity(
            eq("https://events.pagerduty.com/v2/enqueue"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenThrow(new RuntimeException("API error"));

        // Call the method
        boolean result = pagerDutyService.sendAlert(testAlarm, testIntegrationKey, testMessage);

        // Verify
        assertFalse(result);
    }

    @Test
    void testSendGroupedAlert_Success() {
        // Mock successful response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", "success");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.ACCEPTED);
        
        when(restTemplate.postForEntity(
            eq("https://events.pagerduty.com/v2/enqueue"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(responseEntity);

        // Call the method
        String groupingKey = "test-group";
        int alarmCount = 3;
        boolean result = pagerDutyService.sendGroupedAlert(groupingKey, testIntegrationKey, testMessage, alarmCount);

        // Verify
        assertTrue(result);
        
        // Capture and verify the request payload
        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
            eq("https://events.pagerduty.com/v2/enqueue"),
            captor.capture(),
            eq(Map.class)
        );
        
        HttpEntity<?> requestEntity = captor.getValue();
        Map<String, Object> payload = (Map<String, Object>) requestEntity.getBody();
        
        assertEquals(testIntegrationKey, payload.get("routing_key"));
        assertEquals("trigger", payload.get("event_action"));
        
        Map<String, Object> payloadDetails = (Map<String, Object>) payload.get("payload");
        assertEquals("Grouped Alarms Triggered (3 alarms)", payloadDetails.get("summary"));
        assertEquals("GrepWise", payloadDetails.get("source"));
        
        Map<String, Object> customDetails = (Map<String, Object>) payloadDetails.get("custom_details");
        assertEquals(groupingKey, customDetails.get("grouping_key"));
        assertEquals(alarmCount, customDetails.get("alarm_count"));
        assertEquals(testMessage, customDetails.get("full_message"));
    }
}