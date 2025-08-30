package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.DashboardWidget;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving dashboard widget information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class DashboardWidgetRepository {
    private final Map<String, DashboardWidget> widgets = new ConcurrentHashMap<>();

    /**
     * Save a dashboard widget.
     *
     * @param widget The widget to save
     * @return The saved widget with a generated ID
     */
    public DashboardWidget save(DashboardWidget widget) {
        if (widget.getId() == null || widget.getId().isEmpty()) {
            widget.setId(UUID.randomUUID().toString());
        }

        // Set timestamps if not already set
        long now = System.currentTimeMillis();
        if (widget.getCreatedAt() == 0) {
            widget.setCreatedAt(now);
        }
        widget.setUpdatedAt(now);

        widgets.put(widget.getId(), widget);
        return widget;
    }

    /**
     * Find a widget by ID.
     *
     * @param id The ID of the widget to find
     * @return The widget, or null if not found
     */
    public DashboardWidget findById(String id) {
        return widgets.get(id);
    }

    /**
     * Find all widgets.
     *
     * @return A list of all widgets
     */
    public List<DashboardWidget> findAll() {
        return new ArrayList<>(widgets.values());
    }

    /**
     * Find widgets by dashboard ID.
     *
     * @param dashboardId The dashboard ID to filter by
     * @return A list of widgets belonging to the specified dashboard
     */
    public List<DashboardWidget> findByDashboardId(String dashboardId) {
        return widgets.values().stream()
                .filter(widget -> dashboardId.equals(widget.getDashboardId()))
                .collect(Collectors.toList());
    }

    /**
     * Find widgets by type.
     *
     * @param type The widget type to filter by
     * @return A list of widgets of the specified type
     */
    public List<DashboardWidget> findByType(String type) {
        return widgets.values().stream()
                .filter(widget -> type.equals(widget.getType()))
                .collect(Collectors.toList());
    }

    /**
     * Find widgets by title (case-insensitive partial match).
     *
     * @param title The title to search for
     * @return A list of widgets with titles containing the specified text
     */
    public List<DashboardWidget> findByTitleContaining(String title) {
        return widgets.values().stream()
                .filter(widget -> widget.getTitle() != null &&
                        widget.getTitle().toLowerCase().contains(title.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Delete a widget by ID.
     *
     * @param id The ID of the widget to delete
     * @return true if the widget was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return widgets.remove(id) != null;
    }

    /**
     * Delete all widgets belonging to a dashboard.
     *
     * @param dashboardId The dashboard ID
     * @return The number of widgets deleted
     */
    public int deleteByDashboardId(String dashboardId) {
        List<String> widgetIds = widgets.values().stream()
                .filter(widget -> dashboardId.equals(widget.getDashboardId()))
                .map(DashboardWidget::getId)
                .collect(Collectors.toList());

        int deletedCount = 0;
        for (String widgetId : widgetIds) {
            if (widgets.remove(widgetId) != null) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    /**
     * Check if a widget with the given title already exists in a dashboard.
     *
     * @param title       The title to check
     * @param dashboardId The dashboard ID
     * @return true if a widget with the title exists in the dashboard, false otherwise
     */
    public boolean existsByTitleAndDashboardId(String title, String dashboardId) {
        return widgets.values().stream()
                .anyMatch(widget -> title.equals(widget.getTitle()) &&
                        dashboardId.equals(widget.getDashboardId()));
    }

    /**
     * Get the total number of widgets.
     *
     * @return The total number of widgets
     */
    public int count() {
        return widgets.size();
    }

    /**
     * Get the number of widgets in a dashboard.
     *
     * @param dashboardId The dashboard ID
     * @return The number of widgets in the dashboard
     */
    public int countByDashboardId(String dashboardId) {
        return (int) widgets.values().stream()
                .filter(widget -> dashboardId.equals(widget.getDashboardId()))
                .count();
    }

    /**
     * Update an existing widget.
     *
     * @param widget The widget to update
     * @return The updated widget, or null if the widget doesn't exist
     */
    public DashboardWidget update(DashboardWidget widget) {
        if (widget.getId() == null || !widgets.containsKey(widget.getId())) {
            return null;
        }

        widget.updateTimestamp();
        widgets.put(widget.getId(), widget);
        return widget;
    }

    /**
     * Update widget positions in bulk (for drag-and-drop operations).
     *
     * @param widgetPositions Map of widget ID to position updates
     * @return The number of widgets updated
     */
    public int updatePositions(Map<String, Map<String, Integer>> widgetPositions) {
        int updatedCount = 0;

        for (Map.Entry<String, Map<String, Integer>> entry : widgetPositions.entrySet()) {
            String widgetId = entry.getKey();
            Map<String, Integer> position = entry.getValue();

            DashboardWidget widget = widgets.get(widgetId);
            if (widget != null) {
                if (position.containsKey("x")) {
                    widget.setPositionX(position.get("x"));
                }
                if (position.containsKey("y")) {
                    widget.setPositionY(position.get("y"));
                }
                if (position.containsKey("width")) {
                    widget.setWidth(position.get("width"));
                }
                if (position.containsKey("height")) {
                    widget.setHeight(position.get("height"));
                }

                widget.updateTimestamp();
                widgets.put(widgetId, widget);
                updatedCount++;
            }
        }

        return updatedCount;
    }
}