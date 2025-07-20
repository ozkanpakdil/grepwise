package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class RealTimeUpdateService {
    private static final Logger logger = LoggerFactory.getLogger(RealTimeUpdateService.class);
    private static final long SSE_TIMEOUT = 300000L; // 5 minutes
    private static final long HEARTBEAT_INTERVAL = 15000L; // 15 seconds

    private LuceneService luceneService;
    private final ExecutorService executorService;

    // Store active SSE connections
    private final Map<String, List<SseEmitter>> logUpdateEmitters = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> widgetUpdateEmitters = new ConcurrentHashMap<>();
    
    // Store query parameters for each emitter
    private final Map<SseEmitter, Map<String, Object>> emitterQueries = new ConcurrentHashMap<>();
    
    // Track connection statistics
    private final Map<String, Long> connectionStats = new ConcurrentHashMap<>();
    private long totalConnections = 0;
    private long activeConnections = 0;

    public RealTimeUpdateService() {
        this.executorService = Executors.newCachedThreadPool();
        
        // Initialize connection stats
        connectionStats.put("totalConnections", 0L);
        connectionStats.put("activeConnections", 0L);
        connectionStats.put("logUpdateConnections", 0L);
        connectionStats.put("widgetUpdateConnections", 0L);
    }
    
    @Autowired
    @org.springframework.context.annotation.Lazy
    public void setLuceneService(LuceneService luceneService) {
        this.luceneService = luceneService;
    }

    /**
     * Creates a new SSE emitter for log updates based on the provided query
     */
    public SseEmitter createLogUpdateEmitter(String query, boolean isRegex, String timeRange) {
        SseEmitter emitter = createEmitter();
        
        // Store query parameters
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("query", query);
        queryParams.put("isRegex", isRegex);
        queryParams.put("timeRange", timeRange);
        queryParams.put("type", "log");
        emitterQueries.put(emitter, queryParams);
        
        // Add to appropriate collection
        String queryKey = generateQueryKey(query, isRegex, timeRange);
        logUpdateEmitters.computeIfAbsent(queryKey, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        // Update stats
        updateConnectionStats("logUpdateConnections", 1);
        
        // Send initial data
        sendInitialLogData(emitter, query, isRegex, timeRange);
        
        return emitter;
    }

    /**
     * Creates a new SSE emitter for widget updates
     */
    public SseEmitter createWidgetUpdateEmitter(String dashboardId, String widgetId) {
        SseEmitter emitter = createEmitter();
        
        // Store widget parameters
        Map<String, Object> widgetParams = new HashMap<>();
        widgetParams.put("dashboardId", dashboardId);
        widgetParams.put("widgetId", widgetId);
        widgetParams.put("type", "widget");
        emitterQueries.put(emitter, widgetParams);
        
        // Add to appropriate collection
        String widgetKey = dashboardId + ":" + widgetId;
        widgetUpdateEmitters.computeIfAbsent(widgetKey, k -> new CopyOnWriteArrayList<>()).add(emitter);
        
        // Update stats
        updateConnectionStats("widgetUpdateConnections", 1);
        
        // Send initial data
        sendInitialWidgetData(emitter, dashboardId, widgetId);
        
        return emitter;
    }

    /**
     * Helper method to create a new SSE emitter with appropriate callbacks
     */
    private SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);
        
        // Update connection stats
        updateConnectionStats("totalConnections", 1);
        updateConnectionStats("activeConnections", 1);
        
        // Set up callbacks
        emitter.onCompletion(() -> {
            removeEmitter(emitter);
            updateConnectionStats("activeConnections", -1);
            logger.debug("SSE connection completed");
        });
        
        emitter.onTimeout(() -> {
            removeEmitter(emitter);
            updateConnectionStats("activeConnections", -1);
            logger.debug("SSE connection timed out");
        });
        
        emitter.onError(ex -> {
            removeEmitter(emitter);
            updateConnectionStats("activeConnections", -1);
            logger.error("SSE connection error", ex);
        });
        
        // Send initial connection event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("Connection established at " + Instant.now())
                    .id(UUID.randomUUID().toString()));
        } catch (IOException e) {
            logger.error("Error sending initial connection event", e);
        }
        
        return emitter;
    }

    /**
     * Removes an emitter from all collections
     */
    private void removeEmitter(SseEmitter emitter) {
        Map<String, Object> params = emitterQueries.remove(emitter);
        if (params == null) return;
        
        if ("log".equals(params.get("type"))) {
            String query = (String) params.get("query");
            boolean isRegex = (boolean) params.get("isRegex");
            String timeRange = (String) params.get("timeRange");
            String queryKey = generateQueryKey(query, isRegex, timeRange);
            
            List<SseEmitter> emitters = logUpdateEmitters.get(queryKey);
            if (emitters != null) {
                emitters.remove(emitter);
                if (emitters.isEmpty()) {
                    logUpdateEmitters.remove(queryKey);
                }
                updateConnectionStats("logUpdateConnections", -1);
            }
        } else if ("widget".equals(params.get("type"))) {
            String dashboardId = (String) params.get("dashboardId");
            String widgetId = (String) params.get("widgetId");
            String widgetKey = dashboardId + ":" + widgetId;
            
            List<SseEmitter> emitters = widgetUpdateEmitters.get(widgetKey);
            if (emitters != null) {
                emitters.remove(emitter);
                if (emitters.isEmpty()) {
                    widgetUpdateEmitters.remove(widgetKey);
                }
                updateConnectionStats("widgetUpdateConnections", -1);
            }
        }
    }

    /**
     * Sends initial log data to a new connection
     */
    private void sendInitialLogData(SseEmitter emitter, String query, boolean isRegex, String timeRange) {
        executorService.execute(() -> {
            try {
                // Check if luceneService is available
                if (luceneService == null) {
                    logger.warn("LuceneService not available yet, cannot fetch initial data");
                    // Send empty data to client
                    emitter.send(SseEmitter.event()
                            .name("initialData")
                            .data(new ArrayList<>())
                            .id(UUID.randomUUID().toString()));
                    return;
                }
                
                // Fetch initial data
                List<LogEntry> logs = luceneService.search(query, isRegex, null, null);
                
                // Send data to client
                emitter.send(SseEmitter.event()
                        .name("initialData")
                        .data(logs)
                        .id(UUID.randomUUID().toString()));
                
                logger.debug("Sent initial log data for query: {}", query);
            } catch (Exception e) {
                logger.error("Error sending initial log data", e);
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * Sends initial widget data to a new connection
     */
    private void sendInitialWidgetData(SseEmitter emitter, String dashboardId, String widgetId) {
        executorService.execute(() -> {
            try {
                // In a real implementation, you would fetch the widget configuration and data
                // For now, we'll just send a placeholder message
                Map<String, Object> widgetData = new HashMap<>();
                widgetData.put("dashboardId", dashboardId);
                widgetData.put("widgetId", widgetId);
                widgetData.put("timestamp", System.currentTimeMillis());
                widgetData.put("message", "Initial widget data");
                
                // Send data to client
                emitter.send(SseEmitter.event()
                        .name("initialData")
                        .data(widgetData)
                        .id(UUID.randomUUID().toString()));
                
                logger.debug("Sent initial widget data for widget: {}", widgetId);
            } catch (Exception e) {
                logger.error("Error sending initial widget data", e);
                emitter.completeWithError(e);
            }
        });
    }

    /**
     * Broadcasts log updates to all connected clients with matching queries
     */
    public void broadcastLogUpdate(LogEntry logEntry) {
        // In a real implementation, you would check which queries match this log entry
        // For simplicity, we'll broadcast to all log update connections
        logUpdateEmitters.forEach((queryKey, emitters) -> {
            // Parse the query key to get query parameters
            String[] parts = queryKey.split(":");
            if (parts.length < 3) return;
            
            String query = parts[0];
            boolean isRegex = Boolean.parseBoolean(parts[1]);
            String timeRange = parts[2];
            
            // Check if this log entry matches the query
            // This is a simplified check - in a real implementation, you would use the search logic
            if (query == null || query.isEmpty() || logEntry.message().contains(query)) {
                // Broadcast to all emitters for this query
                broadcastToEmitters(emitters, "logUpdate", logEntry);
            }
        });
    }

    /**
     * Broadcasts widget updates to all connected clients for a specific widget
     */
    public void broadcastWidgetUpdate(String dashboardId, String widgetId, Object data) {
        String widgetKey = dashboardId + ":" + widgetId;
        List<SseEmitter> emitters = widgetUpdateEmitters.get(widgetKey);
        
        if (emitters != null && !emitters.isEmpty()) {
            broadcastToEmitters(emitters, "widgetUpdate", data);
        }
    }

    /**
     * Helper method to broadcast an event to a list of emitters
     */
    private void broadcastToEmitters(List<SseEmitter> emitters, String eventName, Object data) {
        List<SseEmitter> deadEmitters = new ArrayList<>();
        
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(data)
                        .id(UUID.randomUUID().toString()));
            } catch (Exception e) {
                logger.error("Error broadcasting to emitter", e);
                deadEmitters.add(emitter);
            }
        });
        
        // Remove dead emitters
        deadEmitters.forEach(this::removeEmitter);
    }

    /**
     * Sends heartbeat events to keep connections alive
     */
    @Scheduled(fixedRate = HEARTBEAT_INTERVAL)
    public void sendHeartbeats() {
        // Collect all emitters
        List<SseEmitter> allEmitters = new ArrayList<>();
        logUpdateEmitters.values().forEach(allEmitters::addAll);
        widgetUpdateEmitters.values().forEach(allEmitters::addAll);
        
        // Send heartbeat to each emitter
        List<SseEmitter> deadEmitters = new ArrayList<>();
        allEmitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data(System.currentTimeMillis())
                        .id("heartbeat"));
            } catch (Exception e) {
                logger.debug("Error sending heartbeat, removing emitter", e);
                deadEmitters.add(emitter);
            }
        });
        
        // Remove dead emitters
        deadEmitters.forEach(this::removeEmitter);
    }

    /**
     * Returns statistics about active connections
     */
    public Map<String, Object> getConnectionStats() {
        Map<String, Object> stats = new HashMap<>(connectionStats);
        stats.put("logUpdateQueries", logUpdateEmitters.size());
        stats.put("widgetUpdateSubscriptions", widgetUpdateEmitters.size());
        return stats;
    }

    /**
     * Updates connection statistics
     */
    private synchronized void updateConnectionStats(String key, long delta) {
        connectionStats.compute(key, (k, v) -> (v == null ? 0 : v) + delta);
    }

    /**
     * Generates a unique key for a query
     */
    private String generateQueryKey(String query, boolean isRegex, String timeRange) {
        return (query == null ? "" : query) + ":" + isRegex + ":" + (timeRange == null ? "" : timeRange);
    }
}