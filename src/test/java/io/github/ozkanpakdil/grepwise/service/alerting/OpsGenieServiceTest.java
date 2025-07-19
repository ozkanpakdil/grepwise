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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OpsGenieServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private OpsGenieService opsGenieService;

    private Alarm testAlarm;
    private final String testApiKey = "test-api-key";
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
        
        NotificationChannel opsGenieChannel = new NotificationChannel("OPSGENIE", testApiKey);
        testAlarm.setNotificationChannels(Arrays.asList(opsGenieChannel));
    }

    @Test
    void testSendAlert_Success() {
        // Mock successful response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("result", "success");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.ACCEPTED);
        
        when(restTemplate.postForEntity(
            eq("https://api.opsgenie.com/v2/alerts"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(responseEntity);

        // Call the method
        boolean result = opsGenieService.sendAlert(testAlarm, testApiKey, testMessage);

        // Verify
        assertTrue(result);
        
        // Capture and verify the request payload and headers
        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
            eq("https://api.opsgenie.com/v2/alerts"),
            captor.capture(),
            eq(Map.class)
        );
        
        HttpEntity<?> requestEntity = captor.getValue();
        Map<String, Object> payload = (Map<String, Object>) requestEntity.getBody();
        HttpHeaders headers = requestEntity.getHeaders();
        
        // Verify headers
        assertEquals("GenieKey " + testApiKey, headers.getFirst("Authorization"));
        
        // Verify payload
        assertEquals("Alarm Triggered: " + testAlarm.getName(), payload.get("message"));
        assertEquals(testMessage, payload.get("description"));
        assertEquals("P1", payload.get("priority"));
        assertEquals("GrepWise", payload.get("source"));
        
        // Verify details
        Map<String, Object> details = (Map<String, Object>) payload.get("details");
        assertEquals(testAlarm.getId(), details.get("alarm_id"));
        assertEquals(testAlarm.getName(), details.get("alarm_name"));
        assertEquals(testAlarm.getQuery(), details.get("query"));
        
        // Verify tags
        List<String> tags = (List<String>) payload.get("tags");
        assertTrue(tags.contains("GrepWise"));
        assertTrue(tags.contains("LogMonitoring"));
        assertTrue(tags.contains("Alarm"));
    }

    @Test
    void testSendAlert_Failure() {
        // Mock error response
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        
        when(restTemplate.postForEntity(
            eq("https://api.opsgenie.com/v2/alerts"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(responseEntity);

        // Call the method
        boolean result = opsGenieService.sendAlert(testAlarm, testApiKey, testMessage);

        // Verify
        assertFalse(result);
    }

    @Test
    void testSendAlert_Exception() {
        // Mock exception
        when(restTemplate.postForEntity(
            eq("https://api.opsgenie.com/v2/alerts"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenThrow(new RuntimeException("API error"));

        // Call the method
        boolean result = opsGenieService.sendAlert(testAlarm, testApiKey, testMessage);

        // Verify
        assertFalse(result);
    }

    @Test
    void testSendGroupedAlert_Success() {
        // Mock successful response
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("result", "success");
        ResponseEntity<Map> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.ACCEPTED);
        
        when(restTemplate.postForEntity(
            eq("https://api.opsgenie.com/v2/alerts"),
            any(HttpEntity.class),
            eq(Map.class)
        )).thenReturn(responseEntity);

        // Call the method
        String groupingKey = "test-group";
        int alarmCount = 3;
        boolean result = opsGenieService.sendGroupedAlert(groupingKey, testApiKey, testMessage, alarmCount);

        // Verify
        assertTrue(result);
        
        // Capture and verify the request payload and headers
        ArgumentCaptor<HttpEntity<?>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
            eq("https://api.opsgenie.com/v2/alerts"),
            captor.capture(),
            eq(Map.class)
        );
        
        HttpEntity<?> requestEntity = captor.getValue();
        Map<String, Object> payload = (Map<String, Object>) requestEntity.getBody();
        HttpHeaders headers = requestEntity.getHeaders();
        
        // Verify headers
        assertEquals("GenieKey " + testApiKey, headers.getFirst("Authorization"));
        
        // Verify payload
        assertEquals("Grouped Alarms Triggered (" + alarmCount + " alarms)", payload.get("message"));
        assertEquals(testMessage, payload.get("description"));
        assertEquals("P1", payload.get("priority"));
        assertEquals("GrepWise", payload.get("source"));
        
        // Verify details
        Map<String, Object> details = (Map<String, Object>) payload.get("details");
        assertEquals(groupingKey, details.get("grouping_key"));
        assertEquals(alarmCount, details.get("alarm_count"));
        
        // Verify tags
        List<String> tags = (List<String>) payload.get("tags");
        assertTrue(tags.contains("GrepWise"));
        assertTrue(tags.contains("LogMonitoring"));
        assertTrue(tags.contains("GroupedAlarm"));
        
        // Verify alias for deduplication
        assertEquals("GrepWise-GroupedAlarm-" + groupingKey, payload.get("alias"));
    }
}