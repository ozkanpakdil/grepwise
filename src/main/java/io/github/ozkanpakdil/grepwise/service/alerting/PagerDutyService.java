package io.github.ozkanpakdil.grepwise.service.alerting;

import io.github.ozkanpakdil.grepwise.model.Alarm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Service for sending alerts to PagerDuty.
 */
@Service
public class PagerDutyService {
    private static final Logger logger = LoggerFactory.getLogger(PagerDutyService.class);
    private static final String PAGERDUTY_EVENTS_API_URL = "https://events.pagerduty.com/v2/enqueue";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Send an alert to PagerDuty.
     *
     * @param alarm The alarm that was triggered
     * @param integrationKey The PagerDuty integration key (routing key)
     * @param message The alert message
     * @return true if the alert was sent successfully, false otherwise
     */
    public boolean sendAlert(Alarm alarm, String integrationKey, String message) {
        try {
            // Create the payload according to PagerDuty Events API v2
            Map<String, Object> payload = new HashMap<>();
            payload.put("routing_key", integrationKey);
            payload.put("event_action", "trigger");
            
            Map<String, Object> payload_details = new HashMap<>();
            payload_details.put("summary", "Alarm Triggered: " + alarm.getName());
            payload_details.put("source", "GrepWise");
            payload_details.put("severity", "critical");
            payload_details.put("component", "Log Monitoring");
            
            Map<String, Object> custom_details = new HashMap<>();
            custom_details.put("alarm_id", alarm.getId());
            custom_details.put("alarm_name", alarm.getName());
            custom_details.put("description", alarm.getDescription());
            custom_details.put("query", alarm.getQuery());
            custom_details.put("condition", alarm.getCondition());
            custom_details.put("threshold", alarm.getThreshold());
            custom_details.put("time_window_minutes", alarm.getTimeWindowMinutes());
            custom_details.put("full_message", message);
            
            payload_details.put("custom_details", custom_details);
            payload.put("payload", payload_details);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create the request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            // Send the request
            ResponseEntity<Map> response = restTemplate.postForEntity(
                PAGERDUTY_EVENTS_API_URL, 
                requestEntity, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent alert to PagerDuty for alarm: {}", alarm.getName());
                return true;
            } else {
                logger.error("Failed to send alert to PagerDuty. Status code: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending alert to PagerDuty: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send a grouped alert to PagerDuty.
     *
     * @param groupingKey The grouping key
     * @param integrationKey The PagerDuty integration key (routing key)
     * @param message The grouped alert message
     * @param alarmCount The number of alarms in the group
     * @return true if the alert was sent successfully, false otherwise
     */
    public boolean sendGroupedAlert(String groupingKey, String integrationKey, String message, int alarmCount) {
        try {
            // Create the payload according to PagerDuty Events API v2
            Map<String, Object> payload = new HashMap<>();
            payload.put("routing_key", integrationKey);
            payload.put("event_action", "trigger");
            
            Map<String, Object> payload_details = new HashMap<>();
            payload_details.put("summary", "Grouped Alarms Triggered (" + alarmCount + " alarms)");
            payload_details.put("source", "GrepWise");
            payload_details.put("severity", "critical");
            payload_details.put("component", "Log Monitoring");
            
            Map<String, Object> custom_details = new HashMap<>();
            custom_details.put("grouping_key", groupingKey);
            custom_details.put("alarm_count", alarmCount);
            custom_details.put("full_message", message);
            
            payload_details.put("custom_details", custom_details);
            payload.put("payload", payload_details);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            // Create the request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            // Send the request
            ResponseEntity<Map> response = restTemplate.postForEntity(
                PAGERDUTY_EVENTS_API_URL, 
                requestEntity, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent grouped alert to PagerDuty for grouping key: {}", groupingKey);
                return true;
            } else {
                logger.error("Failed to send grouped alert to PagerDuty. Status code: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending grouped alert to PagerDuty: {}", e.getMessage(), e);
            return false;
        }
    }
}