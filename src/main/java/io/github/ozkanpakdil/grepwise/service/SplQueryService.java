package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service for parsing and executing SPL-like (Splunk Processing Language) queries.
 * Supports commands like search, where, stats, eval, etc.
 */
@Service
public class SplQueryService {

    private static final Logger logger = LoggerFactory.getLogger(SplQueryService.class);

    @Autowired
    private LuceneService luceneService;

    /**
     * Execute an SPL-like query and return results.
     */
    public SplQueryResult executeSplQuery(String splQuery) throws IOException {
        logger.info("Executing SPL query: {}", splQuery);

        // Parse the SPL query into commands
        List<SplCommand> commands = parseSplQuery(splQuery);

        // Execute commands in sequence
        SplQueryResult result = new SplQueryResult();
        List<LogEntry> currentData = new ArrayList<>();

        for (SplCommand command : commands) {
            switch (command.command().toLowerCase()) {
                case "search":
                    currentData = executeSearchCommand(command);
                    break;
                case "where":
                    currentData = executeWhereCommand(command, currentData);
                    break;
                case "stats":
                    result = executeStatsCommand(command, currentData);
                    return result; // Stats is terminal command
                case "eval":
                    currentData = executeEvalCommand(command, currentData);
                    break;
                case "sort":
                    currentData = executeSortCommand(command, currentData);
                    break;
                case "head":
                    currentData = executeHeadCommand(command, currentData);
                    break;
                case "tail":
                    currentData = executeTailCommand(command, currentData);
                    break;
                default:
                    logger.warn("Unknown SPL command: {}", command.command());
            }
        }

        result.setLogEntries(currentData);
        result.setResultType(SplQueryResult.ResultType.LOG_ENTRIES);
        return result;
    }

    /**
     * Parse SPL query string into individual commands.
     */
    private List<SplCommand> parseSplQuery(String splQuery) {
        List<SplCommand> commands = new ArrayList<>();

        // Split by pipe (|) to get individual commands
        String[] parts = splQuery.split("\\|");

        for (String part : parts) {
            part = part.trim();
            if (!part.isEmpty()) {
                commands.add(parseSingleCommand(part));
            }
        }

        return commands;
    }

    /**
     * Parse a single SPL command.
     */
    private SplCommand parseSingleCommand(String commandStr) {
        String[] tokens = commandStr.trim().split("\\s+", 2);
        String command = tokens[0];
        String args = tokens.length > 1 ? tokens[1] : "";

        return new SplCommand(command, args, parseArguments(args));
    }

    /**
     * Parse command arguments into key-value pairs.
     */
    private Map<String, String> parseArguments(String args) {
        Map<String, String> arguments = new HashMap<>();

        if (args.isEmpty()) {
            return arguments;
        }

        // Simple argument parsing - can be enhanced for more complex cases
        Pattern pattern = Pattern.compile("(\\w+)=([^\\s]+)");
        Matcher matcher = pattern.matcher(args);

        while (matcher.find()) {
            arguments.put(matcher.group(1), matcher.group(2));
        }

        // If no key=value pairs found, treat entire args as search term
        if (arguments.isEmpty() && !args.trim().isEmpty()) {
            arguments.put("query", args.trim());
        }

        return arguments;
    }

    /**
     * Execute search command.
     */
    private List<LogEntry> executeSearchCommand(SplCommand command) throws IOException {
        String query = command.arguments().get("query");
        if (query == null) {
            query = command.args(); // Use raw args if no structured query
        }

        // Remove quotes if present
        if (query.startsWith("\"") && query.endsWith("\"")) {
            query = query.substring(1, query.length() - 1);
        }

        // Check for field-specific searches (e.g., level=ERROR)
        if (query.contains("=")) {
            return executeFieldSearch(query);
        }

        // Default to message search
        return luceneService.search(query, false, null, null);
    }

    /**
     * Execute field-specific search.
     */
    private List<LogEntry> executeFieldSearch(String query) throws IOException {
        String[] parts = query.split("=", 2);
        if (parts.length != 2) {
            return luceneService.search(query, false, null, null);
        }

        String field = parts[0].trim();
        String value = parts[1].trim();

        // Remove quotes from value
        if (value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }

        switch (field.toLowerCase()) {
            case "level":
                return luceneService.findByLevel(value);
            case "source":
                return luceneService.findBySource(value);
            default:
                // For other fields, use general search
                return luceneService.search(query, false, null, null);
        }
    }

    /**
     * Execute where command for filtering.
     */
    private List<LogEntry> executeWhereCommand(SplCommand command, List<LogEntry> data) {
        String condition = command.args();

        return data.stream()
                .filter(entry -> evaluateCondition(entry, condition))
                .collect(Collectors.toList());
    }

