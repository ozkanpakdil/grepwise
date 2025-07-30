package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.config.LdapConfig;
import io.github.ozkanpakdil.grepwise.model.Role;
import io.github.ozkanpakdil.grepwise.model.User;
import io.github.ozkanpakdil.grepwise.repository.RoleRepository;
import io.github.ozkanpakdil.grepwise.repository.UserRepository;
import io.github.ozkanpakdil.grepwise.service.AuditLogService;
import io.github.ozkanpakdil.grepwise.service.TokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Controller for handling LDAP authentication requests.
 */
@RestController
@RequestMapping("/api/auth/ldap")
@ConditionalOnProperty(name = {"grepwise.ldap.enabled"}, havingValue = "true")
public class LdapAuthController {
    private static final Logger logger = LoggerFactory.getLogger(LdapAuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private LdapConfig ldapConfig;

    @Autowired
    private LdapTemplate ldapTemplate;

    /**
     * Authenticate a user using LDAP.
     *
     * @param loginRequest The login request containing username and password
     * @return The authentication response with tokens and user information
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        if (!ldapConfig.isLdapEnabled()) {
            logger.warn("LDAP login attempt when LDAP is disabled");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "LDAP authentication is not enabled"));
        }

        try {
            logger.info("LDAP login attempt for username: {}", loginRequest.getUsername());

            // Authenticate with LDAP
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            // If authentication is successful, check if user exists in local database
            User user = userRepository.findByUsername(loginRequest.getUsername());
            
            if (user == null) {
                // User doesn't exist in local database, create a new user
                user = createUserFromLdapAuthentication(authentication, loginRequest.getUsername());
            } else {
                // User exists, update roles from LDAP if needed
                updateUserRolesFromLdapAuthentication(user, authentication);
            }

            // Generate tokens
            String accessToken = tokenService.generateToken(user);
            String refreshToken = tokenService.generateRefreshToken(user);

            // Log the successful login
            logger.info("LDAP user logged in successfully: {}", user.getUsername());
            auditLogService.createAuthAuditLog(
                "LDAP_LOGIN",
                "SUCCESS",
                user.getUsername(),
                "LDAP login successful"
            );

            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "LDAP login successful");
            response.put("accessToken", accessToken);
            response.put("refreshToken", refreshToken);
            response.put("expiresAt", tokenService.getExpirationDateFromToken(accessToken).getTime());
            response.put("user", convertUserToMap(user));

            return ResponseEntity.ok(response);
        } catch (AuthenticationException e) {
            logger.warn("LDAP login failed: Invalid username or password for: {}", loginRequest.getUsername());
            
            // Log the failed login attempt
            auditLogService.createAuthAuditLog(
                "LDAP_LOGIN",
                "FAILURE",
                loginRequest.getUsername(),
                "LDAP login failed: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Invalid username or password"));
        } catch (Exception e) {
            logger.error("LDAP login error: {}", e.getMessage(), e);
            
            // Log the failed login attempt
            auditLogService.createAuthAuditLog(
                "LDAP_LOGIN",
                "FAILURE",
                loginRequest.getUsername(),
                "LDAP login error: " + e.getMessage()
            );
            
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Create a new user from LDAP authentication.
     *
     * @param authentication The authentication object
     * @param username The username
     * @return The created user
     */
    private User createUserFromLdapAuthentication(Authentication authentication, String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@example.com"); // Default email, can be updated later
        user.setPassword("LDAP_AUTHENTICATED"); // Placeholder password, not used for authentication
        user.setFirstName("");
        user.setLastName("");
        user.setEnabled(true);
        
        // Add roles from LDAP
        addRolesToUser(user, authentication);
        
        // Save the user
        return userRepository.save(user);
    }

    /**
     * Update user roles from LDAP authentication.
     *
     * @param user The user to update
     * @param authentication The authentication object
     */
    private void updateUserRolesFromLdapAuthentication(User user, Authentication authentication) {
        // Clear existing roles
        user.getRoles().clear();
        
        // Add roles from LDAP
        addRolesToUser(user, authentication);
        
        // Save the updated user
        userRepository.save(user);
    }

    /**
     * Add roles to a user from LDAP authentication.
     *
     * @param user The user to add roles to
     * @param authentication The authentication object
     */
    private void addRolesToUser(User user, Authentication authentication) {
        // Get authorities from authentication
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        
        // Add roles based on authorities
        for (String authority : authorities) {
            // Remove ROLE_ prefix if present
            String roleName = authority.startsWith("ROLE_") ? authority.substring(5) : authority;
            
            // Find or create role
            Role role = roleRepository.findByName(roleName);
            if (role == null) {
                // If role doesn't exist, use USER role
                role = roleRepository.findByName("USER");
                if (role == null) {
                    // If USER role doesn't exist, skip
                    continue;
                }
            }
            
            // Add role to user
            user.addRole(role);
        }
        
        // If no roles were added, add USER role
        if (user.getRoles().isEmpty()) {
            Role userRole = roleRepository.findByName("USER");
            if (userRole != null) {
                user.addRole(userRole);
            }
        }
    }

    /**
     * Convert a User entity to a Map.
     *
     * @param user The User entity to convert
     * @return The Map representation of the user
     */
    private Map<String, Object> convertUserToMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("firstName", user.getFirstName());
        userMap.put("lastName", user.getLastName());
        userMap.put("roles", user.getRoleNames());
        userMap.put("enabled", user.isEnabled());
        userMap.put("createdAt", user.getCreatedAt());
        userMap.put("updatedAt", user.getUpdatedAt());
        return userMap;
    }

    /**
     * Request DTO for login.
     */
    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}