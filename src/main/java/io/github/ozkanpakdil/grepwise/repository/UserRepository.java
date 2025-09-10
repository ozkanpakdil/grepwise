package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.User;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

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
 * Repository for storing and retrieving user information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class UserRepository {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final File dataFile = new File(CONFIG_DIR + File.separator + "users.json");

    @PostConstruct
    private void loadFromDisk() {
        try {
            if (dataFile.exists()) {
                User[] arr = objectMapper.readValue(dataFile, User[].class);
                for (User u : arr) {
                    if (u.getId() == null || u.getId().isEmpty()) {
                        u.setId(UUID.randomUUID().toString());
                    }
                    users.put(u.getId(), u);
                }
            } else {
                // Ensure directory exists for future saves
                if (!dataFile.getParentFile().exists()) {
                    dataFile.getParentFile().mkdirs();
                }
            }
        } catch (Exception ignored) {
            // If load fails, start with empty in-memory store
        }
    }

    private void persist() {
        try {
            if (!dataFile.getParentFile().exists()) {
                dataFile.getParentFile().mkdirs();
            }
            objectMapper.writeValue(dataFile, new ArrayList<>(users.values()));
        } catch (IOException ignored) {
            // Best-effort persistence; ignore IO errors to avoid breaking API calls
        }
    }

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
        persist();
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
     * Find users by role name.
     *
     * @param roleName The name of the role to filter by
     * @return A list of users with the specified role
     */
    public List<User> findByRole(String roleName) {
        return users.values().stream()
                .filter(user -> user.hasRole(roleName))
                .collect(Collectors.toList());
    }

    /**
     * Find users by permission.
     *
     * @param permissionName The name of the permission to filter by
     * @return A list of users with the specified permission
     */
    public List<User> findByPermission(String permissionName) {
        return users.values().stream()
                .filter(user -> user.hasPermission(permissionName))
                .collect(Collectors.toList());
    }

    /**
     * Delete a user by ID.
     *
     * @param id The ID of the user to delete
     * @return true if the user was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        boolean removed = users.remove(id) != null;
        if (removed) {
            persist();
        }
        return removed;
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