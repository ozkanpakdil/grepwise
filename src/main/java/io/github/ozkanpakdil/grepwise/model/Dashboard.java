package io.github.ozkanpakdil.grepwise.model;

import java.util.List;
import java.util.Objects;

/**
 * Represents a dashboard configuration for log visualization.
 */
public class Dashboard {
    private String id;
    private String name;
    private String description;
    private String createdBy;
    private long createdAt;
    private long updatedAt;
    private boolean isShared = false;
    private List<DashboardWidget> widgets;

    // Constructors
    public Dashboard() {
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Dashboard(String name, String description, String createdBy) {
        this();
        this.name = name;
        this.description = description;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
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

    public boolean isShared() {
        return isShared;
    }

    public void setShared(boolean shared) {
        isShared = shared;
    }

    public List<DashboardWidget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<DashboardWidget> widgets) {
        this.widgets = widgets;
    }

    public void updateTimestamp() {
        this.updatedAt = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Dashboard dashboard = (Dashboard) o;
        return Objects.equals(id, dashboard.id) &&
                Objects.equals(name, dashboard.name) &&
                Objects.equals(createdBy, dashboard.createdBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, createdBy);
    }

    @Override
    public String toString() {
        return "Dashboard{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdBy='" + createdBy + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", isShared=" + isShared +
                ", widgets=" + (widgets != null ? widgets.size() : 0) + " widgets" +
                '}';
    }
}
