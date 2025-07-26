package io.github.ozkanpakdil.grepwise.model;

import java.util.Objects;

/**
 * Entity representing a tenant in a multi-tenant system.
 * A tenant is an organization or a group that has its own isolated data and users.
 */
public class Tenant {
    private String id;
    private String name;
    private String description;
    private boolean active;
    private long createdAt;
    private long updatedAt;

    /**
     * Default constructor.
     */
    public Tenant() {
    }

    /**
     * Constructor with all fields.
     *
     * @param id          The tenant ID
     * @param name        The tenant name
     * @param description The tenant description
     * @param active      Whether the tenant is active
     * @param createdAt   The creation timestamp
     * @param updatedAt   The last update timestamp
     */
    public Tenant(String id, String name, String description, boolean active, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Get the tenant ID.
     *
     * @return The tenant ID
     */
    public String getId() {
        return id;
    }

    /**
     * Set the tenant ID.
     *
     * @param id The tenant ID
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Get the tenant name.
     *
     * @return The tenant name
     */
    public String getName() {
        return name;
    }

    /**
     * Set the tenant name.
     *
     * @param name The tenant name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the tenant description.
     *
     * @return The tenant description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the tenant description.
     *
     * @param description The tenant description
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Check if the tenant is active.
     *
     * @return true if the tenant is active, false otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Set whether the tenant is active.
     *
     * @param active Whether the tenant is active
     */
    public void setActive(boolean active) {
        this.active = active;
    }

    /**
     * Get the creation timestamp.
     *
     * @return The creation timestamp
     */
    public long getCreatedAt() {
        return createdAt;
    }

    /**
     * Set the creation timestamp.
     *
     * @param createdAt The creation timestamp
     */
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Get the last update timestamp.
     *
     * @return The last update timestamp
     */
    public long getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Set the last update timestamp.
     *
     * @param updatedAt The last update timestamp
     */
    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tenant tenant = (Tenant) o;
        return active == tenant.active &&
                createdAt == tenant.createdAt &&
                updatedAt == tenant.updatedAt &&
                Objects.equals(id, tenant.id) &&
                Objects.equals(name, tenant.name) &&
                Objects.equals(description, tenant.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, active, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Tenant{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", active=" + active +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}