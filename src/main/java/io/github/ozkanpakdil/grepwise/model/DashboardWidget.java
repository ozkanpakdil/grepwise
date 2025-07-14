package io.github.ozkanpakdil.grepwise.model;

import java.util.Map;
import java.util.Objects;

/**
 * Represents a widget/visualization component within a dashboard.
 */
public class DashboardWidget {
    private String id;
    private String dashboardId;
    private String title;
    private String type; // chart, table, metric, etc.
    private String query; // SPL query for data
    private Map<String, Object> configuration; // Widget-specific configuration
    private int positionX;
    private int positionY;
    private int width;
    private int height;
    private long createdAt;
    private long updatedAt;

    // Constructors
    public DashboardWidget() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public DashboardWidget(String dashboardId, String title, String type, String query) {
        this();
        this.dashboardId = dashboardId;
        this.title = title;
        this.type = type;
        this.query = query;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDashboardId() {
        return dashboardId;
    }

    public void setDashboardId(String dashboardId) {
        this.dashboardId = dashboardId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Map<String, Object> configuration) {
        this.configuration = configuration;
    }

    public int getPositionX() {
        return positionX;
    }

    public void setPositionX(int positionX) {
        this.positionX = positionX;
    }

    public int getPositionY() {
        return positionY;
    }

    public void setPositionY(int positionY) {
        this.positionY = positionY;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DashboardWidget widget = (DashboardWidget) o;
        return Objects.equals(id, widget.id) &&
               Objects.equals(dashboardId, widget.dashboardId) &&
               Objects.equals(title, widget.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, dashboardId, title);
    }

    @Override
    public String toString() {
        return "DashboardWidget{" +
                "id='" + id + '\'' +
                ", dashboardId='" + dashboardId + '\'' +
                ", title='" + title + '\'' +
                ", type='" + type + '\'' +
                ", query='" + query + '\'' +
                ", positionX=" + positionX +
                ", positionY=" + positionY +
                ", width=" + width +
                ", height=" + height +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}