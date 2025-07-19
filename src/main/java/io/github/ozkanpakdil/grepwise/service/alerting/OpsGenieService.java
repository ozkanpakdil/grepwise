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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for sending alerts to OpsGenie.
 */
@Service
public class OpsGenieService {
    private static final Logger logger = LoggerFactory.getLogger(OpsGenieService.class);
    private static final String OPSGENIE_ALERT_API_URL = "https://api.opsgenie.com/v2/alerts";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * Send an alert to OpsGenie.
     *
     * @param alarm The alarm that was triggered
     * @param apiKey The OpsGenie API key
     * @param message The alert message
     * @return true if the alert was sent successfully, false otherwise
     */
    public boolean sendAlert(Alarm alarm, String apiKey, String message) {
        try {
            // Create the payload according to OpsGenie Alert API v2
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", "Alarm Triggered: " + alarm.getName());
            payload.put("description", message);
            payload.put("priority", "P1");
            payload.put("source", "GrepWise");
            
            // Add details
            Map<String, Object> details = new HashMap<>();
            details.put("alarm_id", alarm.getId());
            details.put("alarm_name", alarm.getName());
            details.put("description", alarm.getDescription());
            details.put("query", alarm.getQuery());
            details.put("condition", alarm.getCondition());
            details.put("threshold", alarm.getThreshold());
            details.put("time_window_minutes", alarm.getTimeWindowMinutes());
            payload.put("details", details);
            
            // Add tags
            List<String> tags = new ArrayList<>();
            tags.add("GrepWise");
            tags.add("LogMonitoring");
            tags.add("Alarm");
            payload.put("tags", tags);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "GenieKey " + apiKey);
            
            // Create the request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            // Send the request
            ResponseEntity<Map> response = restTemplate.postForEntity(
                OPSGENIE_ALERT_API_URL, 
                requestEntity, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent alert to OpsGenie for alarm: {}", alarm.getName());
                return true;
            } else {
                logger.error("Failed to send alert to OpsGenie. Status code: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending alert to OpsGenie: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Send a grouped alert to OpsGenie.
     *
     * @param groupingKey The grouping key
     * @param apiKey The OpsGenie API key
     * @param message The grouped alert message
     * @param alarmCount The number of alarms in the group
     * @return true if the alert was sent successfully, false otherwise
     */
    public boolean sendGroupedAlert(String groupingKey, String apiKey, String message, int alarmCount) {
        try {
            // Create the payload according to OpsGenie Alert API v2
            Map<String, Object> payload = new HashMap<>();
            payload.put("message", "Grouped Alarms Triggered (" + alarmCount + " alarms)");
            payload.put("description", message);
            payload.put("priority", "P1");
            payload.put("source", "GrepWise");
            
            // Add details
            Map<String, Object> details = new HashMap<>();
            details.put("grouping_key", groupingKey);
            details.put("alarm_count", alarmCount);
            payload.put("details", details);
            
            // Add tags
            List<String> tags = new ArrayList<>();
            tags.add("GrepWise");
            tags.add("LogMonitoring");
            tags.add("GroupedAlarm");
            payload.put("tags", tags);
            
            // Set alias for deduplication
            payload.put("alias", "GrepWise-GroupedAlarm-" + groupingKey);
            
            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "GenieKey " + apiKey);
            
            // Create the request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            // Send the request
            ResponseEntity<Map> response = restTemplate.postForEntity(
                OPSGENIE_ALERT_API_URL, 
                requestEntity, 
                Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully sent grouped alert to OpsGenie for grouping key: {}", groupingKey);
                return true;
            } else {
                logger.error("Failed to send grouped alert to OpsGenie. Status code: {}", response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            logger.error("Error sending grouped alert to OpsGenie: {}", e.getMessage(), e);
            return false;
        }
    }
}