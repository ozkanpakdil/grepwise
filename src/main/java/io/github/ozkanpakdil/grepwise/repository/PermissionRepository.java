package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.Permission;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving permission information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class PermissionRepository {
    private final Map<String, Permission> permissions = new ConcurrentHashMap<>();

    /**
     * Save a permission.
     *
     * @param permission The permission to save
     * @return The saved permission with a generated ID
     */
    public Permission save(Permission permission) {
        if (permission.getId() == null || permission.getId().isEmpty()) {
            permission.setId(UUID.randomUUID().toString());
        }

        // Set timestamps if not already set
        long now = System.currentTimeMillis();
        if (permission.getCreatedAt() == 0) {
            permission.setCreatedAt(now);
        }
        permission.setUpdatedAt(now);

        permissions.put(permission.getId(), permission);
        return permission;
    }

    /**
     * Find a permission by ID.
     *
     * @param id The ID of the permission to find
     * @return The permission, or null if not found
     */
    public Permission findById(String id) {
        return permissions.get(id);
    }

    /**
     * Find a permission by name.
     *
     * @param name The name of the permission to find
     * @return The permission, or null if not found
     */
    public Permission findByName(String name) {
        return permissions.values().stream()
                .filter(permission -> name.equals(permission.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find all permissions.
     *
     * @return A list of all permissions
     */
    public List<Permission> findAll() {
        return new ArrayList<>(permissions.values());
    }

    /**
     * Find permissions by category.
     *
     * @param category The category to filter by
     * @return A list of permissions in the specified category
     */
    public List<Permission> findByCategory(String category) {
        return permissions.values().stream()
                .filter(permission -> category.equals(permission.getCategory()))
                .collect(Collectors.toList());
    }

    /**
     * Delete a permission by ID.
     *
     * @param id The ID of the permission to delete
     * @return true if the permission was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return permissions.remove(id) != null;
    }

    /**
     * Check if a permission name already exists.
     *
     * @param name The permission name to check
     * @return true if the permission name exists, false otherwise
     */
    public boolean existsByName(String name) {
        return permissions.values().stream()
                .anyMatch(permission -> name.equals(permission.getName()));
    }

    /**
     * Get the total number of permissions.
     *
     * @return The total number of permissions
     */
    public int count() {
        return permissions.size();
    }

    /**
     * Initialize default permissions if none exist.
     * This method creates a set of standard permissions for the application.
     */
    public void initializeDefaultPermissions() {
        if (count() > 0) {
            return; // Permissions already exist
        }

        // User management permissions
        createPermissionIfNotExists("user:view", "View users", "User Management");
        createPermissionIfNotExists("user:create", "Create users", "User Management");
        createPermissionIfNotExists("user:edit", "Edit users", "User Management");
        createPermissionIfNotExists("user:delete", "Delete users", "User Management");

        // Role management permissions
        createPermissionIfNotExists("role:view", "View roles", "Role Management");
        createPermissionIfNotExists("role:create", "Create roles", "Role Management");
        createPermissionIfNotExists("role:edit", "Edit roles", "Role Management");
        createPermissionIfNotExists("role:delete", "Delete roles", "Role Management");

        // Log management permissions
        createPermissionIfNotExists("log:view", "View logs", "Log Management");
        createPermissionIfNotExists("log:search", "Search logs", "Log Management");
        createPermissionIfNotExists("log:export", "Export logs", "Log Management");

        // Dashboard permissions
        createPermissionIfNotExists("dashboard:view", "View dashboards", "Dashboard");
        createPermissionIfNotExists("dashboard:create", "Create dashboards", "Dashboard");
        createPermissionIfNotExists("dashboard:edit", "Edit dashboards", "Dashboard");
        createPermissionIfNotExists("dashboard:delete", "Delete dashboards", "Dashboard");
        createPermissionIfNotExists("dashboard:share", "Share dashboards", "Dashboard");

        // Alarm permissions
        createPermissionIfNotExists("alarm:view", "View alarms", "Alarm");
        createPermissionIfNotExists("alarm:create", "Create alarms", "Alarm");
        createPermissionIfNotExists("alarm:edit", "Edit alarms", "Alarm");
        createPermissionIfNotExists("alarm:delete", "Delete alarms", "Alarm");
        createPermissionIfNotExists("alarm:acknowledge", "Acknowledge alarms", "Alarm");

        // Settings permissions
        createPermissionIfNotExists("settings:view", "View settings", "Settings");
        createPermissionIfNotExists("settings:edit", "Edit settings", "Settings");
    }

    private Permission createPermissionIfNotExists(String name, String description, String category) {
        if (existsByName(name)) {
            return findByName(name);
        }

        Permission permission = new Permission();
        permission.setName(name);
        permission.setDescription(description);
        permission.setCategory(category);
        return save(permission);
    }
}