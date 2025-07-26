package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for recognizing and analyzing patterns in log messages.
 * This service identifies common patterns in log messages, extracts variables,
 * and provides methods to search for and analyze patterns.
 */
@Service
public class LogPatternRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(LogPatternRecognitionService.class);
    
    // Regex patterns for common variable types
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", Pattern.CASE_INSENSITIVE);
    private static final Pattern IP_PATTERN = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern URL_PATTERN = Pattern.compile("\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\b\\d+\\b");
    private static final Pattern TIMESTAMP_PATTERN = Pattern.compile("\\b\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,6})?(?:Z|[+-]\\d{2}:?\\d{2})?\\b");
    
    // Cache of recognized patterns
    private final Map<String, LogPattern> patternCache = new ConcurrentHashMap<>();
    
    @Autowired
    private LuceneService luceneService;
    
    /**
     * Analyzes a batch of log entries to identify common patterns.
     * 
     * @param logEntries The log entries to analyze
     * @return A map of pattern templates to their occurrence count
     */
    public Map<String, Integer> analyzeLogPatterns(List<LogEntry> logEntries) {
        Map<String, Integer> patternCounts = new HashMap<>();
        
        for (LogEntry logEntry : logEntries) {
            String message = logEntry.message();
            if (message == null || message.isEmpty()) {
                continue;
            }
            
            LogPattern pattern = recognizePattern(message);
            patternCounts.put(pattern.template(), patternCounts.getOrDefault(pattern.template(), 0) + 1);
        }
        
        return patternCounts;
    }
    
    /**
     * Recognizes the pattern in a log message by replacing variable parts with placeholders.
     * 
     * @param message The log message
     * @return A LogPattern object containing the template and variables
     */
    public LogPattern recognizePattern(String message) {
        if (patternCache.containsKey(message)) {
            return patternCache.get(message);
        }
        
        String template = message;
        Map<String, List<String>> variables = new HashMap<>();
        
        // Extract UUIDs
        template = extractVariables(template, UUID_PATTERN, "UUID", variables);
        
        // Extract IP addresses
        template = extractVariables(template, IP_PATTERN, "IP_ADDRESS", variables);
        
        // Extract email addresses
        template = extractVariables(template, EMAIL_PATTERN, "EMAIL", variables);
        
        // Extract URLs
        template = extractVariables(template, URL_PATTERN, "URL", variables);
        
        // Extract timestamps
        template = extractVariables(template, TIMESTAMP_PATTERN, "TIMESTAMP", variables);
        
        // Extract numbers (after other patterns to avoid conflicts)
        template = extractVariables(template, NUMBER_PATTERN, "NUMBER", variables);
        
        LogPattern pattern = new LogPattern(template, variables);
        patternCache.put(message, pattern);
        
        return pattern;
    }
    
    /**
     * Extracts variables of a specific type from a message and replaces them with placeholders.
     * 
     * @param message The message to process
     * @param pattern The regex pattern to match variables
     * @param variableType The type of variable (used in the placeholder)
     * @param variables Map to store extracted variables
     * @return The message with variables replaced by placeholders
     */
    private String extractVariables(String message, Pattern pattern, String variableType, Map<String, List<String>> variables) {
        Matcher matcher = pattern.matcher(message);
        StringBuffer sb = new StringBuffer();
        List<String> extractedValues = new ArrayList<>();
        
        while (matcher.find()) {
            String value = matcher.group();
            extractedValues.add(value);
            matcher.appendReplacement(sb, "{{" + variableType + "}}");
        }
        matcher.appendTail(sb);
        
        if (!extractedValues.isEmpty()) {
            variables.put(variableType, extractedValues);
        }
        
        return sb.toString();
    }
    
    /**
     * Finds log entries that match a specific pattern template.
     * 
     * @param patternTemplate The pattern template to match
     * @param startTime Optional start time for filtering logs
     * @param endTime Optional end time for filtering logs
     * @return List of log entries matching the pattern
     */
    public List<LogEntry> findLogsByPattern(String patternTemplate, Long startTime, Long endTime) {
        try {
            List<LogEntry> allLogs = luceneService.search("*", false, startTime, endTime);
            
            return allLogs.stream()
                    .filter(log -> {
                        if (log.message() == null) {
                            return false;
                        }
                        LogPattern pattern = recognizePattern(log.message());
                        return pattern.template().equals(patternTemplate);
                    })
                    .collect(Collectors.toList());
        } catch (IOException e) {
            logger.error("Error searching logs for pattern: " + patternTemplate, e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets the most common log patterns within a time range.
     * 
     * @param startTime Optional start time for filtering logs
     * @param endTime Optional end time for filtering logs
     * @param limit Maximum number of patterns to return
     * @return Map of pattern templates to their occurrence count, sorted by count (descending)
     */
    public Map<String, Integer> getMostCommonPatterns(Long startTime, Long endTime, int limit) {
        try {
            List<LogEntry> logs = luceneService.search("*", false, startTime, endTime);
            Map<String, Integer> patternCounts = analyzeLogPatterns(logs);
            
            return patternCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(limit)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (e1, e2) -> e1,
                            LinkedHashMap::new
                    ));
        } catch (IOException e) {
            logger.error("Error getting most common patterns", e);
            return Collections.emptyMap();
        }
    }
    
    /**
     * Clears the pattern cache.
     */
    public void clearPatternCache() {
        patternCache.clear();
        logger.info("Pattern cache cleared");
    }
    
    /**
     * Gets the current size of the pattern cache.
     * 
     * @return The number of patterns in the cache
     */
    public int getPatternCacheSize() {
        return patternCache.size();
    }
    
    /**
     * Represents a recognized log pattern with a template and extracted variables.
     */
    public record LogPattern(String template, Map<String, List<String>> variables) {
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LogPattern that = (LogPattern) o;
            return Objects.equals(template, that.template);
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(template);
        }
    }
}