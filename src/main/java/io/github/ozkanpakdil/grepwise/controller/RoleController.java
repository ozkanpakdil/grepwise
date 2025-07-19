package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.Permission;
import io.github.ozkanpakdil.grepwise.model.Role;
import io.github.ozkanpakdil.grepwise.repository.PermissionRepository;
import io.github.ozkanpakdil.grepwise.repository.RoleRepository;
import io.github.ozkanpakdil.grepwise.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for role management.
 * This controller provides endpoints for creating, retrieving, updating, and deleting roles.
 */
@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
public class RoleController {
    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get all roles.
     *
     * @return List of all roles
     */
    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles() {
        try {
            List<Role> roles = roleRepository.findAll();
            return ResponseEntity.ok(roles);
        } catch (Exception e) {
            logger.error("Error retrieving roles: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a role by ID.
     *
     * @param id The role ID
     * @return The role
     */
    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable String id) {
        try {
            Role role = roleRepository.findById(id);
            if (role != null) {
                return ResponseEntity.ok(role);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving role {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new role.
     *
     * @param roleRequest The role creation request
     * @return The created role
     */
    @PostMapping
    public ResponseEntity<?> createRole(@RequestBody RoleRequest roleRequest) {
        try {
            // Check if role name already exists
            if (roleRepository.existsByName(roleRequest.getName())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Role name already exists"));
            }
            
            // Create new role
            Role role = new Role();
            role.setName(roleRequest.getName());
            role.setDescription(roleRequest.getDescription());
            
            // Add permissions if provided
            if (roleRequest.getPermissionIds() != null && !roleRequest.getPermissionIds().isEmpty()) {
                for (String permissionId : roleRequest.getPermissionIds()) {
                    Permission permission = permissionRepository.findById(permissionId);
                    if (permission != null) {
                        role.addPermission(permission);
                    }
                }
            }
            
            Role createdRole = roleRepository.save(role);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdRole);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid role creation request: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating role: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update an existing role.
     *
     * @param id The role ID
     * @param roleRequest The role update request
     * @return The updated role
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateRole(@PathVariable String id, @RequestBody RoleRequest roleRequest) {
        try {
            Role existingRole = roleRepository.findById(id);
            if (existingRole == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if role name already exists (if changed)
            if (roleRequest.getName() != null && !roleRequest.getName().equals(existingRole.getName()) 
                    && roleRepository.existsByName(roleRequest.getName())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Role name already exists"));
            }
            
            // Update role fields if provided
            if (roleRequest.getName() != null) {
                existingRole.setName(roleRequest.getName());
            }
            
            if (roleRequest.getDescription() != null) {
                existingRole.setDescription(roleRequest.getDescription());
            }
            
            // Update permissions if provided
            if (roleRequest.getPermissionIds() != null) {
                // Clear existing permissions
                existingRole.getPermissions().clear();
                
                // Add new permissions
                for (String permissionId : roleRequest.getPermissionIds()) {
                    Permission permission = permissionRepository.findById(permissionId);
                    if (permission != null) {
                        existingRole.addPermission(permission);
                    }
                }
            }
            
            Role updatedRole = roleRepository.save(existingRole);
            
            return ResponseEntity.ok(updatedRole);
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid role update request for {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating role {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Delete a role.
     *
     * @param id The role ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRole(@PathVariable String id) {
        try {
            Role role = roleRepository.findById(id);
            if (role == null) {
                return ResponseEntity.notFound().build();
            }
            
            // Check if role is in use by any users
            if (!userRepository.findByRole(role.getName()).isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot delete role that is assigned to users"));
            }
            
            boolean deleted = roleRepository.deleteById(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("message", "Role deleted successfully"));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting role {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get all permissions.
     *
     * @return List of all permissions
     */
    @GetMapping("/permissions")
    public ResponseEntity<List<Permission>> getAllPermissions() {
        try {
            List<Permission> permissions = permissionRepository.findAll();
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            logger.error("Error retrieving permissions: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get permissions by category.
     *
     * @param category The category to filter by
     * @return List of permissions in the specified category
     */
    @GetMapping("/permissions/category/{category}")
    public ResponseEntity<List<Permission>> getPermissionsByCategory(@PathVariable String category) {
        try {
            List<Permission> permissions = permissionRepository.findByCategory(category);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            logger.error("Error retrieving permissions by category {}: {}", category, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all permission categories.
     *
     * @return List of all permission categories
     */
    @GetMapping("/permissions/categories")
    public ResponseEntity<List<String>> getAllPermissionCategories() {
        try {
            List<String> categories = permissionRepository.findAll().stream()
                    .map(Permission::getCategory)
                    .distinct()
                    .collect(Collectors.toList());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            logger.error("Error retrieving permission categories: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Request DTO for role creation/update.
     */
    public static class RoleRequest {
        private String name;
        private String description;
        private List<String> permissionIds;

        // Getters and setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<String> getPermissionIds() { return permissionIds; }
        public void setPermissionIds(List<String> permissionIds) { this.permissionIds = permissionIds; }
    }
}