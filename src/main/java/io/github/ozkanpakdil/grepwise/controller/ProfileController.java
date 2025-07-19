package io.github.ozkanpakdil.grepwise.controller;

import io.github.ozkanpakdil.grepwise.model.User;
import io.github.ozkanpakdil.grepwise.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for user profile management.
 * Provides endpoints for getting and updating the current user's profile.
 */
@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * Get the current user's profile.
     *
     * @return The current user's profile
     */
    @GetMapping
    public ResponseEntity<?> getCurrentUserProfile() {
        try {
            // Get the current authenticated user's ID from the security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = (String) authentication.getPrincipal();

            // Find the user in the repository
            User user = userRepository.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            // Convert to a profile response (excluding sensitive information)
            ProfileResponse profileResponse = new ProfileResponse(
                    user.getId(),
                    user.getUsername(),
                    user.getEmail(),
                    user.getFirstName(),
                    user.getLastName(),
                    user.getRoleNames(),
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );

            return ResponseEntity.ok(profileResponse);
        } catch (Exception e) {
            logger.error("Error getting user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Update the current user's profile.
     *
     * @param profileRequest The profile update request
     * @return The updated profile
     */
    @PutMapping
    public ResponseEntity<?> updateCurrentUserProfile(@RequestBody ProfileRequest profileRequest) {
        try {
            // Get the current authenticated user's ID from the security context
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = (String) authentication.getPrincipal();

            // Find the user in the repository
            User user = userRepository.findById(userId);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "User not found"));
            }

            // Validate email uniqueness if changed
            if (profileRequest.getEmail() != null && !profileRequest.getEmail().equals(user.getEmail())
                    && userRepository.existsByEmail(profileRequest.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Email already exists"));
            }

            // Update user fields if provided
            if (profileRequest.getEmail() != null) {
                user.setEmail(profileRequest.getEmail());
            }

            if (profileRequest.getFirstName() != null) {
                user.setFirstName(profileRequest.getFirstName());
            }

            if (profileRequest.getLastName() != null) {
                user.setLastName(profileRequest.getLastName());
            }

            // Update password if provided
            if (profileRequest.getCurrentPassword() != null && profileRequest.getNewPassword() != null) {
                // Verify current password
                if (!passwordEncoder.matches(profileRequest.getCurrentPassword(), user.getPassword())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Current password is incorrect"));
                }

                // Validate new password
                if (profileRequest.getNewPassword().length() < 8) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "New password must be at least 8 characters long"));
                }

                // Update password
                user.setPassword(passwordEncoder.encode(profileRequest.getNewPassword()));
            }

            // Save the updated user
            User updatedUser = userRepository.save(user);

            // Convert to a profile response (excluding sensitive information)
            ProfileResponse profileResponse = new ProfileResponse(
                    updatedUser.getId(),
                    updatedUser.getUsername(),
                    updatedUser.getEmail(),
                    updatedUser.getFirstName(),
                    updatedUser.getLastName(),
                    updatedUser.getRoleNames(),
                    updatedUser.getCreatedAt(),
                    updatedUser.getUpdatedAt()
            );

            return ResponseEntity.ok(profileResponse);
        } catch (Exception e) {
            logger.error("Error updating user profile: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Request object for profile updates.
     */
    public static class ProfileRequest {
        private String email;
        private String firstName;
        private String lastName;
        private String currentPassword;
        private String newPassword;

        // Getters and setters
        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getCurrentPassword() {
            return currentPassword;
        }

        public void setCurrentPassword(String currentPassword) {
            this.currentPassword = currentPassword;
        }

        public String getNewPassword() {
            return newPassword;
        }

        public void setNewPassword(String newPassword) {
            this.newPassword = newPassword;
        }
    }

    /**
     * Response object for profile data.
     */
    public static class ProfileResponse {
        private String id;
        private String username;
        private String email;
        private String firstName;
        private String lastName;
        private java.util.List<String> roles;
        private long createdAt;
        private long updatedAt;

        // Constructor
        public ProfileResponse(String id, String username, String email, String firstName, String lastName,
                              java.util.List<String> roles, long createdAt, long updatedAt) {
            this.id = id;
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.roles = roles;
            this.createdAt = createdAt;
            this.updatedAt = updatedAt;
        }

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public java.util.List<String> getRoles() {
            return roles;
        }

        public void setRoles(java.util.List<String> roles) {
            this.roles = roles;
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
    }
}