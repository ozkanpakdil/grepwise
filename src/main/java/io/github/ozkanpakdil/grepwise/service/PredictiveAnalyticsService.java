package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import io.github.ozkanpakdil.grepwise.model.PredictiveResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for predictive analytics on log data using time series forecasting.
 * This implementation focuses on predicting future log volumes and identifying trends
 * in log occurrence patterns.
 */
@Service
public class PredictiveAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(PredictiveAnalyticsService.class);

    @Autowired
    private LuceneService luceneService;

    @Value("${predictive.analytics.enabled:true}")
    private boolean predictiveAnalyticsEnabled;

    @Value("${predictive.analytics.min.sample.size:20}")
    private int minSampleSize;

    @Value("${predictive.analytics.time.window.minutes:1440}") // Default: 24 hours
    private int timeWindowMinutes;

    @Value("${predictive.analytics.forecast.horizon.minutes:1440}") // Default: 24 hours ahead
    private int forecastHorizonMinutes;

    /**
     * Predicts future log volume based on historical data.
     * Uses simple exponential smoothing for time series forecasting.
     *
     * @param source           The log source to analyze (null for all sources)
     * @param level            The log level to filter by (null for all levels)
     * @param startTime        The start timestamp for historical data analysis
     * @param endTime          The end timestamp for historical data analysis
     * @param intervalMinutes  The size of time intervals to analyze and predict (in minutes)
     * @param horizonIntervals Number of future intervals to predict
     * @return A list of prediction results
     */
    public List<PredictiveResult> predictLogVolume(
            String source,
            String level,
            Long startTime,
            Long endTime,
            int intervalMinutes,
            int horizonIntervals) {

        if (!predictiveAnalyticsEnabled) {
            logger.info("Predictive analytics is disabled");
            return Collections.emptyList();
        }

        if (startTime == null) {
            startTime = System.currentTimeMillis() - ((long) timeWindowMinutes * 60 * 1000);
        }

        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }

        // Use default interval if not specified
        if (intervalMinutes <= 0) {
            intervalMinutes = 60; // Default 60-minute intervals for predictions
        }

        // Use default horizon if not specified
        if (horizonIntervals <= 0) {
            horizonIntervals = forecastHorizonMinutes / intervalMinutes;
        }

        logger.info("Predicting log volume for time range: {} to {}, interval: {} minutes, horizon: {} intervals",
                formatTimestamp(startTime), formatTimestamp(endTime), intervalMinutes, horizonIntervals);

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
            logger.error("Error retrieving logs for predictive analytics", e);
            return Collections.emptyList();
        }

        if (logs.isEmpty()) {
            logger.info("No logs found for the specified criteria");
            return Collections.emptyList();
        }

        // Group logs by time intervals
        Map<Long, Integer> logCountsByInterval = groupLogsByTimeInterval(logs, startTime, endTime, intervalMinutes);

        if (logCountsByInterval.size() < minSampleSize) {
            logger.info("Insufficient data points for prediction. Need at least {} intervals, got {}",
                    minSampleSize, logCountsByInterval.size());
            return Collections.emptyList();
        }

        // Convert to time series (sorted by time)
        List<Double> timeSeries = new ArrayList<>(logCountsByInterval.values().stream()
                .map(Double::valueOf)
                .collect(Collectors.toList()));

        // Perform exponential smoothing forecast
        List<Double> forecast = exponentialSmoothing(timeSeries, horizonIntervals, 0.3);

        // Calculate confidence based on historical variance
        double mean = calculateMean(timeSeries);
        double stdDev = calculateStandardDeviation(timeSeries, mean);
        double coefficientOfVariation = mean > 0 ? stdDev / mean : 1.0;

        // Higher variation means lower confidence
        double baseConfidence = Math.max(0.1, Math.min(0.9, 1.0 - coefficientOfVariation));

        // Create prediction results
        List<PredictiveResult> predictions = new ArrayList<>();
        long lastIntervalStart = endTime;
        long intervalMs = (long) intervalMinutes * 60 * 1000;

        for (int i = 0; i < forecast.size(); i++) {
            long predictionTime = lastIntervalStart + (i + 1) * intervalMs;
            double predictedValue = forecast.get(i);

            // Confidence decreases as we predict further into the future
            double confidenceLevel = baseConfidence * Math.exp(-0.05 * i);

            PredictiveResult prediction = PredictiveResult.builder()
                    .timestamp(System.currentTimeMillis())
                    .predictionTimestamp(predictionTime)
                    .predictedValue(predictedValue)
                    .confidenceLevel(confidenceLevel)
                    .predictionType("VOLUME")
                    .description(String.format(
                            "Predicted log volume for %s: %.2f logs (confidence: %.2f%%)",
                            formatTimestamp(predictionTime), predictedValue, confidenceLevel * 100))
                    .build();

            predictions.add(prediction);
        }

        logger.info("Generated {} volume predictions", predictions.size());
        return predictions;
    }

    /**
     * Predicts trends in log patterns based on historical data.
     * Identifies increasing or decreasing trends in log volume.
     *
     * @param source          The log source to analyze (null for all sources)
     * @param level           The log level to filter by (null for all levels)
     * @param startTime       The start timestamp for historical data analysis
     * @param endTime         The end timestamp for historical data analysis
     * @param intervalMinutes The size of time intervals to analyze (in minutes)
     * @return A trend prediction result
     */
    public PredictiveResult predictLogTrend(
            String source,
            String level,
            Long startTime,
            Long endTime,
            int intervalMinutes) {

        if (!predictiveAnalyticsEnabled) {
            logger.info("Predictive analytics is disabled");
            return null;
        }

        if (startTime == null) {
            startTime = System.currentTimeMillis() - ((long) timeWindowMinutes * 60 * 1000);
        }

        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }

        // Use default interval if not specified
        if (intervalMinutes <= 0) {
            intervalMinutes = 60; // Default 60-minute intervals for trend analysis
        }

        logger.info("Analyzing log trend for time range: {} to {}, interval: {} minutes",
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
            logger.error("Error retrieving logs for trend analysis", e);
            return null;
        }

        if (logs.isEmpty()) {
            logger.info("No logs found for the specified criteria");
            return null;
        }

        // Group logs by time intervals
        Map<Long, Integer> logCountsByInterval = groupLogsByTimeInterval(logs, startTime, endTime, intervalMinutes);

        if (logCountsByInterval.size() < minSampleSize) {
            logger.info("Insufficient data points for trend analysis. Need at least {} intervals, got {}",
                    minSampleSize, logCountsByInterval.size());
            return null;
        }

        // Convert to time series (sorted by time)
        List<Long> times = new ArrayList<>(logCountsByInterval.keySet());
        Collections.sort(times);

        List<Integer> counts = new ArrayList<>();
        for (Long time : times) {
            counts.add(logCountsByInterval.get(time));
        }

        // Calculate linear regression
        double[] regressionResult = calculateLinearRegression(counts);
        double slope = regressionResult[0];
        double intercept = regressionResult[1];
        double rSquared = regressionResult[2];

        // Determine trend direction and strength
        String trendDirection;
        if (Math.abs(slope) < 0.01) {
            trendDirection = "STABLE";
        } else if (slope > 0) {
            trendDirection = "INCREASING";
        } else {
            trendDirection = "DECREASING";
        }

        // Calculate projected change over next day
        int intervalsPerDay = 24 * 60 / intervalMinutes;
        double projectedDailyChange = slope * intervalsPerDay;

        // Calculate average log count
        double averageCount = calculateMean(counts.stream().map(Double::valueOf).collect(Collectors.toList()));

        // Calculate percentage change
        double percentageChange = averageCount > 0 ? (projectedDailyChange / averageCount) * 100 : 0;

        // Create trend prediction result
        PredictiveResult trendPrediction = PredictiveResult.builder()
                .timestamp(System.currentTimeMillis())
                .predictionTimestamp(endTime + (24 * 60 * 60 * 1000)) // 24 hours ahead
                .predictedValue(projectedDailyChange)
                .confidenceLevel(rSquared) // R-squared as confidence
                .predictionType("TREND")
                .description(String.format(
                        "Log volume trend: %s. Projected change over next 24 hours: %.2f logs (%.2f%%). Confidence: %.2f%%",
                        trendDirection, projectedDailyChange, percentageChange, rSquared * 100))
                .addMetadata("trendDirection", trendDirection)
                .addMetadata("slope", slope)
                .addMetadata("rSquared", rSquared)
                .build();

        logger.info("Generated trend prediction: {}", trendPrediction.description());
        return trendPrediction;
    }

    /**
     * Predicts the distribution of log levels in future logs.
     *
     * @param source    The log source to analyze (null for all sources)
     * @param startTime The start timestamp for historical data analysis
     * @param endTime   The end timestamp for historical data analysis
     * @return A prediction result for log level distribution
     */
    public PredictiveResult predictLogLevelDistribution(
            String source,
            Long startTime,
            Long endTime) {

        if (!predictiveAnalyticsEnabled) {
            logger.info("Predictive analytics is disabled");
            return null;
        }

        if (startTime == null) {
            startTime = System.currentTimeMillis() - ((long) timeWindowMinutes * 60 * 1000);
        }

        if (endTime == null) {
            endTime = System.currentTimeMillis();
        }

        logger.info("Predicting log level distribution for time range: {} to {}",
                formatTimestamp(startTime), formatTimestamp(endTime));

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

        } catch (Exception e) {
            logger.error("Error retrieving logs for level distribution prediction", e);
            return null;
        }

        if (logs.isEmpty() || logs.size() < minSampleSize) {
            logger.info("Insufficient data for level distribution prediction. Need at least {} logs, got {}",
                    minSampleSize, logs.size());
            return null;
        }

        // Count logs by level
        Map<String, Long> logCountsByLevel = logs.stream()
                .filter(log -> log.level() != null)
                .collect(Collectors.groupingBy(LogEntry::level, Collectors.counting()));

        // Calculate total logs with levels
        long totalLogs = logCountsByLevel.values().stream().mapToLong(Long::longValue).sum();

        if (totalLogs == 0) {
            logger.info("No logs with level information found");
            return null;
        }

        // Calculate distribution percentages
        Map<String, Double> distributionPercentages = new HashMap<>();
        for (Map.Entry<String, Long> entry : logCountsByLevel.entrySet()) {
            distributionPercentages.put(entry.getKey(), (double) entry.getValue() / totalLogs * 100);
        }

        // Create prediction result
        PredictiveResult prediction = PredictiveResult.builder()
                .timestamp(System.currentTimeMillis())
                .predictionTimestamp(endTime + (24 * 60 * 60 * 1000)) // 24 hours ahead
                .predictedValue(totalLogs)
                .confidenceLevel(0.8) // Fixed confidence for distribution prediction
                .predictionType("LEVEL_DISTRIBUTION")
                .description("Predicted log level distribution for next 24 hours")
                .metadata(new HashMap<>(Map.of("distribution", distributionPercentages)))
                .build();

        logger.info("Generated log level distribution prediction");
        return prediction;
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
        long intervalMs = (long) intervalMinutes * 60 * 1000;
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
     * Performs simple exponential smoothing for time series forecasting.
     *
     * @param timeSeries The historical time series data
     * @param horizon    The number of future points to forecast
     * @param alpha      The smoothing factor (0 < alpha < 1)
     * @return The forecasted values
     */
    private List<Double> exponentialSmoothing(List<Double> timeSeries, int horizon, double alpha) {
        if (timeSeries.isEmpty()) {
            return Collections.emptyList();
        }

        // Initialize with the first value
        double lastSmoothed = timeSeries.get(0);

        // Apply smoothing to historical data
        for (int i = 1; i < timeSeries.size(); i++) {
            lastSmoothed = alpha * timeSeries.get(i) + (1 - alpha) * lastSmoothed;
        }

        // Generate forecast
        List<Double> forecast = new ArrayList<>();
        for (int i = 0; i < horizon; i++) {
            forecast.add(lastSmoothed);
        }

        return forecast;
    }

    /**
     * Calculates linear regression for a time series.
     * Returns [slope, intercept, r-squared].
     */
    private double[] calculateLinearRegression(List<Integer> values) {
        int n = values.size();

        // Create x values (0, 1, 2, ...)
        List<Integer> x = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            x.add(i);
        }

        // Calculate means
        double meanX = calculateMean(x.stream().map(Double::valueOf).collect(Collectors.toList()));
        double meanY = calculateMean(values.stream().map(Double::valueOf).collect(Collectors.toList()));

        // Calculate sums for regression formula
        double sumXY = 0;
        double sumXX = 0;
        double sumYY = 0;

        for (int i = 0; i < n; i++) {
            double xDiff = x.get(i) - meanX;
            double yDiff = values.get(i) - meanY;
            sumXY += xDiff * yDiff;
            sumXX += xDiff * xDiff;
            sumYY += yDiff * yDiff;
        }

        // Calculate slope and intercept
        double slope = sumXY / sumXX;
        double intercept = meanY - slope * meanX;

        // Calculate R-squared
        double rSquared = (sumXY * sumXY) / (sumXX * sumYY);

        return new double[]{slope, intercept, rSquared};
    }

    /**
     * Calculates the mean of a collection of numbers.
     */
    private double calculateMean(Collection<Double> values) {
        return values.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Calculates the standard deviation of a collection of numbers.
     */
    private double calculateStandardDeviation(Collection<Double> values, double mean) {
        double variance = values.stream()
                .mapToDouble(Double::doubleValue)
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
     * Checks if predictive analytics is enabled.
     */
    public boolean isPredictiveAnalyticsEnabled() {
        return predictiveAnalyticsEnabled;
    }

    /**
     * Enables or disables predictive analytics.
     */
    public void setPredictiveAnalyticsEnabled(boolean enabled) {
        this.predictiveAnalyticsEnabled = enabled;
    }

    /**
     * Gets the minimum sample size required for predictions.
     */
    public int getMinSampleSize() {
        return minSampleSize;
    }

    /**
     * Sets the minimum sample size required for predictions.
     */
    public void setMinSampleSize(int size) {
        this.minSampleSize = size;
    }

    /**
     * Gets the default time window for historical data in minutes.
     */
    public int getTimeWindowMinutes() {
        return timeWindowMinutes;
    }

    /**
     * Sets the default time window for historical data in minutes.
     */
    public void setTimeWindowMinutes(int minutes) {
        this.timeWindowMinutes = minutes;
    }

    /**
     * Gets the default forecast horizon in minutes.
     */
    public int getForecastHorizonMinutes() {
        return forecastHorizonMinutes;
    }

    /**
     * Sets the default forecast horizon in minutes.
     */
    public void setForecastHorizonMinutes(int minutes) {
        this.forecastHorizonMinutes = minutes;
    }
}