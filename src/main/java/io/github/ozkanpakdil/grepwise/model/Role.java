package io.github.ozkanpakdil.grepwise.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a role in the system with associated permissions.
 */
public class Role {
    private String id;
    private String name;
    private String description;
    private List<Permission> permissions;
    private long createdAt;
    private long updatedAt;

    public Role() {
        this.permissions = new ArrayList<>();
    }

    public Role(String id, String name, String description, List<Permission> permissions, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = permissions != null ? permissions : new ArrayList<>();
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

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

    public List<Permission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
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

    /**
     * Checks if this role has a specific permission.
     *
     * @param permissionName The name of the permission to check
     * @return true if the role has the permission, false otherwise
     */
    public boolean hasPermission(String permissionName) {
        return permissions.stream()
                .anyMatch(permission -> permission.getName().equals(permissionName));
    }

    /**
     * Adds a permission to this role if it doesn't already exist.
     *
     * @param permission The permission to add
     * @return true if the permission was added, false if it already existed
     */
    public boolean addPermission(Permission permission) {
        if (hasPermission(permission.getName())) {
            return false;
        }
        return permissions.add(permission);
    }

    /**
     * Removes a permission from this role.
     *
     * @param permissionName The name of the permission to remove
     * @return true if the permission was removed, false if it didn't exist
     */
    public boolean removePermission(String permissionName) {
        return permissions.removeIf(permission -> permission.getName().equals(permissionName));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return createdAt == role.createdAt &&
                updatedAt == role.updatedAt &&
                Objects.equals(id, role.id) &&
                Objects.equals(name, role.name) &&
                Objects.equals(description, role.description) &&
                Objects.equals(permissions, role.permissions);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, description, permissions, createdAt, updatedAt);
    }

    @Override
    public String toString() {
        return "Role{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", permissions=" + permissions +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}