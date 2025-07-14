package io.github.ozkanpakdil.grepwise.repository;

import io.github.ozkanpakdil.grepwise.model.Dashboard;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Repository for storing and retrieving dashboard information.
 * This is a simple in-memory implementation for now.
 * In a production environment, this would be replaced with a database implementation.
 */
@Repository
public class DashboardRepository {
    private final Map<String, Dashboard> dashboards = new ConcurrentHashMap<>();

    /**
     * Save a dashboard.
     *
     * @param dashboard The dashboard to save
     * @return The saved dashboard with a generated ID
     */
    public Dashboard save(Dashboard dashboard) {
        if (dashboard.getId() == null || dashboard.getId().isEmpty()) {
            dashboard.setId(UUID.randomUUID().toString());
        }
        
        // Set timestamps if not already set
        long now = System.currentTimeMillis();
        if (dashboard.getCreatedAt() == 0) {
            dashboard.setCreatedAt(now);
        }
        dashboard.setUpdatedAt(now);
        
        dashboards.put(dashboard.getId(), dashboard);
        return dashboard;
    }

    /**
     * Find a dashboard by ID.
     *
     * @param id The ID of the dashboard to find
     * @return The dashboard, or null if not found
     */
    public Dashboard findById(String id) {
        return dashboards.get(id);
    }

    /**
     * Find all dashboards.
     *
     * @return A list of all dashboards
     */
    public List<Dashboard> findAll() {
        return new ArrayList<>(dashboards.values());
    }

    /**
     * Find dashboards by creator.
     *
     * @param createdBy The creator to filter by
     * @return A list of dashboards created by the specified user
     */
    public List<Dashboard> findByCreatedBy(String createdBy) {
        return dashboards.values().stream()
                .filter(dashboard -> createdBy.equals(dashboard.getCreatedBy()))
                .collect(Collectors.toList());
    }

    /**
     * Find shared dashboards.
     *
     * @return A list of shared dashboards
     */
    public List<Dashboard> findByShared(boolean isShared) {
        return dashboards.values().stream()
                .filter(dashboard -> dashboard.isShared() == isShared)
                .collect(Collectors.toList());
    }

    /**
     * Find dashboards by name (case-insensitive partial match).
     *
     * @param name The name to search for
     * @return A list of dashboards with names containing the specified text
     */
    public List<Dashboard> findByNameContaining(String name) {
        return dashboards.values().stream()
                .filter(dashboard -> dashboard.getName() != null && 
                        dashboard.getName().toLowerCase().contains(name.toLowerCase()))
                .collect(Collectors.toList());
    }

    /**
     * Find dashboards accessible by a user (created by them or shared).
     *
     * @param userId The user ID
     * @return A list of dashboards accessible by the user
     */
    public List<Dashboard> findAccessibleByUser(String userId) {
        return dashboards.values().stream()
                .filter(dashboard -> userId.equals(dashboard.getCreatedBy()) || dashboard.isShared())
                .collect(Collectors.toList());
    }

    /**
     * Delete a dashboard by ID.
     *
     * @param id The ID of the dashboard to delete
     * @return true if the dashboard was deleted, false otherwise
     */
    public boolean deleteById(String id) {
        return dashboards.remove(id) != null;
    }

    /**
     * Check if a dashboard with the given name already exists for a user.
     *
     * @param name The name to check
     * @param createdBy The creator
     * @return true if a dashboard with the name exists for the user, false otherwise
     */
    public boolean existsByNameAndCreatedBy(String name, String createdBy) {
        return dashboards.values().stream()
                .anyMatch(dashboard -> name.equals(dashboard.getName()) && 
                         createdBy.equals(dashboard.getCreatedBy()));
    }

    /**
     * Get the total number of dashboards.
     *
     * @return The total number of dashboards
     */
    public int count() {
        return dashboards.size();
    }

    /**
     * Get the number of shared dashboards.
     *
     * @return The number of shared dashboards
     */
    public int countShared() {
        return (int) dashboards.values().stream()
                .filter(Dashboard::isShared)
                .count();
    }

    /**
     * Update an existing dashboard.
     *
     * @param dashboard The dashboard to update
     * @return The updated dashboard, or null if the dashboard doesn't exist
     */
    public Dashboard update(Dashboard dashboard) {
        if (dashboard.getId() == null || !dashboards.containsKey(dashboard.getId())) {
            return null;
        }
        
        dashboard.updateTimestamp();
        dashboards.put(dashboard.getId(), dashboard);
        return dashboard;
    }
}