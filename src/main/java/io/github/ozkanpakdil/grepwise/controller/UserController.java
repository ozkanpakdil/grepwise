package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.Role;
import io.github.ozkanpakdil.grepwise.model.User;
import io.github.ozkanpakdil.grepwise.repository.RoleRepository;
import io.github.ozkanpakdil.grepwise.repository.UserRepository;
import io.github.ozkanpakdil.grepwise.service.AuditLogService;
import io.github.ozkanpakdil.grepwise.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for user management.
 * This controller provides endpoints for creating, retrieving, updating, and deleting users.
 * 
 * This controller is versioned as v1 by default, making all endpoints accessible at /api/v1/users.
 * Individual methods can override this version with their own @ApiVersion annotation.
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuthService authService;
    
    @Autowired
    private AuditLogService auditLogService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Get all users (API v1).
     *
     * @return List of all users with basic information
     */
    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            List<UserResponse> userResponses = users.stream()
                    .map(this::convertToUserResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            logger.error("Error retrieving users: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get a user by ID.
     *
     * @param id The user ID
     * @return The user
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(@PathVariable String id) {
        try {
            User user = userRepository.findById(id);
            if (user != null) {
                return ResponseEntity.ok(convertToUserResponse(user));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error retrieving user {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Create a new user.
     *
     * @param userRequest The user creation request
     * @return The created user
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody UserRequest userRequest) {
        try {
            logger.info("User creation attempt for username: {}", userRequest.getUsername());
            
            // Check if username or email already exists
            if (userRepository.existsByUsername(userRequest.getUsername())) {
                logger.warn("User creation failed: Username already exists: {}", userRequest.getUsername());
                
                // Log the failed user creation
                auditLogService.createUserAuditLog(
                    "CREATE", 
                    "FAILURE", 
                    null, 
                    userRequest.getUsername(), 
                    "User creation failed: Username already exists", 
                    null
                );
                
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }
            
            if (userRepository.existsByEmail(userRequest.getEmail())) {
                logger.warn("User creation failed: Email already exists: {}", userRequest.getEmail());
                
                // Log the failed user creation
                auditLogService.createUserAuditLog(
                    "CREATE", 
                    "FAILURE", 
                    null, 
                    userRequest.getUsername(), 
                    "User creation failed: Email already exists", 
                    null
                );
                
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }
            
            // Create new user
            User user = new User();
            user.setUsername(userRequest.getUsername());
            user.setEmail(userRequest.getEmail());
            user.setPassword(passwordEncoder.encode(userRequest.getPassword()));
            user.setFirstName(userRequest.getFirstName());
            user.setLastName(userRequest.getLastName());
            user.setTenantId(userRequest.getTenantId());
            
            // Add roles if provided
            if (userRequest.getRoleIds() != null && !userRequest.getRoleIds().isEmpty()) {
                for (String roleId : userRequest.getRoleIds()) {
                    Role role = roleRepository.findById(roleId);
                    if (role != null) {
                        user.addRole(role);
                    }
                }
            } else {
                // Add default USER role if no roles provided
                Role userRole = roleRepository.findByName("USER");
                if (userRole != null) {
                    user.addRole(userRole);
                }
            }
            
            user.setEnabled(userRequest.getEnabled() != null ? userRequest.getEnabled() : true);
            
            User createdUser = userRepository.save(user);
            
            // Log the successful user creation
            logger.info("User created successfully: {}", createdUser.getUsername());
            Map<String, String> details = new HashMap<>();
            details.put("email", createdUser.getEmail());
            details.put("firstName", createdUser.getFirstName());
            details.put("lastName", createdUser.getLastName());
            details.put("roles", String.join(",", createdUser.getRoleNames()));
            
            auditLogService.createUserAuditLog(
                "CREATE", 
                "SUCCESS", 
                createdUser.getId(), 
                createdUser.getUsername(), 
                "User created successfully", 
                details
            );
            
            return ResponseEntity.status(HttpStatus.CREATED).body(convertToUserResponse(createdUser));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user creation request: {}", e.getMessage());
            
            // Log the failed user creation
            auditLogService.createUserAuditLog(
                "CREATE", 
                "FAILURE", 
                null, 
                userRequest.getUsername(), 
                "User creation failed: Invalid request - " + e.getMessage(), 
                null
            );
            
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage());
            
            // Log the failed user creation
            auditLogService.createUserAuditLog(
                "CREATE", 
                "FAILURE", 
                null, 
                userRequest.getUsername(), 
                "User creation failed: Internal server error", 
                null
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update an existing user.
     *
     * @param id The user ID
     * @param userRequest The user update request
     * @return The updated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UserRequest userRequest) {
        try {
            logger.info("User update attempt for ID: {}", id);
            
            User existingUser = userRepository.findById(id);
            if (existingUser == null) {
                logger.warn("User update failed: User not found with ID: {}", id);
                
                // Log the failed user update
                auditLogService.createUserAuditLog(
                    "UPDATE", 
                    "FAILURE", 
                    id, 
                    null, 
                    "User update failed: User not found", 
                    null
                );
                
                return ResponseEntity.notFound().build();
            }
            
            // Check if username or email already exists (if changed)
            if (userRequest.getUsername() != null && !userRequest.getUsername().equals(existingUser.getUsername()) 
                    && userRepository.existsByUsername(userRequest.getUsername())) {
                logger.warn("User update failed: Username already exists: {}", userRequest.getUsername());
                
                // Log the failed user update
                auditLogService.createUserAuditLog(
                    "UPDATE", 
                    "FAILURE", 
                    id, 
                    existingUser.getUsername(), 
                    "User update failed: Username already exists", 
                    null
                );
                
                return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
            }
            
            if (userRequest.getEmail() != null && !userRequest.getEmail().equals(existingUser.getEmail()) 
                    && userRepository.existsByEmail(userRequest.getEmail())) {
                logger.warn("User update failed: Email already exists: {}", userRequest.getEmail());
                
                // Log the failed user update
                auditLogService.createUserAuditLog(
                    "UPDATE", 
                    "FAILURE", 
                    id, 
                    existingUser.getUsername(), 
                    "User update failed: Email already exists", 
                    null
                );
                
                return ResponseEntity.badRequest().body(Map.of("error", "Email already exists"));
            }
            
            // Store original values for audit log
            String originalUsername = existingUser.getUsername();
            String originalEmail = existingUser.getEmail();
            List<String> originalRoles = new ArrayList<>(existingUser.getRoleNames());
            boolean originalEnabled = existingUser.isEnabled();
            
            // Update user fields if provided
            if (userRequest.getUsername() != null) {
                existingUser.setUsername(userRequest.getUsername());
            }
            
            if (userRequest.getEmail() != null) {
                existingUser.setEmail(userRequest.getEmail());
            }
            
            if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
                existingUser.setPassword(passwordEncoder.encode(userRequest.getPassword()));
            }
            
            if (userRequest.getFirstName() != null) {
                existingUser.setFirstName(userRequest.getFirstName());
            }
            
            if (userRequest.getLastName() != null) {
                existingUser.setLastName(userRequest.getLastName());
            }
            
            if (userRequest.getTenantId() != null) {
                existingUser.setTenantId(userRequest.getTenantId());
            }
            
            // Update roles if provided
            if (userRequest.getRoleIds() != null) {
                // Clear existing roles
                existingUser.getRoles().clear();
                
                // Add new roles
                for (String roleId : userRequest.getRoleIds()) {
                    Role role = roleRepository.findById(roleId);
                    if (role != null) {
                        existingUser.addRole(role);
                    }
                }
            }
            
            if (userRequest.getEnabled() != null) {
                existingUser.setEnabled(userRequest.getEnabled());
            }
            
            User updatedUser = userRepository.save(existingUser);
            
            // Log the successful user update
            logger.info("User updated successfully: {}", updatedUser.getUsername());
            
            // Create details map with changes
            Map<String, String> details = new HashMap<>();
            if (!originalUsername.equals(updatedUser.getUsername())) {
                details.put("usernameChanged", originalUsername + " -> " + updatedUser.getUsername());
            }
            if (!originalEmail.equals(updatedUser.getEmail())) {
                details.put("emailChanged", originalEmail + " -> " + updatedUser.getEmail());
            }
            if (userRequest.getPassword() != null && !userRequest.getPassword().isEmpty()) {
                details.put("passwordChanged", "true");
            }
            if (!originalRoles.equals(updatedUser.getRoleNames())) {
                details.put("rolesChanged", String.join(",", originalRoles) + " -> " + String.join(",", updatedUser.getRoleNames()));
            }
            if (originalEnabled != updatedUser.isEnabled()) {
                details.put("enabledChanged", originalEnabled + " -> " + updatedUser.isEnabled());
            }
            
            auditLogService.createUserAuditLog(
                "UPDATE", 
                "SUCCESS", 
                updatedUser.getId(), 
                updatedUser.getUsername(), 
                "User updated successfully", 
                details
            );
            
            return ResponseEntity.ok(convertToUserResponse(updatedUser));
        } catch (IllegalArgumentException e) {
            logger.warn("Invalid user update request for {}: {}", id, e.getMessage());
            
            // Log the failed user update
            auditLogService.createUserAuditLog(
                "UPDATE", 
                "FAILURE", 
                id, 
                null, 
                "User update failed: Invalid request - " + e.getMessage(), 
                null
            );
            
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating user {}: {}", id, e.getMessage());
            
            // Log the failed user update
            auditLogService.createUserAuditLog(
                "UPDATE", 
                "FAILURE", 
                id, 
                null, 
                "User update failed: Internal server error", 
                null
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Delete a user.
     *
     * @param id The user ID
     * @return Success response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            logger.info("User deletion attempt for ID: {}", id);
            
            // Get user before deletion for audit logging
            User user = userRepository.findById(id);
            String username = user != null ? user.getUsername() : null;
            
            boolean deleted = userRepository.deleteById(id);
            if (deleted) {
                logger.info("User deleted successfully: ID={}, username={}", id, username);
                
                // Log the successful user deletion
                Map<String, String> details = new HashMap<>();
                if (username != null) {
                    details.put("username", username);
                }
                
                auditLogService.createUserAuditLog(
                    "DELETE", 
                    "SUCCESS", 
                    id, 
                    username, 
                    "User deleted successfully", 
                    details
                );
                
                return ResponseEntity.ok(Map.of("message", "User deleted successfully"));
            } else {
                logger.warn("User deletion failed: User not found with ID: {}", id);
                
                // Log the failed user deletion
                auditLogService.createUserAuditLog(
                    "DELETE", 
                    "FAILURE", 
                    id, 
                    null, 
                    "User deletion failed: User not found", 
                    null
                );
                
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            logger.error("Error deleting user {}: {}", id, e.getMessage());
            
            // Log the failed user deletion
            auditLogService.createUserAuditLog(
                "DELETE", 
                "FAILURE", 
                id, 
                null, 
                "User deletion failed: Internal server error", 
                null
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Get users by role.
     *
     * @param roleName The role to filter by
     * @return List of users with the specified role
     */
    @GetMapping("/role/{roleName}")
    public ResponseEntity<List<UserResponse>> getUsersByRole(@PathVariable String roleName) {
        try {
            List<User> users = userRepository.findByRole(roleName);
            List<UserResponse> userResponses = users.stream()
                    .map(this::convertToUserResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            logger.error("Error retrieving users by role {}: {}", roleName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Get users by permission.
     *
     * @param permissionName The permission to filter by
     * @return List of users with the specified permission
     */
    @GetMapping("/permission/{permissionName}")
    public ResponseEntity<List<UserResponse>> getUsersByPermission(@PathVariable String permissionName) {
        try {
            List<User> users = userRepository.findByPermission(permissionName);
            List<UserResponse> userResponses = users.stream()
                    .map(this::convertToUserResponse)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(userResponses);
        } catch (Exception e) {
            logger.error("Error retrieving users by permission {}: {}", permissionName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * Convert a User entity to a UserResponse DTO.
     *
     * @param user The User entity to convert
     * @return The UserResponse DTO
     */
    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRoleIds(user.getRoles().stream().map(Role::getId).collect(Collectors.toList()));
        response.setRoleNames(user.getRoleNames());
        response.setTenantId(user.getTenantId());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        response.setEnabled(user.isEnabled());
        return response;
    }

    /**
     * Request DTO for user creation/update.
     */
    public static class UserRequest {
        private String username;
        private String email;
        private String password;
        private String firstName;
        private String lastName;
        private List<String> roleIds;
        private Boolean enabled;
        private String tenantId;

        // Getters and setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public List<String> getRoleIds() { return roleIds; }
        public void setRoleIds(List<String> roleIds) { this.roleIds = roleIds; }

        public Boolean getEnabled() { return enabled; }
        public void setEnabled(Boolean enabled) { this.enabled = enabled; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }
    
    /**
     * Response DTO for user data.
     */
    public static class UserResponse {
        private String id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private List<String> roleIds;
        private List<String> roleNames;
        private String tenantId;
        private long createdAt;
        private long updatedAt;
        private boolean enabled;

        // Getters and setters
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }

        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }

        public List<String> getRoleIds() { return roleIds; }
        public void setRoleIds(List<String> roleIds) { this.roleIds = roleIds; }

        public List<String> getRoleNames() { return roleNames; }
        public void setRoleNames(List<String> roleNames) { this.roleNames = roleNames; }
        
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }

        public long getCreatedAt() { return createdAt; }
        public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

        public long getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}