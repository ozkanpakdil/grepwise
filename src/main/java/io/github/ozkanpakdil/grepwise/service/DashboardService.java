package io.github.ozkanpakdil.grepwise.service;

import io.github.ozkanpakdil.grepwise.model.Dashboard;
import io.github.ozkanpakdil.grepwise.model.DashboardWidget;
import io.github.ozkanpakdil.grepwise.repository.DashboardRepository;
import io.github.ozkanpakdil.grepwise.repository.DashboardWidgetRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing dashboards and dashboard widgets.
 */
@Service
public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DashboardWidgetRepository widgetRepository;

    @Autowired
    private SplQueryService splQueryService;

    // Dashboard CRUD Operations

    /**
     * Create a new dashboard.
     *
     * @param dashboard The dashboard to create
     * @return The created dashboard
     */
    public Dashboard createDashboard(Dashboard dashboard) {
        logger.info("Creating new dashboard: {} by user: {}", dashboard.getName(), dashboard.getCreatedBy());

        // Validate dashboard
        validateDashboard(dashboard);

        // Check if dashboard with same name already exists for this user
        if (dashboardRepository.existsByNameAndCreatedBy(dashboard.getName(), dashboard.getCreatedBy())) {
            throw new IllegalArgumentException("Dashboard with name '" + dashboard.getName() + "' already exists for this user");
        }

        return dashboardRepository.save(dashboard);
    }

    /**
     * Update an existing dashboard.
     *
     * @param dashboard The dashboard to update
     * @return The updated dashboard
     */
    public Dashboard updateDashboard(Dashboard dashboard) {
        logger.info("Updating dashboard: {}", dashboard.getId());

        // Validate dashboard
        validateDashboard(dashboard);

        // Check if dashboard exists
        Dashboard existingDashboard = dashboardRepository.findById(dashboard.getId());
        if (existingDashboard == null) {
            throw new IllegalArgumentException("Dashboard with ID '" + dashboard.getId() + "' not found");
        }

        // Check if name conflicts with another dashboard for the same user
        if (!existingDashboard.getName().equals(dashboard.getName()) &&
                dashboardRepository.existsByNameAndCreatedBy(dashboard.getName(), dashboard.getCreatedBy())) {
            throw new IllegalArgumentException("Dashboard with name '" + dashboard.getName() + "' already exists for this user");
        }

        return dashboardRepository.update(dashboard);
    }

    /**
     * Get all dashboards accessible by a user.
     *
     * @param userId The user ID
     * @return List of accessible dashboards
     */
    public List<Dashboard> getDashboardsForUser(String userId) {
        List<Dashboard> dashboards = dashboardRepository.findAccessibleByUser(userId);

        // Load widgets for each dashboard
        for (Dashboard dashboard : dashboards) {
            List<DashboardWidget> widgets = widgetRepository.findByDashboardId(dashboard.getId());
            dashboard.setWidgets(widgets);
        }

        return dashboards;
    }

    /**
     * Get a dashboard by ID.
     *
     * @param id     The dashboard ID
     * @param userId The requesting user ID (for access control)
     * @return The dashboard with widgets
     */
    public Dashboard getDashboardById(String id, String userId) {
        Dashboard dashboard = dashboardRepository.findById(id);
        if (dashboard == null) {
            throw new IllegalArgumentException("Dashboard with ID '" + id + "' not found");
        }

        // Check access permissions
        if (!dashboard.getCreatedBy().equals(userId) && !dashboard.isShared()) {
            throw new IllegalArgumentException("Access denied to dashboard '" + id + "'");
        }

        // Load widgets
        List<DashboardWidget> widgets = widgetRepository.findByDashboardId(id);
        dashboard.setWidgets(widgets);

        return dashboard;
    }

    /**
     * Delete a dashboard and all its widgets.
     *
     * @param id     The dashboard ID
     * @param userId The requesting user ID (for access control)
     * @return true if deleted successfully
     */
    public boolean deleteDashboard(String id, String userId) {
        logger.info("Deleting dashboard: {} by user: {}", id, userId);

        Dashboard dashboard = dashboardRepository.findById(id);
        if (dashboard == null) {
            throw new IllegalArgumentException("Dashboard with ID '" + id + "' not found");
        }

        // Check if user has permission to delete
        if (!dashboard.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("Access denied: Only the creator can delete this dashboard");
        }

        // Delete all widgets first
        int deletedWidgets = widgetRepository.deleteByDashboardId(id);
        logger.info("Deleted {} widgets from dashboard {}", deletedWidgets, id);

        // Delete the dashboard
        return dashboardRepository.deleteById(id);
    }

    /**
     * Share or unshare a dashboard.
     *
     * @param id       The dashboard ID
     * @param isShared Whether to share the dashboard
     * @param userId   The requesting user ID (for access control)
     * @return The updated dashboard
     */
    public Dashboard shareDashboard(String id, boolean isShared, String userId) {
        logger.info("Setting dashboard {} shared status to: {} by user: {}", id, isShared, userId);

        Dashboard dashboard = dashboardRepository.findById(id);
        if (dashboard == null) {
            throw new IllegalArgumentException("Dashboard with ID '" + id + "' not found");
        }

        // Check if user has permission to share
        if (!dashboard.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("Access denied: Only the creator can share this dashboard");
        }

        dashboard.setShared(isShared);
        return dashboardRepository.update(dashboard);
    }

    // Widget CRUD Operations

    /**
     * Add a widget to a dashboard.
     *
     * @param widget The widget to add
     * @param userId The requesting user ID (for access control)
     * @return The created widget
     */
    public DashboardWidget addWidget(DashboardWidget widget, String userId) {
        logger.info("Adding widget '{}' to dashboard: {}", widget.getTitle(), widget.getDashboardId());

        // Validate widget
        validateWidget(widget);

        // Check if dashboard exists and user has access
        Dashboard dashboard = getDashboardById(widget.getDashboardId(), userId);

        // Check if user has permission to modify
        if (!dashboard.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("Access denied: Only the creator can modify this dashboard");
        }

        // Check if widget with same title already exists in this dashboard
        if (widgetRepository.existsByTitleAndDashboardId(widget.getTitle(), widget.getDashboardId())) {
            throw new IllegalArgumentException("Widget with title '" + widget.getTitle() + "' already exists in this dashboard");
        }

        return widgetRepository.save(widget);
    }

    /**
     * Update a widget.
     *
     * @param widget The widget to update
     * @param userId The requesting user ID (for access control)
     * @return The updated widget
     */
    public DashboardWidget updateWidget(DashboardWidget widget, String userId) {
        logger.info("Updating widget: {}", widget.getId());

        // Validate widget
        validateWidget(widget);

        // Check if widget exists
        DashboardWidget existingWidget = widgetRepository.findById(widget.getId());
        if (existingWidget == null) {
            throw new IllegalArgumentException("Widget with ID '" + widget.getId() + "' not found");
        }

        // Check if dashboard exists and user has access
        Dashboard dashboard = getDashboardById(widget.getDashboardId(), userId);

        // Check if user has permission to modify
        if (!dashboard.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("Access denied: Only the creator can modify this dashboard");
        }

        // Check if title conflicts with another widget in the same dashboard
        if (!existingWidget.getTitle().equals(widget.getTitle()) &&
                widgetRepository.existsByTitleAndDashboardId(widget.getTitle(), widget.getDashboardId())) {
            throw new IllegalArgumentException("Widget with title '" + widget.getTitle() + "' already exists in this dashboard");
        }

        return widgetRepository.update(widget);
    }

    /**
     * Delete a widget.
     *
     * @param widgetId The widget ID
     * @param userId   The requesting user ID (for access control)
     * @return true if deleted successfully
     */
    public boolean deleteWidget(String widgetId, String userId) {
        logger.info("Deleting widget: {} by user: {}", widgetId, userId);

        DashboardWidget widget = widgetRepository.findById(widgetId);
        if (widget == null) {
            throw new IllegalArgumentException("Widget with ID '" + widgetId + "' not found");
        }

        // Check if dashboard exists and user has access
        Dashboard dashboard = getDashboardById(widget.getDashboardId(), userId);

        // Check if user has permission to modify
        if (!dashboard.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("Access denied: Only the creator can modify this dashboard");
        }

        return widgetRepository.deleteById(widgetId);
    }

    /**
     * Update widget positions (for drag-and-drop operations).
     *
     * @param widgetPositions Map of widget ID to position updates
     * @param userId          The requesting user ID (for access control)
     * @return The number of widgets updated
     */
    public int updateWidgetPositions(Map<String, Map<String, Integer>> widgetPositions, String userId) {
        logger.info("Updating positions for {} widgets by user: {}", widgetPositions.size(), userId);

        // Validate access for all widgets
        for (String widgetId : widgetPositions.keySet()) {
            DashboardWidget widget = widgetRepository.findById(widgetId);
            if (widget == null) {
                throw new IllegalArgumentException("Widget with ID '" + widgetId + "' not found");
            }

            // Check if dashboard exists and user has access
            Dashboard dashboard = getDashboardById(widget.getDashboardId(), userId);

            // Check if user has permission to modify
            if (!dashboard.getCreatedBy().equals(userId)) {
                throw new IllegalArgumentException("Access denied: Only the creator can modify this dashboard");
            }
        }

        return widgetRepository.updatePositions(widgetPositions);
    }

    /**
     * Execute a widget query and return the results.
     *
     * @param widgetId The widget ID
     * @param userId   The requesting user ID (for access control)
     * @return The query results
     */
    public Map<String, Object> executeWidgetQuery(String widgetId, String userId) {
        DashboardWidget widget = widgetRepository.findById(widgetId);
        if (widget == null) {
            throw new IllegalArgumentException("Widget with ID '" + widgetId + "' not found");
        }

        // Check if dashboard exists and user has access
        getDashboardById(widget.getDashboardId(), userId);

        // Execute the query using SplQueryService
        try {
            var result = splQueryService.executeSplQuery(widget.getQuery());
            Map<String, Object> response = new HashMap<>();
            response.put("resultType", result.getResultType().toString());
            response.put("logEntries", result.getLogEntries());
            response.put("statistics", result.getStatistics());
            return response;
        } catch (Exception e) {
            logger.error("Error executing widget query: {}", e.getMessage());
            throw new RuntimeException("Failed to execute widget query: " + e.getMessage());
        }
    }

    // Utility Methods

    /**
     * Get dashboard statistics.
     *
     * @return Map containing dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDashboards", dashboardRepository.count());
        stats.put("sharedDashboards", dashboardRepository.countShared());
        stats.put("totalWidgets", widgetRepository.count());
        return stats;
    }

    /**
     * Validate a dashboard.
     *
     * @param dashboard The dashboard to validate
     */
    private void validateDashboard(Dashboard dashboard) {
        if (dashboard == null) {
            throw new IllegalArgumentException("Dashboard cannot be null");
        }
        if (dashboard.getName() == null || dashboard.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Dashboard name is required");
        }
        if (dashboard.getCreatedBy() == null || dashboard.getCreatedBy().trim().isEmpty()) {
            throw new IllegalArgumentException("Dashboard creator is required");
        }
        if (dashboard.getName().length() > 100) {
            throw new IllegalArgumentException("Dashboard name cannot exceed 100 characters");
        }
        if (dashboard.getDescription() != null && dashboard.getDescription().length() > 1000) {
            throw new IllegalArgumentException("Dashboard description cannot exceed 1000 characters");
        }
    }

    /**
     * Validate a dashboard widget.
     *
     * @param widget The widget to validate
     */
    private void validateWidget(DashboardWidget widget) {
        if (widget == null) {
            throw new IllegalArgumentException("Widget cannot be null");
        }
        if (widget.getDashboardId() == null || widget.getDashboardId().trim().isEmpty()) {
            throw new IllegalArgumentException("Widget dashboard ID is required");
        }
        if (widget.getTitle() == null || widget.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Widget title is required");
        }
        if (widget.getType() == null || widget.getType().trim().isEmpty()) {
            throw new IllegalArgumentException("Widget type is required");
        }
        if (widget.getQuery() == null || widget.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Widget query is required");
        }
        if (widget.getTitle().length() > 100) {
            throw new IllegalArgumentException("Widget title cannot exceed 100 characters");
        }
        if (widget.getWidth() < 1 || widget.getWidth() > 12) {
            throw new IllegalArgumentException("Widget width must be between 1 and 12");
        }
        if (widget.getHeight() < 1 || widget.getHeight() > 12) {
            throw new IllegalArgumentException("Widget height must be between 1 and 12");
        }
    }
}
