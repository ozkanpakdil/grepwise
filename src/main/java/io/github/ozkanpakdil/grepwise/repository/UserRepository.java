package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving user information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    /**
     * Save a user.
     *
     * @param user The user to save
     * @return The saved user with a generated ID
     */
    public User save(User user) {
        if (user.getId() == null || user.getId().isEmpty()) {
            user.setId(UUID.randomUUID().toString());
        }
        
        // Set timestamps if not already set
        long now = System.currentTimeMillis();
        if (user.getCreatedAt() == 0) {
            user.setCreatedAt(now);
        }
        user.setUpdatedAt(now);
        
        users.put(user.getId(), user);
        return user;
    }

    /**
     * Find a user by ID.
     *
     * @param id The ID of the user to find
     * @return The user, or null if not found
     */
    public User findById(String id) {
        return users.get(id);
    }

    /**
     * Find a user by username.
     *
     * @param username The username to find
     * @return The user, or null if not found
     */
    public User findByUsername(String username) {
        return users.values().stream()
                .filter(user -> username.equals(user.getUsername()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find a user by email.
     *
     * @param email The email to find
     * @return The user, or null if not found
     */
    public User findByEmail(String email) {
        return users.values().stream()
                .filter(user -> email.equals(user.getEmail()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Find all users.
     *
     * @return A list of all users
     */
    public List<User> findAll() {
        return new ArrayList<>(users.values());
    }

    /**
     * Find users by role.
     *
     * @param role The role to filter by
     * @return A list of users with the specified role
     */
    public List<User> findByRole(String role) {
        return users.values().stream()
                .filter(user -> user.getRoles().contains(role))
                .collect(Collectors.toList());
    }

    /**
     * Delete a user by ID.
     *
     * @param id The ID of the user to delete
     * @return true if the user was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return users.remove(id) != null;
    }

    /**
     * Check if a username already exists.
     *
     * @param username The username to check
     * @return true if the username exists, false otherwise
     */
    public boolean existsByUsername(String username) {
        return users.values().stream()
                .anyMatch(user -> username.equals(user.getUsername()));
    }

    /**
     * Check if an email already exists.
     *
     * @param email The email to check
     * @return true if the email exists, false otherwise
     */
    public boolean existsByEmail(String email) {
        return users.values().stream()
                .anyMatch(user -> email.equals(user.getEmail()));
    }

    /**
     * Get the total number of users.
     *
     * @return The total number of users
     */
    public int count() {
        return users.size();
    }
}