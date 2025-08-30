package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.LogSourceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for fetching logs from AWS CloudWatch Logs.
 * This service is responsible for connecting to AWS CloudWatch Logs,
 * fetching log events, and indexing them using the LuceneService.
 */
@Service
public class CloudWatchLogService {
    private static final Logger logger = LoggerFactory.getLogger(CloudWatchLogService.class);

    private final LuceneService luceneService;
    private final LogBufferService logBufferService;

    // Map to store the last token for each log stream
    private final Map<String, String> nextTokenMap = new ConcurrentHashMap<>();

    // Map to store the last timestamp for each log stream
    private final Map<String, Long> lastTimestampMap = new ConcurrentHashMap<>();

    // Map to store active CloudWatch log sources
    private final Map<String, LogSourceConfig> activeSources = new ConcurrentHashMap<>();

    /**
     * Constructor with required dependencies.
     */
    public CloudWatchLogService(LuceneService luceneService, LogBufferService logBufferService) {
        this.luceneService = luceneService;
        this.logBufferService = logBufferService;
        logger.info("CloudWatchLogService initialized");
    }

    /**
     * Register a CloudWatch log source.
     *
     * @param config The CloudWatch log source configuration
     * @return true if the source was registered successfully, false otherwise
     */
    public boolean registerSource(LogSourceConfig config) {
        if (config.getSourceType() != LogSourceConfig.SourceType.CLOUDWATCH) {
            logger.error("Cannot register non-CloudWatch source: {}", config.getId());
            return false;
        }

        if (!config.isEnabled()) {
            logger.warn("Cannot register disabled CloudWatch source: {}", config.getId());
            return false;
        }

        // Validate required fields
        if (config.getLogGroupName() == null || config.getLogGroupName().isEmpty()) {
            logger.error("CloudWatch log group name is required: {}", config.getId());
            return false;
        }

        // Store the source configuration
        activeSources.put(config.getId(), config);
        logger.info("Registered CloudWatch log source: {}", config.getId());

        // Fetch logs immediately
        fetchLogsForSource(config);

        return true;
    }

    /**
     * Unregister a CloudWatch log source.
     *
     * @param sourceId The ID of the CloudWatch log source
     * @return true if the source was unregistered successfully, false otherwise
     */
    public boolean unregisterSource(String sourceId) {
        LogSourceConfig config = activeSources.remove(sourceId);
        if (config != null) {
            logger.info("Unregistered CloudWatch log source: {}", sourceId);
            return true;
        } else {
            logger.warn("No CloudWatch log source found with ID: {}", sourceId);
            return false;
        }
    }

    /**
     * Get the number of active CloudWatch log sources.
     *
     * @return The number of active CloudWatch log sources
     */
    public int getActiveSourceCount() {
        return activeSources.size();
    }

    /**
     * Scheduled task to fetch logs from all active CloudWatch log sources.
     * This runs at a fixed rate defined by the smallest queryRefreshIntervalSeconds
     * among all active sources, but not more frequently than once per minute.
     */
    @Scheduled(fixedRate = 60000) // Run every minute
    public void fetchLogsFromAllSources() {
        if (activeSources.isEmpty()) {
            return;
        }

        logger.debug("Fetching logs from {} CloudWatch sources", activeSources.size());

        for (LogSourceConfig config : activeSources.values()) {
            try {
                fetchLogsForSource(config);
            } catch (Exception e) {
                logger.error("Error fetching logs from CloudWatch source: {}", config.getId(), e);
            }
        }
    }

    /**
     * Fetch logs for a specific CloudWatch log source.
     *
     * @param config The CloudWatch log source configuration
     * @return The number of log events fetched
     */
    public int fetchLogsForSource(LogSourceConfig config) {
        if (config.getSourceType() != LogSourceConfig.SourceType.CLOUDWATCH) {
            logger.error("Cannot fetch logs for non-CloudWatch source: {}", config.getId());
            return 0;
        }

        if (!config.isEnabled()) {
            logger.warn("Cannot fetch logs for disabled CloudWatch source: {}", config.getId());
            return 0;
        }

        logger.debug("Fetching logs for CloudWatch source: {}", config.getId());

        try {
            // Create CloudWatch Logs client
            CloudWatchLogsClient cloudWatchLogsClient = createCloudWatchLogsClient(config);

            // If no specific log stream is specified, fetch from all streams in the log group
            if (config.getLogStreamName() == null || config.getLogStreamName().isEmpty()) {
                return fetchLogsFromAllStreams(cloudWatchLogsClient, config);
            } else {
                return fetchLogsFromStream(cloudWatchLogsClient, config, config.getLogStreamName());
            }
        } catch (Exception e) {
            logger.error("Error fetching logs from CloudWatch: {}", config.getId(), e);
            return 0;
        }
    }

    /**
     * Fetch logs from all streams in a log group.
     *
     * @param client The CloudWatch Logs client
     * @param config The CloudWatch log source configuration
     * @return The number of log events fetched
     */
    private int fetchLogsFromAllStreams(CloudWatchLogsClient client, LogSourceConfig config) {
        int totalEvents = 0;

        try {
            // Describe log streams in the log group
            DescribeLogStreamsRequest describeRequest = DescribeLogStreamsRequest.builder()
                    .logGroupName(config.getLogGroupName())
                    .build();

            DescribeLogStreamsResponse describeResponse = client.describeLogStreams(describeRequest);
            List<LogStream> logStreams = describeResponse.logStreams();

            logger.debug("Found {} log streams in group {}", logStreams.size(), config.getLogGroupName());

            // Fetch logs from each stream
            for (LogStream logStream : logStreams) {
                totalEvents += fetchLogsFromStream(client, config, logStream.logStreamName());
            }

            return totalEvents;
        } catch (Exception e) {
            logger.error("Error fetching logs from all streams: {}", config.getId(), e);
            return totalEvents;
        }
    }

