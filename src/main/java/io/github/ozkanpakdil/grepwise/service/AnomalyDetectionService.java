package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.AnomalyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for detecting anomalies in log data using statistical methods.
 * This implementation focuses on detecting unusual log frequency patterns
 * and identifying outliers in log occurrence rates.
 */
@Service
public class AnomalyDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(AnomalyDetectionService.class);

    @Autowired
    private LuceneService luceneService;

    @Value("${anomaly.detection.enabled:true}")
    private boolean anomalyDetectionEnabled;

    @Value("${anomaly.detection.threshold:2.0}")
    private double anomalyThreshold;

    @Value("${anomaly.detection.min.sample.size:10}")
    private int minSampleSize;

    @Value("${anomaly.detection.time.window.minutes:60}")
    private int timeWindowMinutes;

    /**
     * Detects anomalies in log frequency over a specified time period.
     * Uses Z-score method to identify time intervals with abnormal log counts.
     *
     * @param source The log source to analyze (null for all sources)
     * @param level The log level to filter by (null for all levels)
     * @param startTime The start timestamp for analysis
     * @param endTime The end timestamp for analysis
     * @param intervalMinutes The size of time intervals to analyze (in minutes)
     * @return A list of anomaly results
     */
    public List<AnomalyResult> detectFrequencyAnomalies(
            String source, 
            String level, 
            Long startTime, 
            Long endTime, 
            int intervalMinutes) {
        
        if (!anomalyDetectionEnabled) {
            logger.info("Anomaly detection is disabled");
            return Collections.emptyList();
        }

        if (startTime == null) {
            startTime = System.currentTimeMillis() - (timeWindowMinutes * 60 * 1000);
        }
        
        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }

        // Use default interval if not specified
        if (intervalMinutes <= 0) {
            intervalMinutes = 5; // Default 5-minute intervals
        }

        logger.info("Detecting frequency anomalies for time range: {} to {}, interval: {} minutes",
                formatTimestamp(startTime), formatTimestamp(endTime), intervalMinutes);

        // Get logs for the specified time range
        List<LogEntry> logs;
        try {
            logs = luceneService.search("*", false, startTime, endTime);
            
            // Apply source filter if specified
            if (source != null && !source.isEmpty()) {
                logs = logs.stream()
                    .filter(log -> source.equals(log.source()))
                    .collect(Collectors.toList());
            }
            
            // Apply level filter if specified
            if (level != null && !level.isEmpty()) {
                logs = logs.stream()
                    .filter(log -> level.equals(log.level()))
                    .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving logs for anomaly detection", e);
            return Collections.emptyList();
        }

        if (logs.isEmpty()) {
            logger.info("No logs found for the specified criteria");
            return Collections.emptyList();
        }

        // Group logs by time intervals
        Map<Long, Integer> logCountsByInterval = groupLogsByTimeInterval(logs, startTime, endTime, intervalMinutes);
        
        if (logCountsByInterval.size() < minSampleSize) {
            logger.info("Insufficient data points for anomaly detection. Need at least {} intervals, got {}",
                    minSampleSize, logCountsByInterval.size());
            return Collections.emptyList();
        }

        // Calculate statistics
        double mean = calculateMean(logCountsByInterval.values());
        double stdDev = calculateStandardDeviation(logCountsByInterval.values(), mean);
        
        if (stdDev == 0) {
            logger.info("Standard deviation is zero, no anomalies detected");
            return Collections.emptyList();
        }

        // Detect anomalies using Z-score
        List<AnomalyResult> anomalies = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : logCountsByInterval.entrySet()) {
            long intervalStart = entry.getKey();
            int count = entry.getValue();
            
            double zScore = Math.abs((count - mean) / stdDev);
            
            if (zScore > anomalyThreshold) {
                AnomalyResult anomaly = AnomalyResult.builder()
                    .timestamp(intervalStart)
                    .score(zScore)
                    .expectedValue(mean)
                    .actualValue(count)
                    .description(String.format(
                        "Anomalous log frequency detected at %s: %d logs (expected around %.2f, z-score: %.2f)",
                        formatTimestamp(intervalStart), count, mean, zScore))
                    .build();
                
                anomalies.add(anomaly);
            }
        }

        logger.info("Detected {} anomalies out of {} intervals", anomalies.size(), logCountsByInterval.size());
        return anomalies;
    }

    /**
     * Detects pattern anomalies by analyzing the frequency of specific log patterns.
     * 
     * @param source The log source to analyze (null for all sources)
     * @param startTime The start timestamp for analysis
     * @param endTime The end timestamp for analysis
     * @param patternQuery The Lucene query to match specific log patterns
     * @return A list of anomaly results
     */
    public List<AnomalyResult> detectPatternAnomalies(
            String source, 
            Long startTime, 
            Long endTime, 
            String patternQuery) {
        
        if (!anomalyDetectionEnabled) {
            logger.info("Anomaly detection is disabled");
            return Collections.emptyList();
        }

        if (startTime == null) {
            startTime = System.currentTimeMillis() - (timeWindowMinutes * 60 * 1000);
        }
        
        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }

        if (patternQuery == null || patternQuery.isEmpty()) {
            patternQuery = "*";
        }

        logger.info("Detecting pattern anomalies for time range: {} to {}, pattern: {}",
                formatTimestamp(startTime), formatTimestamp(endTime), patternQuery);

        // Get logs for the specified time range and pattern
        List<LogEntry> matchingLogs;
        List<LogEntry> allLogs;
        try {
            matchingLogs = luceneService.search(patternQuery, false, startTime, endTime);
            allLogs = luceneService.search("*", false, startTime, endTime);
            
            // Apply source filter if specified
            if (source != null && !source.isEmpty()) {
                matchingLogs = matchingLogs.stream()
                    .filter(log -> source.equals(log.source()))
                    .collect(Collectors.toList());
                allLogs = allLogs.stream()
                    .filter(log -> source.equals(log.source()))
                    .collect(Collectors.toList());
            }
            
        } catch (Exception e) {
            logger.error("Error retrieving logs for pattern anomaly detection", e);
            return Collections.emptyList();
        }

        if (allLogs.isEmpty()) {
            logger.info("No logs found for the specified criteria");
            return Collections.emptyList();
        }

        // Calculate the baseline ratio of pattern occurrence
        double patternRatio = (double) matchingLogs.size() / allLogs.size();
        
        // Group logs by time intervals
        int intervalMinutes = 5; // Default 5-minute intervals for pattern analysis
        Map<Long, List<LogEntry>> logsByInterval = groupLogsByTimeIntervalWithEntries(allLogs, startTime, endTime, intervalMinutes);
        
        if (logsByInterval.size() < minSampleSize) {
            logger.info("Insufficient data points for pattern anomaly detection. Need at least {} intervals, got {}",
                    minSampleSize, logsByInterval.size());
            return Collections.emptyList();
        }

        // Calculate pattern ratios for each interval
        Map<Long, Double> patternRatiosByInterval = new HashMap<>();
        for (Map.Entry<Long, List<LogEntry>> entry : logsByInterval.entrySet()) {
            long intervalStart = entry.getKey();
            List<LogEntry> intervalLogs = entry.getValue();
            
            if (intervalLogs.isEmpty()) {
                continue;
            }
            
            // Create a final copy of matchingLogs for use in the lambda
            final List<LogEntry> finalMatchingLogs = matchingLogs;
            long matchingCount = intervalLogs.stream()
                .filter(log -> finalMatchingLogs.contains(log))
                .count();
                
            double intervalRatio = (double) matchingCount / intervalLogs.size();
            patternRatiosByInterval.put(intervalStart, intervalRatio);
        }
        
        // Calculate statistics for pattern ratios
        double meanRatio = calculateMean(patternRatiosByInterval.values());
        double stdDevRatio = calculateStandardDeviation(patternRatiosByInterval.values(), meanRatio);
        
        if (stdDevRatio == 0) {
            logger.info("Standard deviation of pattern ratios is zero, no anomalies detected");
            return Collections.emptyList();
        }

        // Detect anomalies in pattern ratios using Z-score
        List<AnomalyResult> anomalies = new ArrayList<>();
        for (Map.Entry<Long, Double> entry : patternRatiosByInterval.entrySet()) {
            long intervalStart = entry.getKey();
            double ratio = entry.getValue();
            
            double zScore = Math.abs((ratio - meanRatio) / stdDevRatio);
            
            if (zScore > anomalyThreshold) {
                AnomalyResult anomaly = AnomalyResult.builder()
                    .timestamp(intervalStart)
                    .score(zScore)
                    .expectedValue(meanRatio)
                    .actualValue(ratio)
                    .description(String.format(
                        "Anomalous pattern frequency detected at %s: %.2f%% (expected around %.2f%%, z-score: %.2f)",
                        formatTimestamp(intervalStart), ratio * 100, meanRatio * 100, zScore))
                    .build();
                
                anomalies.add(anomaly);
            }
        }

        logger.info("Detected {} pattern anomalies out of {} intervals", anomalies.size(), patternRatiosByInterval.size());
        return anomalies;
    }

    /**
     * Groups log entries by time intervals and counts logs in each interval.
     */
    private Map<Long, Integer> groupLogsByTimeInterval(
            List<LogEntry> logs, 
            long startTime, 
            long endTime, 
            int intervalMinutes) {
        
        Map<Long, Integer> logCountsByInterval = new TreeMap<>();
        
        // Initialize all intervals with zero counts
        long intervalMs = intervalMinutes * 60 * 1000;
        for (long time = startTime; time < endTime; time += intervalMs) {
            logCountsByInterval.put(time, 0);
        }
        
        // Count logs in each interval
        for (LogEntry log : logs) {
            long logTime = log.timestamp();
            if (logTime >= startTime && logTime < endTime) {
                long intervalStart = startTime + ((logTime - startTime) / intervalMs) * intervalMs;
                logCountsByInterval.put(intervalStart, logCountsByInterval.getOrDefault(intervalStart, 0) + 1);
            }
        }
        
        return logCountsByInterval;
    }

    /**
     * Groups log entries by time intervals and keeps the actual log entries.
     */
    private Map<Long, List<LogEntry>> groupLogsByTimeIntervalWithEntries(
            List<LogEntry> logs, 
            long startTime, 
            long endTime, 
            int intervalMinutes) {
        
        Map<Long, List<LogEntry>> logsByInterval = new TreeMap<>();
        
        // Initialize all intervals with empty lists
        long intervalMs = intervalMinutes * 60 * 1000;
        for (long time = startTime; time < endTime; time += intervalMs) {
            logsByInterval.put(time, new ArrayList<>());
        }
        
        // Group logs by interval
        for (LogEntry log : logs) {
            long logTime = log.timestamp();
            if (logTime >= startTime && logTime < endTime) {
                long intervalStart = startTime + ((logTime - startTime) / intervalMs) * intervalMs;
                logsByInterval.computeIfAbsent(intervalStart, k -> new ArrayList<>()).add(log);
            }
        }
        
        return logsByInterval;
    }

    /**
     * Calculates the mean of a collection of numbers.
     */
    private double calculateMean(Collection<? extends Number> values) {
        return values.stream()
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates the standard deviation of a collection of numbers.
     */
    private double calculateStandardDeviation(Collection<? extends Number> values, double mean) {
        double variance = values.stream()
                .mapToDouble(Number::doubleValue)
                .map(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0.0);
        
        return Math.sqrt(variance);
    }

    /**
     * Formats a timestamp as a human-readable date string.
     */
    private String formatTimestamp(long timestamp) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()).toString();
    }

    /**
     * Sets the anomaly detection threshold.
     * Higher values mean fewer anomalies will be detected.
     */
    public void setAnomalyThreshold(double threshold) {
        this.anomalyThreshold = threshold;
    }

    /**
     * Gets the current anomaly detection threshold.
     */
    public double getAnomalyThreshold() {
        return anomalyThreshold;
    }

    /**
     * Enables or disables anomaly detection.
     */
    public void setAnomalyDetectionEnabled(boolean enabled) {
        this.anomalyDetectionEnabled = enabled;
    }

    /**
     * Checks if anomaly detection is enabled.
     */
    public boolean isAnomalyDetectionEnabled() {
        return anomalyDetectionEnabled;
    }

    /**
     * Sets the minimum sample size required for anomaly detection.
     */
    public void setMinSampleSize(int size) {
        this.minSampleSize = size;
    }

    /**
     * Gets the minimum sample size required for anomaly detection.
     */
    public int getMinSampleSize() {
        return minSampleSize;
    }

    /**
     * Sets the default time window for anomaly detection in minutes.
     */
    public void setTimeWindowMinutes(int minutes) {
        this.timeWindowMinutes = minutes;
    }

    /**
     * Gets the default time window for anomaly detection in minutes.
     */
    public int getTimeWindowMinutes() {
        return timeWindowMinutes;
    }
}