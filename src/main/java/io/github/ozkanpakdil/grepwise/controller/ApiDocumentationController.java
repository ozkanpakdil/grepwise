package io.github.ozkanpakdil.grepwise.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for API documentation.
 * This provides a simple API documentation page that lists all available endpoints.
 */
@Controller
@RequestMapping("/api-docs")
public class ApiDocumentationController {

    private final LogSearchController logSearchController;

    public ApiDocumentationController(LogSearchController logSearchController) {
        this.logSearchController = logSearchController;
    }

    /**
     * Returns the API documentation page.
     *
     * @return HTML page with API documentation
     */
    @GetMapping(produces = "text/html")
    public String getApiDocumentation(org.springframework.ui.Model model) {
        model.addAttribute("controller", this);
        return "api-documentation";
    }

    /**
     * Returns the API endpoints as JSON.
     *
     * @return JSON object with API endpoints
     */
    @GetMapping(produces = "application/json")
    @ResponseBody
    public Map<String, Object> getApiEndpoints() {
        Map<String, Object> response = new HashMap<>();
        response.put("title", "GrepWise API Documentation");
        response.put("version", "1.0.0");
        response.put("description", "API documentation for GrepWise, an open-source alternative to Splunk for log analysis and monitoring.");

        List<Map<String, Object>> endpoints = new ArrayList<>();

        // Log Search Endpoints
        addEndpoint(endpoints, "GET", "/api/logs/search",
                "Search logs with a query string",
                "Parameters: query (string), isRegex (boolean), timeRange (string), startTime (long), endTime (long)");

        addEndpoint(endpoints, "GET", "/api/logs/spl",
                "Execute an SPL query",
                "Parameters: splQuery (string)");

        addEndpoint(endpoints, "GET", "/api/logs/{id}",
                "Get a log entry by ID",
                "Parameters: id (string)");

        addEndpoint(endpoints, "GET", "/api/logs",
                "Get all logs",
                "No parameters");

        addEndpoint(endpoints, "GET", "/api/logs/level/{level}",
                "Get logs by level",
                "Parameters: level (string)");

        addEndpoint(endpoints, "GET", "/api/logs/source/{source}",
                "Get logs by source",
                "Parameters: source (string)");

        addEndpoint(endpoints, "GET", "/api/logs/timerange",
                "Get logs by time range",
                "Parameters: startTime (long), endTime (long)");

        addEndpoint(endpoints, "GET", "/api/logs/levels",
                "Get all log levels",
                "No parameters");

        addEndpoint(endpoints, "GET", "/api/logs/sources",
                "Get all log sources",
                "No parameters");

        addEndpoint(endpoints, "GET", "/api/logs/count",
                "Get log count by time slots",
                "Parameters: query (string), isRegex (boolean), timeRange (string), startTime (long), endTime (long), slots (int)");

        // Alarm Endpoints
        addEndpoint(endpoints, "GET", "/api/alarms",
                "Get all alarms",
                "No parameters");

        addEndpoint(endpoints, "GET", "/api/alarms/{id}",
                "Get alarm by ID",
                "Parameters: id (string)");

        addEndpoint(endpoints, "POST", "/api/alarms",
                "Create a new alarm",
                "Request body: AlarmRequest (name, description, query, condition, threshold, timeWindowMinutes, enabled, notificationEmail, notificationChannels, throttleWindowMinutes, maxNotificationsPerWindow, groupingKey, groupingWindowMinutes)");

        addEndpoint(endpoints, "PUT", "/api/alarms/{id}",
                "Update an alarm",
                "Parameters: id (string), Request body: AlarmRequest");

        addEndpoint(endpoints, "DELETE", "/api/alarms/{id}",
                "Delete an alarm",
                "Parameters: id (string)");

        addEndpoint(endpoints, "PUT", "/api/alarms/{id}/toggle",
                "Toggle alarm enabled/disabled",
                "Parameters: id (string)");

        addEndpoint(endpoints, "GET", "/api/alarms/enabled/{enabled}",
                "Get alarms by enabled status",
                "Parameters: enabled (boolean)");

        addEndpoint(endpoints, "POST", "/api/alarms/{id}/evaluate",
                "Evaluate an alarm",
                "Parameters: id (string)");

        addEndpoint(endpoints, "GET", "/api/alarms/statistics",
                "Get alarm statistics",
                "No parameters");

        // Dashboard Endpoints
        addEndpoint(endpoints, "GET", "/api/dashboards",
                "Get all dashboards",
                "Parameters: userId (string, optional)");

        addEndpoint(endpoints, "GET", "/api/dashboards/{id}",
                "Get dashboard by ID",
                "Parameters: id (string), userId (string, optional)");

        addEndpoint(endpoints, "POST", "/api/dashboards",
                "Create a new dashboard",
                "Request body: DashboardRequest (name, description, createdBy, shared)");

        addEndpoint(endpoints, "PUT", "/api/dashboards/{id}",
                "Update a dashboard",
                "Parameters: id (string), Request body: DashboardRequest");

        addEndpoint(endpoints, "DELETE", "/api/dashboards/{id}",
                "Delete a dashboard",
                "Parameters: id (string), userId (string, optional)");

        addEndpoint(endpoints, "POST", "/api/dashboards/{id}/share",
                "Share a dashboard",
                "Parameters: id (string), Request body: ShareRequest (shared, userId)");

        addEndpoint(endpoints, "POST", "/api/dashboards/{dashboardId}/widgets",
                "Add a widget to a dashboard",
                "Parameters: dashboardId (string), Request body: WidgetRequest (title, type, query, configuration, positionX, positionY, width, height, userId)");

        addEndpoint(endpoints, "PUT", "/api/dashboards/{dashboardId}/widgets/{widgetId}",
                "Update a widget",
                "Parameters: dashboardId (string), widgetId (string), Request body: WidgetRequest");

        addEndpoint(endpoints, "DELETE", "/api/dashboards/{dashboardId}/widgets/{widgetId}",
                "Delete a widget",
                "Parameters: dashboardId (string), widgetId (string), userId (string, optional)");

        addEndpoint(endpoints, "PUT", "/api/dashboards/{dashboardId}/widget-positions",
                "Update widget positions",
                "Parameters: dashboardId (string), Request body: PositionUpdateRequest (widgetPositions, userId)");

        addEndpoint(endpoints, "GET", "/api/dashboards/{dashboardId}/widgets/{widgetId}/data",
                "Get widget data",
                "Parameters: dashboardId (string), widgetId (string), userId (string, optional)");

        addEndpoint(endpoints, "GET", "/api/dashboards/statistics",
                "Get dashboard statistics",
                "No parameters");

        // Log Directory Config Endpoints
        addEndpoint(endpoints, "GET", "/api/config/log-directories",
                "Get all log directory configurations",
                "No parameters");

        addEndpoint(endpoints, "GET", "/api/config/log-directories/{id}",
                "Get a log directory configuration by ID",
                "Parameters: id (string)");

        addEndpoint(endpoints, "POST", "/api/config/log-directories",
                "Create a new log directory configuration",
                "Request body: LogDirectoryConfig");

        addEndpoint(endpoints, "PUT", "/api/config/log-directories/{id}",
                "Update a log directory configuration",
                "Parameters: id (string), Request body: LogDirectoryConfig");

        addEndpoint(endpoints, "DELETE", "/api/config/log-directories/{id}",
                "Delete a log directory configuration",
                "Parameters: id (string)");

        addEndpoint(endpoints, "POST", "/api/config/log-directories/{id}/scan",
                "Trigger a scan of a specific directory",
                "Parameters: id (string)");

        addEndpoint(endpoints, "POST", "/api/config/log-directories/scan-all",
                "Trigger a scan of all directories",
                "No parameters");

        response.put("endpoints", endpoints);
        return response;
    }

    private void addEndpoint(List<Map<String, Object>> endpoints, String method, String path, String description, String parameters) {
        Map<String, Object> endpoint = new HashMap<>();
        endpoint.put("method", method);
        endpoint.put("path", path);
        endpoint.put("description", description);
        endpoint.put("parameters", parameters);
        endpoints.add(endpoint);
    }
}