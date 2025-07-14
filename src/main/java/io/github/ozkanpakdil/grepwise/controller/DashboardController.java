package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.Dashboard;
import io.github.ozkanpakdil.grepwise.model.DashboardWidget;
import io.github.ozkanpakdil.grepwise.service.DashboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for dashboard management.
 */
@RestController
@RequestMapping("/api/dashboards")
@CrossOrigin(origins = "*")
public class DashboardController {
    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);

    @Autowired
    private DashboardService dashboardService;

    // Dashboard endpoints

    /**
     * Get all dashboards accessible by the current user.
     *
     * @param userId The user ID (in a real app, this would come from authentication)
     * @return List of accessible dashboards
     */
    @GetMapping
    public ResponseEntity<List<Dashboard>> getDashboards(@RequestParam String userId) {
        try {
            List<Dashboard> dashboards = dashboardService.getDashboardsForUser(userId);
            return ResponseEntity.ok(dashboards);
        } catch (Exception e) {
            logger.error("Error retrieving dashboards for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a dashboard by ID.
     *
     * @param id The dashboard ID
     * @param userId The user ID (for access control)
     * @return The dashboard with widgets
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDashboardById(@PathVariable String id, @RequestParam String userId) {
        try {
            Dashboard dashboard = dashboardService.getDashboardById(id, userId);
            return ResponseEntity.ok(dashboard);
        } catch (IllegalArgumentException e) {
            logger.warn("Access denied or dashboard not found {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error retrieving dashboard {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new dashboard.
     *
     * @param dashboardRequest The dashboard creation request
     * @return The created dashboard
     */
    @PostMapping
    public ResponseEntity<?> createDashboard(@RequestBody DashboardRequest dashboardRequest) {
        try {
            Dashboard dashboard = convertRequestToDashboard(dashboardRequest);
            Dashboard createdDashboard = dashboardService.createDashboard(dashboard);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdDashboard);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid dashboard creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating dashboard: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update an existing dashboard.
     *
     * @param id The dashboard ID
     * @param dashboardRequest The dashboard update request
     * @return The updated dashboard
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDashboard(@PathVariable String id, @RequestBody DashboardRequest dashboardRequest) {
        try {
            Dashboard dashboard = convertRequestToDashboard(dashboardRequest);
            dashboard.setId(id);
            Dashboard updatedDashboard = dashboardService.updateDashboard(dashboard);
            return ResponseEntity.ok(updatedDashboard);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid dashboard update request for {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating dashboard {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Delete a dashboard.
     *
     * @param id The dashboard ID
     * @param userId The user ID (for access control)
     * @return Success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDashboard(@PathVariable String id, @RequestParam String userId) {
        try {
            boolean deleted = dashboardService.deleteDashboard(id, userId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Dashboard deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Access denied or dashboard not found {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting dashboard {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Share or unshare a dashboard.
     *
     * @param id The dashboard ID
     * @param shareRequest The share request
     * @return The updated dashboard
     */
    @PutMapping("/{id}/share")
    public ResponseEntity<?> shareDashboard(@PathVariable String id, @RequestBody ShareRequest shareRequest) {
        try {
            Dashboard dashboard = dashboardService.shareDashboard(id, shareRequest.isShared(), shareRequest.getUserId());
            return ResponseEntity.ok(dashboard);
        } catch (IllegalArgumentException e) {
            logger.warn("Access denied or dashboard not found {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error sharing dashboard {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    // Widget endpoints

    /**
     * Add a widget to a dashboard.
     *
     * @param dashboardId The dashboard ID
     * @param widgetRequest The widget creation request
     * @return The created widget
     */
    @PostMapping("/{dashboardId}/widgets")
    public ResponseEntity<?> addWidget(@PathVariable String dashboardId, @RequestBody WidgetRequest widgetRequest) {
        try {
            DashboardWidget widget = convertRequestToWidget(widgetRequest);
            widget.setDashboardId(dashboardId);
            DashboardWidget createdWidget = dashboardService.addWidget(widget, widgetRequest.getUserId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdWidget);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid widget creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating widget: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update a widget.
     *
     * @param dashboardId The dashboard ID
     * @param widgetId The widget ID
     * @param widgetRequest The widget update request
     * @return The updated widget
     */
    @PutMapping("/{dashboardId}/widgets/{widgetId}")
    public ResponseEntity<?> updateWidget(@PathVariable String dashboardId, @PathVariable String widgetId, 
                                        @RequestBody WidgetRequest widgetRequest) {
        try {
            DashboardWidget widget = convertRequestToWidget(widgetRequest);
            widget.setId(widgetId);
            widget.setDashboardId(dashboardId);
            DashboardWidget updatedWidget = dashboardService.updateWidget(widget, widgetRequest.getUserId());
            return ResponseEntity.ok(updatedWidget);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid widget update request for {}: {}", widgetId, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating widget {}: {}", widgetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Delete a widget.
     *
     * @param dashboardId The dashboard ID
     * @param widgetId The widget ID
     * @param userId The user ID (for access control)
     * @return Success response
     */
    @DeleteMapping("/{dashboardId}/widgets/{widgetId}")
    public ResponseEntity<?> deleteWidget(@PathVariable String dashboardId, @PathVariable String widgetId, 
                                        @RequestParam String userId) {
        try {
            boolean deleted = dashboardService.deleteWidget(widgetId, userId);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Widget deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (IllegalArgumentException e) {
            logger.warn("Access denied or widget not found {}: {}", widgetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error deleting widget {}: {}", widgetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update widget positions (for drag-and-drop operations).
     *
     * @param dashboardId The dashboard ID
     * @param positionRequest The position update request
     * @return Success response
     */
    @PutMapping("/{dashboardId}/widgets/positions")
    public ResponseEntity<?> updateWidgetPositions(@PathVariable String dashboardId, 
                                                 @RequestBody PositionUpdateRequest positionRequest) {
        try {
            int updatedCount = dashboardService.updateWidgetPositions(
                positionRequest.getWidgetPositions(), positionRequest.getUserId());
            return ResponseEntity.ok(Map.of("message", "Updated " + updatedCount + " widgets"));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid position update request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating widget positions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Execute a widget query and return the results.
     *
     * @param dashboardId The dashboard ID
     * @param widgetId The widget ID
     * @param userId The user ID (for access control)
     * @return The query results
     */
    @GetMapping("/{dashboardId}/widgets/{widgetId}/data")
    public ResponseEntity<?> getWidgetData(@PathVariable String dashboardId, @PathVariable String widgetId, 
                                         @RequestParam String userId) {
        try {
            Map<String, Object> data = dashboardService.executeWidgetQuery(widgetId, userId);
            return ResponseEntity.ok(data);
        } catch (IllegalArgumentException e) {
            logger.warn("Access denied or widget not found {}: {}", widgetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error executing widget query {}: {}", widgetId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get dashboard statistics.
     *
     * @return Dashboard statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getDashboardStatistics() {
        try {
            Map<String, Object> statistics = dashboardService.getDashboardStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            logger.error("Error retrieving dashboard statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper methods

    private Dashboard convertRequestToDashboard(DashboardRequest request) {
        Dashboard dashboard = new Dashboard();
        dashboard.setName(request.getName());
        dashboard.setDescription(request.getDescription());
        dashboard.setCreatedBy(request.getCreatedBy());
        dashboard.setShared(request.isShared());
        return dashboard;
    }

    private DashboardWidget convertRequestToWidget(WidgetRequest request) {
        DashboardWidget widget = new DashboardWidget();
        widget.setTitle(request.getTitle());
        widget.setType(request.getType());
        widget.setQuery(request.getQuery());
        widget.setConfiguration(request.getConfiguration());
        widget.setPositionX(request.getPositionX());
        widget.setPositionY(request.getPositionY());
        widget.setWidth(request.getWidth());
        widget.setHeight(request.getHeight());
        return widget;
    }

    // Request DTOs

    public static class DashboardRequest {
        private String name;
        private String description;
        private String createdBy;
        private boolean isShared = false;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getCreatedBy() { return createdBy; }
        public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
        public boolean isShared() { return isShared; }
        public void setShared(boolean shared) { isShared = shared; }
    }

    public static class WidgetRequest {
        private String title;
        private String type;
        private String query;
        private Map<String, Object> configuration;
        private int positionX;
        private int positionY;
        private int width = 4;
        private int height = 3;
        private String userId; // For access control

        // Getters and setters
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }
        public Map<String, Object> getConfiguration() { return configuration; }
        public void setConfiguration(Map<String, Object> configuration) { this.configuration = configuration; }
        public int getPositionX() { return positionX; }
        public void setPositionX(int positionX) { this.positionX = positionX; }
        public int getPositionY() { return positionY; }
        public void setPositionY(int positionY) { this.positionY = positionY; }
        public int getWidth() { return width; }
        public void setWidth(int width) { this.width = width; }
        public int getHeight() { return height; }
        public void setHeight(int height) { this.height = height; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class ShareRequest {
        private boolean isShared;
        private String userId;

        // Getters and setters
        public boolean isShared() { return isShared; }
        public void setShared(boolean shared) { isShared = shared; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }

    public static class PositionUpdateRequest {
        private Map<String, Map<String, Integer>> widgetPositions;
        private String userId;

        // Getters and setters
        public Map<String, Map<String, Integer>> getWidgetPositions() { return widgetPositions; }
        public void setWidgetPositions(Map<String, Map<String, Integer>> widgetPositions) { this.widgetPositions = widgetPositions; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
    }
}