    /**
     * Execute stats command for aggregation.
     */
    private SplQueryResult executeStatsCommand(SplCommand command, List<LogEntry> data) {
        SplQueryResult result = new SplQueryResult();
        result.setResultType(SplQueryResult.ResultType.STATISTICS);

        String args = command.args();
        Map<String, Object> stats = new HashMap<>();

        if (args.contains("count")) {
            if (args.contains("by")) {
                // Group by field
                String[] parts = args.split("by");
                if (parts.length > 1) {
                    String groupField = parts[1].trim();
                    stats = calculateGroupedCount(data, groupField);
                }
            } else {
                // Simple count
                stats.put("count", data.size());
            }
        }

        result.setStatistics(stats);
        return result;
    }

    /**
     * Execute eval command for field creation/modification.
     */
    private List<LogEntry> executeEvalCommand(SplCommand command, List<LogEntry> data) {
        // Simple eval implementation - can be enhanced
        String args = command.args();

        // For now, just return data unchanged
        // TODO: Implement field evaluation logic
        logger.info("Eval command not fully implemented yet: {}", args);
        return data;
    }

    /**
     * Execute sort command.
     */
    private List<LogEntry> executeSortCommand(SplCommand command, List<LogEntry> data) {
        String field = command.args().trim();
        boolean descending = field.startsWith("-");

        if (descending) {
            field = field.substring(1);
        }

        switch (field.toLowerCase()) {
            case "timestamp":
                return data.stream()
                        .sorted(descending ?
                                Comparator.comparing(LogEntry::timestamp).reversed() :
                                Comparator.comparing(LogEntry::timestamp))
                        .collect(Collectors.toList());
            case "level":
                return data.stream()
                        .sorted(descending ?
                                Comparator.comparing(LogEntry::level).reversed() :
                                Comparator.comparing(LogEntry::level))
                        .collect(Collectors.toList());
            default:
                return data;
        }
    }

    /**
     * Execute head command (limit to first N results).
     */
    private List<LogEntry> executeHeadCommand(SplCommand command, List<LogEntry> data) {
        int limit = 10; // default
        try {
            limit = Integer.parseInt(command.args().trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid head limit, using default: {}", command.args());
        }

        return data.stream().limit(limit).collect(Collectors.toList());
    }

    /**
     * Execute tail command (limit to last N results).
     */
    private List<LogEntry> executeTailCommand(SplCommand command, List<LogEntry> data) {
        int limit = 10; // default
        try {
            limit = Integer.parseInt(command.args().trim());
        } catch (NumberFormatException e) {
            logger.warn("Invalid tail limit, using default: {}", command.args());
        }

        int size = data.size();
        int start = Math.max(0, size - limit);
        return data.subList(start, size);
    }

    /**
     * Evaluate a condition for where clause.
     */
    private boolean evaluateCondition(LogEntry entry, String condition) {
        // Simple condition evaluation - can be enhanced
        if (condition.contains("=")) {
            String[] parts = condition.split("=", 2);
            if (parts.length == 2) {
                String field = parts[0].trim();
                String value = parts[1].trim().replace("\"", "");

                switch (field.toLowerCase()) {
                    case "level":
                        return value.equals(entry.level());
                    case "source":
                        return value.equals(entry.source());
                    case "message":
                        return entry.message().contains(value);
                }
            }
        }

        return true; // Default to include
    }

    /**
     * Calculate grouped count statistics.
     */
    private Map<String, Object> calculateGroupedCount(List<LogEntry> data, String groupField) {
        Map<String, Long> counts = new HashMap<>();

        for (LogEntry entry : data) {
            String key = getFieldValue(entry, groupField);
            counts.put(key, counts.getOrDefault(key, 0L) + 1);
        }

        return new HashMap<>(counts);
    }

    /**
     * Get field value from log entry.
     */
    private String getFieldValue(LogEntry entry, String field) {
        switch (field.toLowerCase()) {
            case "level":
                return entry.level();
            case "source":
                return entry.source();
            case "message":
                return entry.message();
            default:
                return "unknown";
        }
    }

    /**
         * SPL Command representation.
         */
        public record SplCommand(String command, String args, Map<String, String> arguments) {
    }

    /**
     * SPL Query Result representation.
     */
    public static class SplQueryResult {
        private ResultType resultType;
        private List<LogEntry> logEntries;
        private Map<String, Object> statistics;

        public ResultType getResultType() {
            return resultType;
        }

        public void setResultType(ResultType resultType) {
            this.resultType = resultType;
        }

        public List<LogEntry> getLogEntries() {
            return logEntries;
        }

        public void setLogEntries(List<LogEntry> logEntries) {
            this.logEntries = logEntries;
        }

        public Map<String, Object> getStatistics() {
            return statistics;
        }

        public void setStatistics(Map<String, Object> statistics) {
            this.statistics = statistics;
        }

        public enum ResultType {
            LOG_ENTRIES,
            STATISTICS
        }
    }
}
