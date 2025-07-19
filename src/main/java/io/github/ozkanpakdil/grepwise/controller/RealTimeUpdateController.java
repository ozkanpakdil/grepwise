package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.service.RealTimeUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/realtime")
@Tag(name = "Real-Time Updates", description = "API endpoints for real-time data updates using Server-Sent Events")
public class RealTimeUpdateController {

    private static final Logger logger = LoggerFactory.getLogger(RealTimeUpdateController.class);
    private final RealTimeUpdateService realTimeUpdateService;

    @Autowired
    public RealTimeUpdateController(RealTimeUpdateService realTimeUpdateService) {
        this.realTimeUpdateService = realTimeUpdateService;
    }

    @Operation(summary = "Subscribe to real-time log updates", 
               description = "Creates an SSE connection to receive real-time log updates based on the provided query")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SSE connection established successfully",
                     content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
        @ApiResponse(responseCode = "400", description = "Invalid query parameters",
                     content = @Content)
    })
    @GetMapping(value = "/logs", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToLogUpdates(
            @Parameter(description = "Log search query") @RequestParam(required = false) String query,
            @Parameter(description = "Whether the query is a regex") @RequestParam(required = false, defaultValue = "false") boolean isRegex,
            @Parameter(description = "Time range for the query (e.g., '1h', '24h', '7d')") @RequestParam(required = false) String timeRange) {
        
        logger.info("New SSE connection established for log updates with query: {}", query);
        return realTimeUpdateService.createLogUpdateEmitter(query, isRegex, timeRange);
    }

    @Operation(summary = "Subscribe to dashboard widget updates", 
               description = "Creates an SSE connection to receive real-time updates for a specific dashboard widget")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "SSE connection established successfully",
                     content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE)),
        @ApiResponse(responseCode = "400", description = "Invalid widget parameters",
                     content = @Content)
    })
    @GetMapping(value = "/widgets/{widgetId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToWidgetUpdates(
            @Parameter(description = "Widget ID") @PathVariable String widgetId,
            @Parameter(description = "Dashboard ID") @RequestParam String dashboardId) {
        
        logger.info("New SSE connection established for widget updates. Widget ID: {}, Dashboard ID: {}", widgetId, dashboardId);
        return realTimeUpdateService.createWidgetUpdateEmitter(dashboardId, widgetId);
    }

    @Operation(summary = "Get connection statistics", 
               description = "Returns statistics about active SSE connections")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully",
                     content = @Content)
    })
    @GetMapping("/stats")
    public Map<String, Object> getConnectionStats() {
        return realTimeUpdateService.getConnectionStats();
    }
}