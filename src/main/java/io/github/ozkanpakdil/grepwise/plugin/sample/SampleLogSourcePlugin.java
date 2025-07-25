package io.github.ozkanpakdil.grepwise.plugin.sample;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.plugin.AbstractPlugin;
import io.github.ozkanpakdil.grepwise.plugin.LogSourcePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * A sample implementation of the LogSourcePlugin interface for testing purposes.
 * This plugin generates random log entries with various log levels and messages.
 */
public class SampleLogSourcePlugin extends AbstractPlugin implements LogSourcePlugin {

    private static final Logger logger = LoggerFactory.getLogger(SampleLogSourcePlugin.class);
    
    private static final String[] LOG_LEVELS = {"INFO", "WARN", "ERROR", "DEBUG", "TRACE"};
    private static final String[] LOG_MESSAGES = {
            "User logged in successfully",
            "Failed login attempt",
            "Database connection established",
            "Database query executed in {0}ms",
            "API request received: {0}",
            "API response sent: status={0}",
            "File not found: {0}",
            "Permission denied for user {0}",
            "Memory usage: {0}MB",
            "CPU usage: {0}%",
            "Disk space remaining: {0}GB",
            "Network traffic: {0} requests/sec",
            "Cache hit ratio: {0}%",
            "Thread pool size: {0}",
            "Job scheduled: {0}",
            "Job completed: {0}, duration={1}ms",
            "Configuration loaded from {0}",
            "Service started: {0}",
            "Service stopped: {0}",
            "Exception occurred: {0}"
    };
    
    private final Random random = new Random();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private boolean isRunning = false;
    
    /**
     * Constructs a new SampleLogSourcePlugin.
     */
    public SampleLogSourcePlugin() {
        super(
                "sample-log-source",
                "Sample Log Source",
                "1.0.0",
                "A sample log source plugin that generates random log entries"
        );
    }
    
    @Override
    public String getSourceType() {
        return "sample";
    }
    
    @Override
    public List<LogEntry> collectLogs(int maxEntries, long fromTimestamp) throws Exception {
        if (!isRunning) {
            throw new IllegalStateException("Plugin is not running");
        }
        
        logger.info("Collecting logs: maxEntries={}, fromTimestamp={}", maxEntries, fromTimestamp);
        
        List<LogEntry> logs = new ArrayList<>();
        long currentTime = System.currentTimeMillis();
        
        for (int i = 0; i < maxEntries; i++) {
            // Generate a random timestamp between fromTimestamp and current time
            long timestamp = fromTimestamp + (long) (random.nextDouble() * (currentTime - fromTimestamp));
            
            // Create a log entry
            LogEntry entry = createRandomLogEntry(timestamp);
            logs.add(entry);
        }
        
        // Sort logs by timestamp
        logs.sort((a, b) -> Long.compare(a.timestamp(), b.timestamp()));
        
        logger.info("Collected {} log entries", logs.size());
        return logs;
    }
    
    @Override
    public boolean testConnection() {
        return isRunning;
    }
    
    @Override
    public String getConfigurationSchema() {
        return "{"
                + "\"type\": \"object\","
                + "\"properties\": {"
                + "  \"maxEntriesPerRequest\": {"
                + "    \"type\": \"integer\","
                + "    \"description\": \"Maximum number of log entries to return per request\","
                + "    \"default\": 100"
                + "  },"
                + "  \"generateErrorsFrequency\": {"
                + "    \"type\": \"number\","
                + "    \"description\": \"Frequency of ERROR level logs (0.0-1.0)\","
                + "    \"default\": 0.1"
                + "  }"
                + "},"
                + "\"required\": [\"maxEntriesPerRequest\"]"
                + "}";
    }
    
    @Override
    protected void doInitialize() throws Exception {
        logger.info("Initializing SampleLogSourcePlugin");
        // No initialization needed for this sample plugin
    }
    
    @Override
    protected void doStart() throws Exception {
        logger.info("Starting SampleLogSourcePlugin");
        isRunning = true;
    }
    
    @Override
    protected void doStop() throws Exception {
        logger.info("Stopping SampleLogSourcePlugin");
        isRunning = false;
    }
    
    /**
     * Creates a random log entry with the specified timestamp.
     *
     * @param timestamp The timestamp for the log entry
     * @return A random log entry
     */
    private LogEntry createRandomLogEntry(long timestamp) {
        String level = LOG_LEVELS[random.nextInt(LOG_LEVELS.length)];
        String messageTemplate = LOG_MESSAGES[random.nextInt(LOG_MESSAGES.length)];
        
        // Replace placeholders with random values
        String message = messageTemplate;
        while (message.contains("{")) {
            int placeholderStart = message.indexOf("{");
            int placeholderEnd = message.indexOf("}", placeholderStart);
            
            if (placeholderEnd > placeholderStart) {
                String placeholder = message.substring(placeholderStart, placeholderEnd + 1);
                String replacement = generateRandomValue();
                message = message.replace(placeholder, replacement);
            } else {
                break;
            }
        }
        
        // Format the timestamp
        LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault());
        String formattedDate = dateTime.format(dateFormatter);
        
        // Create raw content
        String rawContent = formattedDate + " [" + level + "] " + message;
        
        // Create metadata map
        Map<String, String> metadata = new HashMap<>();
        metadata.put("thread", "thread-" + random.nextInt(10));
        metadata.put("logger", "com.example.SampleApp" + (random.nextInt(5) + 1));
        
        if (level.equals("ERROR")) {
            metadata.put("exception", "java.lang.Exception: Sample exception");
            metadata.put("stacktrace", "java.lang.Exception: Sample exception\n\tat com.example.SampleApp.method1(SampleApp.java:42)\n\tat com.example.SampleApp.method2(SampleApp.java:24)");
        }
        
        // Create a unique ID for the log entry
        String id = UUID.randomUUID().toString();
        
        // Create the log entry using the constructor
        return new LogEntry(
                id,
                timestamp,
                Long.valueOf(System.currentTimeMillis()), // recordTime (current time) as Long object
                level,
                message,
                "sample-log-source",
                metadata,
                rawContent
        );
    }
    
    /**
     * Generates a random value for placeholder replacement.
     *
     * @return A random value as a string
     */
    private String generateRandomValue() {
        int type = random.nextInt(4);
        
        switch (type) {
            case 0: // Number
                return String.valueOf(random.nextInt(1000));
            case 1: // Percentage
                return String.valueOf(random.nextInt(101));
            case 2: // File path
                return "/var/log/sample/file" + random.nextInt(100) + ".log";
            case 3: // Username
                return "user" + random.nextInt(100);
            default:
                return "value";
        }
    }
}