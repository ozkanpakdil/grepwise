package io.github.ozkanpakdil.grepwise.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import io.github.ozkanpakdil.grepwise.model.Permission;
import io.github.ozkanpakdil.grepwise.model.Role;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.github.ozkanpakdil.grepwise.GrepWiseApplication.CONFIG_DIR;

/**
 * Repository for storing and retrieving role information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class RoleRepository {
    private final Map<String, Role> roles = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final File dataFile = new File(CONFIG_DIR + File.separator + "roles.json");

    @Autowired
    private PermissionRepository permissionRepository;

    @PostConstruct
    private void loadFromDisk() {
        try {
            if (dataFile.exists()) {
                Role[] arr = objectMapper.readValue(dataFile, Role[].class);
                for (Role r : arr) {
                    if (r.getId() == null || r.getId().isEmpty()) {
                        r.setId(UUID.randomUUID().toString());
                    }
                    roles.put(r.getId(), r);
                }
            } else {
                if (!dataFile.getParentFile().exists()) {
                    dataFile.getParentFile().mkdirs();
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void persist() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            objectMapper.writeValue(dataFile, new ArrayList<>(roles.values()));
        } catch (IOException ignored) {
        }
    }

    /**
     * Save a role.
     *
     * @param role The role to save
     * @return The saved role with a generated ID
     */
    public Role save(Role role) {
        if (role.getId() == null || role.getId().isEmpty()) {
            role.setId(UUID.randomUUID().toString());
        }

        // Set timestamps if not already set
        long now = System.currentTimeMillis();
        if (role.getCreatedAt() == 0) {
            role.setCreatedAt(now);
        }
        role.setUpdatedAt(now);

        roles.put(role.getId(), role);
        persist();
        return role;
    }

    /**
     * Find a role by ID.
     *
     * @param id The ID of the role to find
     * @return The role, or null if not found
     */
    public Role findById(String id) {
        return roles.get(id);
    }

    /**
     * Find a role by name.
     *
     * @param name The name of the role to find
     * @return The role, or null if not found
     */
    public Role findByName(String name) {
        return roles.values().stream()
                .filter(role -> name.equals(role.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find all roles.
     *
     * @return A list of all roles
     */
    public List<Role> findAll() {
        return new ArrayList<>(roles.values());
    }

    /**
     * Delete a role by ID.
     *
     * @param id The ID of the role to delete
     * @return true if the role was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        boolean removed = roles.remove(id) != null;
        if (removed) {
            persist();
        }
        return removed;
    }

    /**
     * Check if a role name already exists.
     *
     * @param name The role name to check
     * @return true if the role name exists, false otherwise
     */
    public boolean existsByName(String name) {
        return roles.values().stream()
                .anyMatch(role -> name.equals(role.getName()));
    }

    /**
     * Get the total number of roles.
     *
     * @return The total number of roles
     */
    public int count() {
        return roles.size();
    }

    /**
     * Find roles that have a specific permission.
     *
     * @param permissionName The name of the permission to filter by
     * @return A list of roles with the specified permission
     */
    public List<Role> findByPermission(String permissionName) {
        return roles.values().stream()
                .filter(role -> role.hasPermission(permissionName))
                .collect(Collectors.toList());
    }

    /**
     * Initialize default roles if none exist.
     * This method creates a set of standard roles for the application.
     */
    public void initializeDefaultRoles() {
        if (count() > 0) {
            return; // Roles already exist
        }

        // Ensure permissions are initialized
        permissionRepository.initializeDefaultPermissions();

        // Create admin role with all permissions
        Role adminRole = createRoleIfNotExists("ADMIN", "Administrator with full access");
        permissionRepository.findAll().forEach(adminRole::addPermission);
        save(adminRole);

        // Create user role with basic permissions
        Role userRole = createRoleIfNotExists("USER", "Standard user with limited access");
        addPermissionToRole(userRole, "log:view");
        addPermissionToRole(userRole, "log:search");
        addPermissionToRole(userRole, "dashboard:view");
        addPermissionToRole(userRole, "alarm:view");
        addPermissionToRole(userRole, "alarm:acknowledge");
        save(userRole);

        // Create manager role with elevated permissions
        Role managerRole = createRoleIfNotExists("MANAGER", "Manager with elevated access");
        addPermissionToRole(managerRole, "log:view");
        addPermissionToRole(managerRole, "log:search");
        addPermissionToRole(managerRole, "log:export");
        addPermissionToRole(managerRole, "dashboard:view");
        addPermissionToRole(managerRole, "dashboard:create");
        addPermissionToRole(managerRole, "dashboard:edit");
        addPermissionToRole(managerRole, "dashboard:delete");
        addPermissionToRole(managerRole, "dashboard:share");
        addPermissionToRole(managerRole, "alarm:view");
        addPermissionToRole(managerRole, "alarm:create");
        addPermissionToRole(managerRole, "alarm:edit");
        addPermissionToRole(managerRole, "alarm:delete");
        addPermissionToRole(managerRole, "alarm:acknowledge");
        addPermissionToRole(managerRole, "user:view");
        save(managerRole);
    }

    private Role createRoleIfNotExists(String name, String description) {
        if (existsByName(name)) {
            return findByName(name);
        }

        Role role = new Role();
        role.setName(name);
        role.setDescription(description);
        return save(role);
    }

    private void addPermissionToRole(Role role, String permissionName) {
        Permission permission = permissionRepository.findByName(permissionName);
        if (permission != null) {
            role.addPermission(permission);
        }
    }
}