    /**
     * Fetch logs from a specific log stream.
     *
     * @param client     The CloudWatch Logs client
     * @param config     The CloudWatch log source configuration
     * @param streamName The name of the log stream
     * @return The number of log events fetched
     */
    private int fetchLogsFromStream(CloudWatchLogsClient client, LogSourceConfig config, String streamName) {
        int eventCount = 0;
        String sourceKey = config.getId() + ":" + streamName;

        try {
            // Get the next token for this stream (if any)
            String nextToken = nextTokenMap.get(sourceKey);

            // Get the last timestamp for this stream (if any)
            Long startTime = lastTimestampMap.getOrDefault(sourceKey, 0L);

            // Build the request
            GetLogEventsRequest.Builder requestBuilder = GetLogEventsRequest.builder()
                    .logGroupName(config.getLogGroupName())
                    .logStreamName(streamName)
                    .startTime(startTime);

            if (nextToken != null) {
                requestBuilder.nextToken(nextToken);
            }

            GetLogEventsRequest request = requestBuilder.build();

            // Get log events
            GetLogEventsResponse response = client.getLogEvents(request);
            List<OutputLogEvent> events = response.events();

            if (!events.isEmpty()) {
                logger.debug("Fetched {} log events from stream {}", events.size(), streamName);

                // Process log events
                List<LogEntry> logEntries = new ArrayList<>();

                for (OutputLogEvent event : events) {
                    LogEntry entry = createLogEntry(event, config, streamName);
                    logEntries.add(entry);

                    // Update the last timestamp
                    long timestamp = event.timestamp();
                    if (timestamp > lastTimestampMap.getOrDefault(sourceKey, 0L)) {
                        lastTimestampMap.put(sourceKey, timestamp);
                    }

                    eventCount++;
                }

                // Buffer the log entries for indexing
                logBufferService.addAllToBuffer(logEntries);

                // Store the next token for future requests
                nextTokenMap.put(sourceKey, response.nextForwardToken());
            }

            return eventCount;
        } catch (Exception e) {
            logger.error("Error fetching logs from stream {}: {}", streamName, e.getMessage());
            return eventCount;
        }
    }

    /**
     * Create a LogEntry from a CloudWatch log event.
     *
     * @param event      The CloudWatch log event
     * @param config     The CloudWatch log source configuration
     * @param streamName The name of the log stream
     * @return The created LogEntry
     */
    private LogEntry createLogEntry(OutputLogEvent event, LogSourceConfig config, String streamName) {
        // Extract log level if possible
        String message = event.message();
        String logLevel;
        if (message.contains("ERROR") || message.contains("SEVERE")) {
            logLevel = "ERROR";
        } else if (message.contains("WARN")) {
            logLevel = "WARN";
        } else if (message.contains("INFO")) {
            logLevel = "INFO";
        } else if (message.contains("DEBUG")) {
            logLevel = "DEBUG";
        } else if (message.contains("TRACE")) {
            logLevel = "TRACE";
        } else {
            logLevel = "INFO";
        }

        // Create metadata map
        Map<String, String> metadata = new HashMap<>();
        metadata.put("logGroup", config.getLogGroupName());
        metadata.put("logStream", streamName);
        metadata.put("region", config.getAwsRegion());

        // Create a log entry with the CloudWatch event data
        return new LogEntry(
                UUID.randomUUID().toString(),           // id
                System.currentTimeMillis(),             // timestamp (when the log was fetched)
                event.timestamp(),                      // recordTime (from CloudWatch)
                logLevel,                               // level (extracted from message)
                event.message(),                        // message
                "cloudwatch:" + config.getLogGroupName() + ":" + streamName, // source
                metadata,                               // metadata
                event.message()                         // raw content
        );
    }

    /**
     * Create a CloudWatch Logs client for the given configuration.
     *
     * @param config The CloudWatch log source configuration
     * @return The created CloudWatch Logs client
     */
    private CloudWatchLogsClient createCloudWatchLogsClient(LogSourceConfig config) {
        Region region = Region.of(config.getAwsRegion());

        // Use provided credentials if available, otherwise use default credentials provider
        AwsCredentialsProvider credentialsProvider;
        if (config.getAwsAccessKey() != null && !config.getAwsAccessKey().isEmpty() &&
                config.getAwsSecretKey() != null && !config.getAwsSecretKey().isEmpty()) {
            AwsBasicCredentials credentials = AwsBasicCredentials.create(
                    config.getAwsAccessKey(),
                    config.getAwsSecretKey());
            credentialsProvider = StaticCredentialsProvider.create(credentials);
            logger.debug("Using provided AWS credentials for source: {}", config.getId());
        } else {
            credentialsProvider = DefaultCredentialsProvider.create();
            logger.debug("Using default AWS credentials provider for source: {}", config.getId());
        }

        return CloudWatchLogsClient.builder()
                .region(region)
                .credentialsProvider(credentialsProvider)
                .build();
    }
}